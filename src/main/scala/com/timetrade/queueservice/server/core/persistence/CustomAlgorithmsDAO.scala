package com.timetrade.queueservice.server.core.persistence

import akka.event.Logging
import scala.slick.jdbc.StaticQuery
import scala.slick.jdbc.JdbcBackend.Session
import scala.concurrent.Future
import scala.slick.jdbc.StaticQuery
import com.timetrade.queueservice.server.core.algorithms.CustomAlgorithms
import com.timetrade.queueservice.server.core.algorithms.CustomAlgorithmsView
import java.time.OffsetDateTime
import java.time.ZoneOffset



class CustomAlgorithmsDAO(val datastore: Datastore) extends BlockingFutures {
  private lazy val log = Logging.getLogger(datastore.loggingBus, this)
  
  // Use the same one for non-blocking Futures for now.
  private implicit val ec = datastore.ecForBlockingFutures
  
  import com.typesafe.slick.driver.ms.SQLServerDriver

   // The CustomAlgorithms table has rows shaped like tuples of these types.
  private type CustomAlgorithmsTuple = (
    Int,                  // Wait Time History Algorithm Id (Auto incrementing Integer)
    Int,                  // Licensee ID
    String,               // ExternalId
    Short,                // QueueWaitTimeRuleId
    String,               // CustomAlgorithmName
    String,               // Description
    String,               // Calculation
    Boolean,              // enabled
    Int,                  // CreatedByUserId
    String                // CreatedDate
  )
  
  // Import the query language features from the driver.
  lazy val jdbcProfile = datastore.jdbcProfile
  import jdbcProfile.simple.{Session => _, _}
  
  protected class CustomAlgorithmsTable(tag: Tag) 
    extends Table[CustomAlgorithmsTuple](tag, CustomAlgorithmsDAO.tableName) {

    def customAlgorithmId = column[Int]("Id", O.AutoInc, O.PrimaryKey)
    def licenseeId = column[Int]("LicenseeId", O.NotNull)
    def externalId = column[String]("ExternalId", O.NotNull)
    def queueWaitTimeRuleId = column[Short]("QueueWaitTimeRuleId", O.NotNull)
    def customAlgorithmName = column[String]("CustomAlgorithmName", O.NotNull)
    def description = column[String]("Description", O.NotNull)
    def calculation = column[String]("Calculation", O.NotNull)    
    def enabled = column[Boolean]("Enabled", O.NotNull)
    def createdByUserId = column[Int]("CreatedByUserId", O.NotNull)
    def createdDate = column[String]("CreatedDate", O.NotNull)
    
 

    def * = (
      customAlgorithmId,
      licenseeId,
      externalId,
      queueWaitTimeRuleId,
      customAlgorithmName,
      description,
      calculation,
      enabled,
      createdByUserId,
      createdDate
    )
  }
  
  //TableQuery
   private val customAlgorithmsTable = TableQuery[CustomAlgorithmsTable]
  
   /** Query to fetch a specific enabled custom algorithm */
  def get(licenseeId: Int, queueWaitTimeRuleId:Short): Future[List[CustomAlgorithmsView]] = blockingFutureWhenReady{ implicit session =>
      
    //SELECT * FROM CustomAlgorithms WHERE LicenseeId = x
    customAlgorithmsTable.filter(wta=> ((wta.licenseeId === licenseeId) && (wta.enabled=== true) && (wta.queueWaitTimeRuleId === queueWaitTimeRuleId)))
      .list
      .map {case(row) => applyTuple(row)}
  }

  def create(customAlgorithms: CustomAlgorithms): Future[Unit] = blockingFutureWhenReady { implicit session =>
    session.withTransaction {
      //make disable previous instances of custom algorithm
      val preUpdate = for { ca <- customAlgorithmsTable if ((ca.externalId === customAlgorithms.externalId) && (ca.licenseeId === customAlgorithms.licenseeId)) } yield ca.enabled
      preUpdate.update(false)
      preUpdate.updateInvoker
      
      //INSERT INTO CustomAlgorithms(LicenseeId, ExternalId,  QueueWaitTimeRuleId,  CustomAlgorithmName ,Description, Calculation, Enabled, CreatedByUserId, CreatedDate) VALUES ()
      customAlgorithmsTable.map(ca => ( ca.licenseeId,ca.externalId, ca.queueWaitTimeRuleId,  ca.customAlgorithmName,ca.description, ca.calculation, ca.enabled, ca.createdByUserId, ca.createdDate )) += (getTuple(customAlgorithms))
    
      customAlgorithmsTable.insertInvoker
    }
  }
  
