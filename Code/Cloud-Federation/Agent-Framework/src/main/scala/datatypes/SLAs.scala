package datatypes

import datatypes.CPUUnit.{CPUUnit}
import datatypes.CloudCurrency.CloudCurrency
import datatypes.ImgFormat.ImgFormat

case class Price(value: Float, currency: CloudCurrency)

/**
 * Created by costa on 10/16/14.
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
 *
 */
case class HostSLA(relOnlineTime: 							Float,
						 private var _supportedImgFormats:	Vector[ImgFormat],
						 private var _maxVMsPerCPU:			Vector[(CPUUnit, Integer)])
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
		val allFormatsContained = other.supportedImgFormats.forall(this.supportedImgFormats.contains)
		if(! allFormatsContained)
			return false

		// Check if each Mapping of CPUUnit to Integer is at least as low as in other's:
		// (the lower the Integer value, the less VMs are able to run on the same machine)
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

	/**
	 * Amplifies the hardeness of this and the other SLA together.
	 * <p>
	 *    Hardeness means that a SLA is <em>harder<em> than the other, if more things have to be fulfilled,
	 *    or the quality of Service, agreed to by this SLA is higher.
	 * @param other
	 * @return
	 */
	def combineToAmplifiedSLA(other: HostSLA): HostSLA = {
		val amplRelOnlineTime = if (this.relOnlineTime >= other.relOnlineTime) this.relOnlineTime else other.relOnlineTime
		val amplSupportedImgFormats = this.supportedImgFormats ++ other.supportedImgFormats.filter(!this.supportedImgFormats.contains(_))
		val amplMaxVMsPerCPU = this.maxVMsPerCPU.map(getSmallerMaxValPerCPU(_, other.maxVMsPerCPU))

		return new HostSLA(amplRelOnlineTime, amplSupportedImgFormats, amplMaxVMsPerCPU)
	}


/* Method Overrides: */
/* ================= */

	override def equals(obj: scala.Any): Boolean = obj match{
		case that: HostSLA 	=> this.supportedImgFormats.equals(that.supportedImgFormats) &&
		  								this.maxVMsPerCPU.equals(that.maxVMsPerCPU)
		case _ 					=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[HostSLA]


	private def getSmallerMaxValPerCPU(tuple: (CPUUnit, Integer), otherMaxVMsPerCPU: Vector[(CPUUnit, Integer)]): (CPUUnit, Integer) = {
		//Find the element with the same CPUUnit of tuple in otherMaxVMsPerCPU-Vector:
		val otherTuple: Option[(CPUUnit, Integer)] = otherMaxVMsPerCPU.find(t => t._1.equals(tuple._1))

		//Then return the smaller Integer value from both of the (CPUUnit, Integer) tuples:
		val newTuple : (CPUUnit, Integer) = if(tuple._2 < otherTuple.get._2) tuple else otherTuple.get
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
