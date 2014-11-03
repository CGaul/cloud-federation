package datatypes

import datatypes.CPUUnit.{CPUUnit}
import datatypes.CloudCurrency.CloudCurrency
import datatypes.ImgFormat.ImgFormat

case class Price(value: Float, currency: CloudCurrency)

/**
 * A service level agreement is a contract between a service provider and a user.
 * In most cases, the agreement is between a business and a consumer,
 * though SLAs may be established between two business as well.
 * In either case, the SLA defines specific services that are guaranteed
 * over a given amount of time, often for a specific price.
 * from: http://www.techterms.com/definition/sla
 *
 * @param relOnlineTime The procentual, guaranteed online time for each requested machine in the Cloud-Network.
 * @param _supportedImgFormats A list of minimal requirements regarding supported ImgFormat.
 *                            Defaults to all known formats
 * @param _maxResPerCPU A list of maximum allowed VMs per Host with a given CPUUnit.
 *                     If a Host (classified by its CPUUnit) hypervises more running VMs than specified here,
 *                     this host must not be a part of a federation, based on the given SLA.
 *                     Represented as a Vector[Tuple2] elements,
 *                     where each Tuple2 has a "CPUUnit -> max. number of VMs" mapping.
 * @author Constantin Gaul, created on 10/16/14.
 */