    def remove(externalId: String, licenseeId:Int): Future[Int] = blockingFutureWhenReady { implicit session => 
    //DELETE FROM CustomAlgorithms WHERE ExternalId = x AND LicenseeId = x
    customAlgorithmsTable
      .filter { wta=> ((wta.externalId === externalId) && (wta.licenseeId === licenseeId))}
      .delete
  }
     private def createTable(): Future[Unit] = blockingFuture { implicit session =>
       var statement = ""
       val ddl = customAlgorithmsTable.ddl
       datastore.db withDynSession {
         statement = ddl.createStatements.next()
       }
    StaticQuery.updateNA(statement).execute
    log.info("Created table {} in datastore {} at {}",
              CustomAlgorithmsDAO.tableName, datastore.jdbcUrl.toString, System.currentTimeMillis)
  }
  
  private def createTableIfNonExistent: Future[Boolean] = {
    val msg = s"Initializing ${CustomAlgorithmsDAO.tableName} for ${datastore.jdbcUrl}..."
    println(msg)

    try {
      datastore.tableExists(CustomAlgorithmsDAO.tableName) flatMap { exists =>
        if (exists) {
          println("Table already exists")
          Future.successful(false)
        } else {
          println("Table does not already exist. Creating.")
          val f = createTable()
            .map { _ => true }
            // Deal with two threads racing and the second one to attempt creation fails.
            .recoverWith {
              case t: Throwable =>
                // The failure to create may be due to another thread beating me to it.
                datastore.tableExists(CustomAlgorithmsDAO.tableName) map { exists2 =>
                  if (exists2) {
                    println("Table did not already exist when I first looked but now it does")
                    false
                  } else throw t
                }
            }
          f.onSuccess {
            case _ =>
              println("Table did not already exist. Created.")
          }
          f.onFailure { case t: Throwable => log.error(t, "Table creation failed") }
          f map { _ => true}
        }
      }
    } finally {
      println(msg + "done")
    }
  }

    // Create the table if it does not yet exist, and provide a way for
  // clients to wait for that creation to finish.
  lazy val becomeReady: Future[Boolean] = createTableIfNonExistent

  // Run a block of code:
  //  - in a Future declared to be blocking
  //  - on the ExecutionContext designated for blocking Futures
  //  - with a db Session
  //  - after the table has been created
  implicit val _ = datastore
  private def blockingFutureWhenReady[T](block: Session => T): Future[T] =
    becomeReady flatMap { _ => blockingFuture(block) }

  // Run a block of code:
  //  - in a Future declared to be blocking
  //  - on the ExecutionContext designated for blocking Futures
  //  - with a db Session
  private def blockingFuture[T](block: Session => T): Future[T] =
    blockingFutureOn(datastore.ecForBlockingFutures)(datastore.db.withSession(block))
    
     private def applyTuple(tu: CustomAlgorithmsTuple):CustomAlgorithmsView =
    CustomAlgorithmsView(
        tu._2,
        tu._3,
        tu._4,
        tu._5,
        tu._6,
        tu._7,
        tu._8)
        
   private def getTuple(customAlgorithms: CustomAlgorithms) = (       
     customAlgorithms.licenseeId,
     customAlgorithms.externalId,
     customAlgorithms.queueWaitTimeRuleId, 
     customAlgorithms.customAlgorithmName,
     customAlgorithms.description,
     customAlgorithms.calculation,
     customAlgorithms.enabled,
     customAlgorithms.createdByUserId,
     OffsetDateTime.now(ZoneOffset.UTC).toString()
  )

}
object CustomAlgorithmsDAO {
  val tableName = "CustomAlgorithms"
}