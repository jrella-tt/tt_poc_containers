package com.timetrade.queueservice.server.core.algorithms

import spray.json.DefaultJsonProtocol


case class CustomAlgorithms(
  licenseeId: Int,  
  externalId: String,
  queueWaitTimeRuleId: Short,  
  customAlgorithmName: String,
  description: String,
  calculation: String,
  enabled: Boolean = true,
  createdByUserId: Int 
)

/** Companion object provides JSON conversion. */
object CustomAlgorithms   extends DefaultJsonProtocol{
  val ewtCustomAlgorithm:Short = 4
  implicit val _ = jsonFormat8(apply)
}