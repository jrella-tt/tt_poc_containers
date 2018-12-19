package com.timetrade.queueservice.server.core.persistence

import com.timetrade.queueservice.server.core.algorithms.{CustomAlgorithms, CustomAlgorithmsView}
import com.timetrade.queueservice.server.testtraits.ConfigurableActorSystems
import com.timetrade.queueservice.testtraits.TimedSuite
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.{Futures, ScalaFutures}
import org.scalatest.fixture.FunSpec
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, Tag, fixture, _}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

object DockerComposeTag extends Tag("DockerComposeTag")

class CustomAlgorithmsDAOSpec  extends fixture.FunSuite
  with Matchers
  with Futures
  with ScalaFutures
  with TypeCheckedTripleEquals
  with TimedSuite
  with fixture.ConfigMapFixture {

  val testLicenseeId = 878
  val testExternalId = "WTHA"

  def await[T](f: Future[T]): Unit = {
    implicit val _ = ExecutionContext.global
    f onFailure { case t: Throwable => t.printStackTrace }
    whenReady(f){ _ => }
  }

    // Configure the behavior of the "whenReady" calls below.
    implicit val defaultPatience = PatienceConfig(timeout =  Span(60, Seconds),
      interval = Span(100, Millis))


  def storeCustomAlgorithmsRecord(dao: CustomAlgorithmsDAO): CustomAlgorithms = {
    val wtha = getCustomAlgorithmsRecord
    await(dao.create(wtha))
    wtha
  }

  def cleanUp(dao: CustomAlgorithmsDAO) =
    await(dao.remove(testExternalId,testLicenseeId))

  def getCustomAlgorithmsRecord: CustomAlgorithms =
    CustomAlgorithms(
      testLicenseeId,
      testExternalId,
      4,
      "Test algorithm",
      "This test is used to calculated Wait Time History",
      "( E{ durationInProgress - timeSpentInProgress } + E{ durationInQueue + static } ) / Resource",
      true,
      22941472
    )

  def getCustomAlgorithmsViewRecord: CustomAlgorithmsView =
    CustomAlgorithmsView(
      testLicenseeId,
      testExternalId,
      4,
      "Test algorithm",
      "This test is used to calculated Wait Time History",
      "( E{ durationInProgress - timeSpentInProgress } + E{ durationInQueue + static } ) / Resource",
      true
    )

  test("Create Table", DockerComposeTag) {
    configMap =>{
      await(getDataStore().becomeReady)
      whenReady(getDataStore().datastore.tableExists(CustomAlgorithmsDAO.tableName)) {
        _ should be (true)
      }
    }
  }


  test("Insert Record", DockerComposeTag) {
    configMap =>{
      storeCustomAlgorithmsRecord(getDataStore())
      whenReady(getDataStore().get(testLicenseeId,4)) { recordList =>
        recordList.toSeq.size should === (1)
        recordList.toSeq.head === (getCustomAlgorithmsViewRecord)
        cleanUp(getDataStore())
      }
    }
  }







}