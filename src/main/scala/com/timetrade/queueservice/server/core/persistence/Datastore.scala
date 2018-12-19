/** Copyright(c) 2013-2014 by TimeTrade Systems.  All Rights Reserved. */
package com.timetrade.queueservice.server.core.persistence

import scala.concurrent.Future
import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.meta.MTable

import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.LoggingBus
import PimpedDatabase._PimpedDatabase



/** Encapsulates place where we persist data.
  *
  * @constructor
  * @param settings the [[PersistenceSettings]] to use
  * @param loggingBus where to log database operations
  *
  */
class Datastore(val settings: PersistenceSettings, val loggingBus: LoggingBus)
               (implicit system: ActorSystem)
  //extends ExceptionLogging
extends BlockingFutures {

 // val log = Logging.getLogger(loggingBus, classOf[Datastore])

  /** Return the specified by the settings. */
  def jdbcUrl: JdbcUrl = settings.url

  /** Return the Database specified by the settings */
  lazy val db: Database = Database.forJdbcUrl(jdbcUrl, settings.maxConnectionPoolSize)

  /** the ExecutionContext to be used for blocking db operations. */
  val ecForBlockingFutures =
    MeteredExecutionContext.perActorSystem(
      maxConcurrency = settings.maxConnectionPoolSize.getOrElse(4),
      blockagesMeterLabelPrefix = "Database")

  /** Return the JdbcProfile implied by the settings. */
  def jdbcProfile: JdbcProfile = settings.url.matchingSlickLiftedJdbcProfile.get

  /** Test if the database can be reached. */
  def isReachable: Future[Boolean] = blockingFutureOn(ecForBlockingFutures) {
    Datastore.isReachable(db)
  }

  /** Test if a table exists. */
  def tableExists(name: String): Future[Boolean] = blockingFutureOn(ecForBlockingFutures) {
    try {
      val result = Datastore.tableExists(db, name)
      //log.debug("Table {} existence in db {} = {}", name, jdbcUrl.toString, result)
      result
    } catch {
      // Temporary diagnostic to catch elusive test failure.
      case t: Throwable =>
        t.printStackTrace
        throw t
    }
  }

  /** Release resources. */
  def close() = ecForBlockingFutures.shutdown()
}

/** Companion object. */
object Datastore {

  /** Returns a Datastore instance suitable for most automated tests.
    *
    *
    * Note: the following parameter is not implicit to force the test writer
    * to create an ActorSystem to avoid running the risk of using an implicit one
    * brought into scope by some test trait being used.
    *
    * @param system the ActorSystem in effect.
    */
  def defaultForTesting(implicit system: ActorSystem): Datastore =
    new Datastore(PersistenceSettings.defaultForTesting, null)

  /** Test if the database can be reached.
    * @param jdbcUrl the JDBC URL for the database
    */
  def isReachable(jdbcUrl: JdbcUrl): Boolean = isReachable(Database.forJdbcUrl(jdbcUrl, None))

  /** Test if the database can be reached.
    * @param db the Database
    */
  def isReachable(db: Database): Boolean = {
    try {
      db withSession { session =>
        session.conn.close()
        true
      }
    } catch { case t: Throwable => false }
  }

  /** Test if a table exists.
    * @param db the Database
    * @param name the name of the table
    */
  def tableExists(db: Database, name: String): Boolean =
    db withSession { implicit session =>
      //println(MTable.getTables(None, None, None, None).list.mkString("\n").toString)
      // This long form call was necessary on MSSQL using either driver MS or jTDS
      !MTable.getTables(None, None, Some(name), None).list.isEmpty
    }

  // This turns down the annoying c3p0 logging.
  sys.props += ("com.mchange.v2.log.MLog" -> "com.mchange.v2.log.FallbackMLog")
  sys.props += ("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL" -> "WARNING")
}