case class HostSLA(relOnlineTime: 							Float,
						 private var _supportedImgFormats:	Vector[ImgFormat],
						 private var _maxResPerCPU:			Vector[(CPUUnit, Int)])
{

	// Each supported Image format should only be once in the Vector
	// and the Vector should be sorted by the natural ordering of the ImgFormat enum:
	_supportedImgFormats = _supportedImgFormats.distinct
	_supportedImgFormats = _supportedImgFormats.sorted

	// Each CPUUnit should only have one representing Tuple in the maxResPerCPU Vector
	// and the Vector should be sorted by the CPUUnitOrdering, extending Ordering[CPUUnit]
	// If the maxResPerCPU Vector has duplicate tuples, both representing one CPUUnit, drop the whole mapping for it:
	_maxResPerCPU = _maxResPerCPU.filter(t1 => _maxResPerCPU.count(t2 => t1._1 == t2._1) == 1)
	_maxResPerCPU = _maxResPerCPU.sortBy(_._1)(CPUUnitOrdering)

/* Case Class Variable Getter: */
/* =========================== */

	def supportedImgFormats = _supportedImgFormats
	def maxResPerCPU			= _maxResPerCPU



/* Public Methods: */
/* =============== */

	/**
	 * Combines the Quality of Service from this and the other SLA, building an amplified HostSLA.
	 * @param other
	 * @return
	 */
	def combineToAmplifiedSLA(other: HostSLA): HostSLA = {
		// The new (amplified) relative online time is the larger of both relOnlineTimes:
		val amplRelOnlineTime = if (this.relOnlineTime >= other.relOnlineTime) this.relOnlineTime else other.relOnlineTime

		// Amplified Supported Images of two combined HostSLAs are the concatenation of both:
		val amplSupportedImgFormats = this.supportedImgFormats ++ other.supportedImgFormats.filter(!this.supportedImgFormats.contains(_))

		// For each resource-per-CPU Tuple in this.maxResPerCPU, find the smallest Int-Limit from this and other.
		val thisAmplResPerCPU: Vector[(CPUUnit, Int)] = this.maxResPerCPU.map(getSmallerMaxValPerCPU(_, other.maxResPerCPU))
		// Find each CPUUnit that is not yet defined in this.maxResPerCPU but in other.maxResPerCPU:
		val undefinedCPUUnits: Vector[CPUUnit]				= other.maxResPerCPU.map(_._1).filter(this.maxResPerCPU.map(_._1).contains(_) == false)
		//For each of these undefined CPUUnits, use the values of other.maxVMPerCPU:
		val otherResPerCPU: Vector[(CPUUnit, Int)]		= other.maxResPerCPU.filter(t => undefinedCPUUnits.contains(t._1))

		val amplResPerCPU				= thisAmplResPerCPU ++ otherResPerCPU

		return new HostSLA(amplRelOnlineTime, amplSupportedImgFormats, amplResPerCPU)
	}

	/**
	  * Checks if this HostSLA is able to <em>fulfill</em> the other HostSLA.
	 * @param other The HostSLA, that is checked against
	 * @return true, if this HostSLA is guaranteeing a level of QoS that is at least
	 * 		  as high as the other, false if this SLA is not guaranteeing that level.
	 */
	def fulfillsQoS(other: HostSLA): Boolean = {
		// Check, if the relative online time is at least as high as other's:
		if(other.relOnlineTime > this.relOnlineTime)
		return false

		// Check, if at least all imageFormats are supported, that are supported by the other:
		// To do this, check if every other.imgFormat is in this.imgFormats:
		val allFormatsSupported = other.supportedImgFormats.forall(this.supportedImgFormats.contains)
		if(! allFormatsSupported)
		return false

		// Check if each Mapping of other.(CPUUnit, Int) is at least as low as in this.(CPUUnit, Int):
		// (the lower the Int value, the less VMs are able to run on the same machine)
		// If this SLA does not have a value for the other's (CPUUnit, Int) - Tuple,
		// this SLA is still fulfilling the QoS, as theoretically unlimited CPUUnit Resources could be spawned there:
		for (actCPUTuple <- other.maxResPerCPU) {
			val actCPUUnit = actCPUTuple._1
			val index = this.maxResPerCPU.indexWhere(t => t._1.equals(actCPUUnit))
			if(index != -1){
				//If a match is found for the actCPUTuple, compare both Int-Values with each other:
				if(actCPUTuple._2 > this.maxResPerCPU(index)._2)
				return false
			}
		}

		//If non of the breaking condition was reached until here, this SLA completely fulfills the other SLA:
		return true
	}

	def checkAgainstResPerCPU(resCountByCPU: Vector[(CPUUnit, Int)]): (Boolean, Vector[(CPUUnit, Int)]) ={
		// The output val of this method:
		var resourceCheck: (Boolean, Vector[(CPUUnit, Int)]) = (true, Vector())

		// For each available CPUUnit, check if a resource Count is violating the SLA-Limit:
		for (actCPUUnit <- CPUUnit.values) {
			val resByCPUUnit: Option[(CPUUnit, Int)] = resCountByCPU.find(t => t._1 == actCPUUnit)
			val slaByCPUUnit: Option[(CPUUnit, Int)] = this.maxResPerCPU.find(t => t._1 == actCPUUnit)

			// The SLA is violated, if more defined resources are available per CPUUnit, then allowed per SLA.
			// Cornercases:
			// If there is no SLA defined for that CPUUnit, unlimited resources are allowed.
			// If there is no resource defined for that CPUUnit, no resource could violate the SLA.
			val undefinedSLA = (actCPUUnit, Int.MaxValue)
			val undefinedResource = (actCPUUnit, 0)
			val slaResDiff: Int 			= slaByCPUUnit.getOrElse(undefinedSLA)._2 - resByCPUUnit.getOrElse(undefinedResource)._2
			val violatingSLA: Boolean = slaResDiff < 0
			if (violatingSLA) {
				resourceCheck = (false, resourceCheck._2:+ (actCPUUnit, slaResDiff))
			}
		}
		return resourceCheck
	}


	/* Private Methods: */
	/* ================ */

	private def getSmallerMaxValPerCPU(tuple: (CPUUnit, Int), othermaxResPerCPU: Vector[(CPUUnit, Int)]): (CPUUnit, Int) = {
		//Find the element with the same CPUUnit of tuple in othermaxResPerCPU-Vector:
		val otherTuple: Option[(CPUUnit, Int)] = othermaxResPerCPU.find(t => t._1.equals(tuple._1))

		// Then return the smaller Int value from both of the (CPUUnit, Int) tuples:
		// If otherTuple is None, compare tuple with itself and return tuple
		val newTuple : (CPUUnit, Int) = if(tuple._2 <= otherTuple.getOrElse(tuple)._2) tuple else otherTuple.get
		return newTuple
	}


/* Method Overrides: */
/* ================= */

	override def equals(obj: scala.Any): Boolean = obj match{
		case that: HostSLA 	=> this.supportedImgFormats.equals(that.supportedImgFormats) &&
		  								this.maxResPerCPU.equals(that.maxResPerCPU)
		case _ 					=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[HostSLA]
}

/**
 *
 * @param priceRangePerCPU
 * @param priceRangePerRAM
 * @param priceRangePerStorage
 */
case class CloudSLA(	priceRangePerCPU:		 Vector[(CPUUnit, Price, Price)],
						  	priceRangePerRAM:		 (ByteSize, Price, Price),
						  	priceRangePerStorage: (ByteSize, Price, Price))
{

	override def equals(obj: scala.Any): Boolean = obj match{
		case that: CloudSLA 	=> return (this.priceRangePerCPU.equals(that.priceRangePerCPU) &&
		  										  this.priceRangePerRAM.equals(that.priceRangePerRAM) &&
		  										  this.priceRangePerStorage.equals(that.priceRangePerStorage))
		case _ 					=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[CloudSLA]

}
