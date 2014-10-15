package datatypes

import datatypes.CPU_Unit._

/**
 * Created by costa on 10/15/14.
 */
object CPU_Unit extends Enumeration{
	type CPU_Unit = Value
	val SMALL, MEDIUM, LARGE, EXTRA_LARGE = Value
}
//CPU Speed per Node [CPU_Unit]
//Amount of RAM per Node [MB]
/**
 *
 * @param nodeIDs All NodeIDs that are used inside this Resource-Container are listed in this Vector.
 *                May be None for Resource Requests.
 * @param cpu CPU Speed per Node [CPU_Unit]
 * @param ram Amount of RAM per Node [MB]
 * @param storage Amount of Storage per Node [MB]
 * @param bandwidth Bandwidth, relatively monitored from GW to Node [KB]
 * @param latency Latency, relatively monitored from GW to Node [ms]
 */
case class Resources(nodeIDs: Vector[Integer],
							cpu: Vector[(Integer, CPU_Unit)],
							ram: Vector[(Integer, Integer)],
							storage: 	Vector[(Integer, Integer)],
							bandwidth:	Vector[(Integer, Integer)],
							latency:	Vector[(Integer, Float)])
//	val nodeIDs: 	Vector[Integer]
//	val cpu: 		Vector[(Integer, CPU_Unit)] //CPU Speed per Node [CPU_Unit]
//	val ram: 		Vector[(Integer, Integer)]  //Amount of RAM per Node [MB]
//	val storage: 	Vector[(Integer, Integer)]  //Amount of Storage per Node [MB]
//
//	val bandwidth:	Vector[(Integer, Integer)]  //Bandwidth, relatively monitored from GW to Node [KB]
//	val latency:	Vector[(Integer, Float)]  	 //Latency, relatively monitored from GW to Node [ms]
