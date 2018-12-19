package com.timetrade.queueservice.server.core.persistence

import scala.concurrent.Future
import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.meta.MTable

import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.LoggingBus

import PimpedDatabase._PimpedDatabase

class PostGresDatastore(val settings: PersistenceSettings, val loggingBus: LoggingBus)
                       (implicit system: ActorSystem)
    extends ExceptionLogging
    with BlockingFutures {
  
  val log = Logging.getLogger(loggingBus, classOf[PostGresDatastore])

  /** Return the specified by the settings. */
  def jdbcUrl: JdbcUrl = settings.postHistoryUrl

  /** Return the Database specified by the settings */
  lazy val db: Database = Database.forURL(jdbcUrl.s, driver = jdbcUrl.matchingJdbcDriver.getOrElse(SlickDriverNames.PostGresDriverName))
  
  /** the ExecutionContext to be used for blocking db operations. */
  val ecForBlockingFutures =
    MeteredExecutionContext.perActorSystem(
      blockagesMeterLabelPrefix = "PostDatabase")

  /** Return the JdbcProfile implied by the settings. */
  def jdbcProfile: JdbcProfile = settings.postHistoryUrl.matchingSlickLiftedJdbcProfile.get

  /** Test if the database can be reached. */
  def isReachable: Future[Boolean] = blockingFutureOn(ecForBlockingFutures) {
    PostGresDatastore.isReachable(db)
  }

  /** Test if a table exists. */
  def tableExists(name: String): Future[Boolean] = blockingFutureOn(ecForBlockingFutures) {
    try {
      val result = PostGresDatastore.tableExists(db, name)
      log.debug("Table {} existence in db {} = {}", name, jdbcUrl.toString, result)
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
object PostGresDatastore {
  
  /** Returns a Datastore instance suitable for most automated tests.
    *
    * @param system the ActorSystem in effect.
    */
  def defaultForTesting(name:String)(implicit system: ActorSystem): PostGresDatastore =
    new PostGresDatastore(PersistenceSettings.defaultForTestingPostGres(name), system.eventStream)

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
   
      !MTable.getTables(None, None, Some(name), None).list.isEmpty
    }

  // This turns down the annoying c3p0 logging.
  sys.props += ("com.mchange.v2.log.MLog" -> "com.mchange.v2.log.FallbackMLog")
  sys.props += ("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL" -> "WARNING")
}
