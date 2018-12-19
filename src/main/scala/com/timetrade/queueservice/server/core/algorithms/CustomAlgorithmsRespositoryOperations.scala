package com.timetrade.queueservice.server.core.algorithms

import scala.concurrent.Future

import com.timetrade.queueservice.server.core.Core


trait CustomAlgorithmsRespositoryOperations { self: Core =>
  /**
   * Get a list of records of queue_wait_time_audit table
   * @param licenseeId the LicenseeId
   * @param confirmationNumber the Confirmation Number
   * @return a List of WaitTimeHistory
   */
  def getCustomAlgorithmByLicenseeAndQueueWaitTimeRuleId(
    licenseeId: Int, queueWaitTimeRuleId: Short): Future[List[(CustomAlgorithmsView)]] = customAlgorithmsDAO.get(licenseeId, queueWaitTimeRuleId)

  def createCustomAlgorithms(customAlgorithms: CustomAlgorithms): Future[Unit] = { customAlgorithmsDAO.create(customAlgorithms) }

  def deleteCustomAlgorithms(customAlgorithmsExternalId:String,licenseeId:Int): Future[Int] = { customAlgorithmsDAO.remove(customAlgorithmsExternalId, licenseeId) }
}

