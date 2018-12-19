package com.timetrade.queueservice.server.core.algorithms

//import spray.json.DefaultJsonProtocol

case class CustomAlgorithmsView(
  licenseeId: Int,  
  externalId: String,
  queueWaitTimeRuleId: Short,  
  customAlgorithmName: String,
  description: String,
  calculation: String,
  enabled: Boolean = true
)

/** Companion object provides JSON conversion. */
object CustomAlgorithmsView  {
  //implicit val _ = jsonFormat7(apply)
}