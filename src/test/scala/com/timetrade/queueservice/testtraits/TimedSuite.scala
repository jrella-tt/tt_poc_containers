package com.timetrade.queueservice.testtraits

import com.timetrade.queueservice.server.core.persistence.{CustomAlgorithmsDAO, Datastore}
import com.timetrade.queueservice.server.testtraits.ConfigurableActorSystems
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEachTestData
import org.scalatest.Suite
import org.scalatest.TestData

/** Adds per-suite timing to a ScalaTest suite. */
trait TimedSuite
  extends BeforeAndAfterAll
  with BeforeAndAfterEachTestData with ConfigurableActorSystems { self: Suite =>

  private var begunAt: Long = 0
  private var dao:CustomAlgorithmsDAO = null

  override def beforeAll(): Unit = {
    begunAt = System.currentTimeMillis
    //val system = defaultActorSystem()
    val datastore = Datastore.defaultForTesting(null)
    dao = new CustomAlgorithmsDAO(datastore)
  }

  override def afterAll(): Unit = {
    val duration = System.currentTimeMillis - begunAt
    println(s"Suite duration for ${this.getClass.getSimpleName} was ${duration.toString} msec")
  }

  protected def getDataStore():CustomAlgorithmsDAO = {
    dao
  }


  // Make it easy to count the actual number of tests run.
  override def beforeEach(testData: TestData) = println(s"Beginning test ${testData.name}")
  override def afterEach(testData: TestData) = println(s"Ending test ${testData.name}")
}
