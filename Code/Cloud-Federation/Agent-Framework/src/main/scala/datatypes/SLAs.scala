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
 * @param _maxVMsPerCPU A list of maximum allowed VMs per Host with a given CPUUnit.
 *                     If a Host (classified by its CPUUnit) hypervises more running VMs than specified here,
 *                     this host must not be a part of a federation, based on the given SLA.
 *                     Represented as a Vector[Tuple2] elements,
 *                     where each Tuple2 has a "CPUUnit -> max. number of VMs" mapping.
 * @author Constantin Gaul, created on 10/16/14.
 */
case class HostSLA(relOnlineTime: 							Float,
						 private var _supportedImgFormats:	Vector[ImgFormat],
						 private var _maxVMsPerCPU:			Vector[(CPUUnit, Int)])
{

	// Each supported Image format should only be once in the Vector
	// and the Vector should be sorted by the natural ordering of the ImgFormat enum:
	_supportedImgFormats = _supportedImgFormats.distinct
	_supportedImgFormats = _supportedImgFormats.sorted

	// Each CPUUnit should only have one representing Tuple in the maxVMsPerCPU Vector
	// and the Vector should be sorted by the CPUUnitOrdering, extending Ordering[CPUUnit]
	// If the maxVMsPerCPU Vector has duplicate tuples, both representing one CPUUnit, drop the whole mapping for it:
	_maxVMsPerCPU = _maxVMsPerCPU.filter(t1 => _maxVMsPerCPU.count(t2 => t1._1 == t2._1) == 1)
	_maxVMsPerCPU = _maxVMsPerCPU.sortBy(_._1)(CPUUnitOrdering)

/* Case Class Variable Getter: */
/* =========================== */

	def supportedImgFormats = _supportedImgFormats
	def maxVMsPerCPU			= _maxVMsPerCPU



/* Public Methods: */
/* =============== */

	/**
	 * Combines the Quality of Service from this and the other SLA, building an amplified HostSLA.
	 * @param other
	 * @return
	 */
	def combineToAmplifiedSLA(other: HostSLA): HostSLA = {
		val amplRelOnlineTime = if (this.relOnlineTime >= other.relOnlineTime) this.relOnlineTime else other.relOnlineTime
		val amplSupportedImgFormats = this.supportedImgFormats ++ other.supportedImgFormats.filter(!this.supportedImgFormats.contains(_))
		val amplMaxVMsPerCPU = this.maxVMsPerCPU.map(getSmallerMaxValPerCPU(_, other.maxVMsPerCPU))

		return new HostSLA(amplRelOnlineTime, amplSupportedImgFormats, amplMaxVMsPerCPU)
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

		// Check if each Mapping of CPUUnit to Int is at least as low as in other's:
		// (the lower the Int value, the less VMs are able to run on the same machine)
		// To do this, first check if each CPUUnit mapping from other is defined in this SLA:
		for (actCPUTuple <- other.maxVMsPerCPU) {
			val actCPUUnit = actCPUTuple._1
			val index = this.maxVMsPerCPU.indexWhere(t => t._1.equals(actCPUUnit))
			if(index == -1)
			return false
			else{
				//If a match is found for the actCPUTuple, compare both Int-Values with each other:
				if(actCPUTuple._2 > this.maxVMsPerCPU(index)._2)
				return false
			}
		}

		//If non of the breaking condition was reached until here, this SLA completely fulfills the other SLA:
		return true
	}

	def checkAgainstVMsPerCPU(vmCountByCPU: Vector[(CPUUnit, Int)]): Boolean ={
		//For each available CPUUnit, check if a resource Count is violating the SLA-Limit:
		for (actCPUUnit <- CPUUnit.values) {
			val resByCPUUnit: Option[(CPUUnit, Int)] = vmCountByCPU.find(t => t._1 == actCPUUnit)
			val slaByCPUUnit: Option[(CPUUnit, Int)] = this.maxVMsPerCPU.find(t => t._1 == actCPUUnit)

			// The SLA is violated, if more defined resources are available per CPUUnit, then allowed per SLA.
			// Cornercases:
			// If there is no SLA defined for that CPUUnit, unlimited resources are allowed.
			// If there is no resource defined for that CPUUnit, no resource could violate the SLA.
			val undefinedSLA = (actCPUUnit, Int.MaxValue)
			val undefinedResource = (actCPUUnit, 0)
			val violatingSLA: Boolean = slaByCPUUnit.getOrElse(undefinedSLA)._2 < resByCPUUnit.getOrElse(undefinedResource)._2
			if (violatingSLA) {
				return false
			}
		}
		return true
	}



/* Method Overrides: */
/* ================= */

	override def equals(obj: scala.Any): Boolean = obj match{
		case that: HostSLA 	=> this.supportedImgFormats.equals(that.supportedImgFormats) &&
		  								this.maxVMsPerCPU.equals(that.maxVMsPerCPU)
		case _ 					=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[HostSLA]


	private def getSmallerMaxValPerCPU(tuple: (CPUUnit, Int), otherMaxVMsPerCPU: Vector[(CPUUnit, Int)]): (CPUUnit, Int) = {
		//Find the element with the same CPUUnit of tuple in otherMaxVMsPerCPU-Vector:
		val otherTuple: Option[(CPUUnit, Int)] = otherMaxVMsPerCPU.find(t => t._1.equals(tuple._1))

		//Then return the smaller Int value from both of the (CPUUnit, Int) tuples:
		val newTuple : (CPUUnit, Int) = if(tuple._2 < otherTuple.get._2) tuple else otherTuple.get
		return newTuple
	}
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
