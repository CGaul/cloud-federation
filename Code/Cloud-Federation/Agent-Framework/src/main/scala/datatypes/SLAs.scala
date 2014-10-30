package datatypes

import datatypes.CPUUnit.CPU_Unit
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
 * @param supportedImgFormats A list of minimal requirements regarding supported Img_Format.
 *                            Defaults to all known formats
 * @param maxVMsPerCPU A list of maximum allowed VMs per Host with a given CPU_Unit.
 *                     If a Host (classified by its CPU_Unit) hypervises more running VMs than specified here,
 *                     this host must not be a part of a federation, based on the given SLA.
 *                     Represented as a Vector[Tuple2] elements,
 *                     where each Tuple2 has a "CPU_Unit -> max. number of VMs" mapping.
 *
 */
case class HostSLA(relOnlineTime: 			Float,
						 private var _supportedImgFormats:	Vector[ImgFormat],
						 private var _maxVMsPerCPU:			Vector[(CPU_Unit, Integer)])
{

	_supportedImgFormats = _supportedImgFormats.distinct
	_supportedImgFormats = _supportedImgFormats.sorted


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

		// Check if each Mapping of CPU_Unit to Integer is at least as low as in other's:
		// (the lower the Integer value, the less VMs are able to run on the same machine)
		// To do this, first check if each CPU_Unit mapping from other is defined in this SLA:
		for (actCPUTuple <- other.maxVMsPerCPU) {
			val actCPUUnit = actCPUTuple._1
			val index = this.maxVMsPerCPU.indexWhere(t => t._1.equals(actCPUUnit))
			if(index == -1)
				return false
			else{
				//If a match is found for the actCPUTuple, compare both Int-Values with each other:
				if(this.maxVMsPerCPU(index)._2 > actCPUTuple._2)
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


	private def getSmallerMaxValPerCPU(tuple: (CPU_Unit, Integer), otherMaxVMsPerCPU: Vector[(CPU_Unit, Integer)]): (CPU_Unit, Integer) = {
		//Find the element with the same CPU_Unit of tuple in otherMaxVMsPerCPU-Vector:
		val otherTuple: Option[(CPU_Unit, Integer)] = otherMaxVMsPerCPU.find(t => t._1.equals(tuple._1))

		//Then return the smaller Integer value from both of the (CPU_Unit, Integer) tuples:
		val newTuple : (CPU_Unit, Integer) = if(tuple._2 < otherTuple.get._2) tuple else otherTuple.get
		return newTuple
	}
}

/**
 *
 * @param priceRangePerCPU
 * @param priceRangePerRAM
 * @param priceRangePerStorage
 */
case class CloudSLA(	priceRangePerCPU:		 Vector[(CPU_Unit, Price, Price)],
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
