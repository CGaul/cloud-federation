package datatypes

import java.util.Currency

import datatypes.CPU_Unit.CPU_Unit
import datatypes.CloudCurrency.CloudCurrency
import datatypes.Img_Format.Img_Format

/**
 * Lists all possible virtualization image formats
 * that are accessible via libvirt.
 * from: http://libvirt.org/storage.html
 */
object Img_Format extends Enumeration {
	type Img_Format = Value
	val RAW, BOCHS, CLOOP, COW, DMG, ISO, QCOW, QCOW2, QED, VMDK, VPC, IMG = Value
}

object CloudCurrency extends Enumeration {
	type CloudCurrency = Value
	//val Currency.getAvailableCurrencies
	val CLOUD_CREDIT = Value
}

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
case class HardSLA(relOnlineTime: 		Float,
					supportedImgFormats:		Vector[Img_Format],
					maxVMsPerCPU:				Vector[(CPU_Unit, Integer)])
{

	override def equals(obj: scala.Any): Boolean = obj match{
		case that: HardSLA 	=> return (	this.supportedImgFormats.equals(that.supportedImgFormats) &&
		  											this.maxVMsPerCPU.equals(that.maxVMsPerCPU))
		case _ 					=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[HardSLA]


	def combineToAmplifiedSLA(other: HardSLA): HardSLA = {
		val amplRelOnlineTime = if (this.relOnlineTime >= other.relOnlineTime) this.relOnlineTime else other.relOnlineTime
		val amplSupportedImgFormats = this.supportedImgFormats ++ other.supportedImgFormats.filter(!this.supportedImgFormats.contains(_))
		val amplMaxVMsPerCPU = this.maxVMsPerCPU.map(getSmallerMaxValPerCPU(_, other.maxVMsPerCPU))

		return new HardSLA(amplRelOnlineTime, amplSupportedImgFormats, amplMaxVMsPerCPU)
	}


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
case class SoftSLA(	priceRangePerCPU:		 Vector[(CPU_Unit, Price, Price)],
						  	priceRangePerRAM:		 (ByteSize, Price, Price),
						  	priceRangePerStorage: (ByteSize, Price, Price))
{

	override def equals(obj: scala.Any): Boolean = obj match{
		case that: SoftSLA 	=> return (this.priceRangePerCPU.equals(that.priceRangePerCPU) &&
		  										  this.priceRangePerRAM.equals(that.priceRangePerRAM) &&
		  										  this.priceRangePerStorage.equals(that.priceRangePerStorage))
		case _ 					=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[SoftSLA]

}
