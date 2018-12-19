/** Copyright(c) 2013-2016 by TimeTrade Systems.  All Rights Reserved. */
package com.timetrade.queueservice.server.core.persistence

import java.io.File
import java.util.UUID

import scala.slick.driver.H2Driver
import scala.slick.driver.JdbcProfile
import scala.slick.driver.PostgresDriver

import com.typesafe.slick.driver.ms.SQLServerDriver

/** Value type for JDBC "URL"s.
  *
  * @param s the string representation of the URL.
  */
case class JdbcUrl (s: String) extends AnyVal {

  /** @return the class name for the appropriate JDBC driver, if any.
    * This is all that is needed to perform Slick static query operations.
    */
  def matchingJdbcDriver: Option[String] =
    if (s startsWith s"jdbc:h2") Some(SlickDriverNames.H2DriverName)
    else if (s startsWith s"jdbc:jtds:") Some(SlickDriverNames.JTDSDriverName)
    else if (s startsWith s"jdbc:sqlserver:") Some(SlickDriverNames.MSSQLDriverName)
    else if (s startsWith s"jdbc:postgresql:") Some(SlickDriverNames.PostGresDriverName)
    else None 

    /** @return the appropriate Slick JdbcProfile, if any.
      * This is only needed when using Slick's lifted embedding operations.
      */
  def matchingSlickLiftedJdbcProfile: Option[JdbcProfile] =
    if (s startsWith s"jdbc:h2") Some(H2Driver)
    else if (s startsWith s"jdbc:jtds:") Some(SQLServerDriver)
    else if (s startsWith s"jdbc:sqlserver:") Some(SQLServerDriver)
    else if (s startsWith s"jdbc:postgresql:") Some(PostgresDriver)
    else None
}

/** Companion object provides some factory methods. */
object JdbcUrl {

  /** Easily obtainable instance for testing. */
  def defaultForTesting = defaultForMsTesting
  //def defaultForTesting = defaultForH2Testing

  /** Named in-memory db. */
  def forH2(name: String): JdbcUrl =
    // "DB_CLOSE_DELAY=-1" ensures that the DB survives disconnecting the last connection.
    apply(s"jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1")

  /** Named in-file db. */
  def forH2(file: File): JdbcUrl = apply(s"jdbc:h2:file:${file.getAbsolutePath}")

  /** H2 file-resident, muti-proc safe.
    * See here: http://www.h2database.com/html/features.html#auto_mixed_mode
    */
  def forSharedOnDiskH2(name: String): JdbcUrl = {
    require(new File("/var/tmp").isDirectory)

    apply(s"jdbc:h2:/var/tmp/${name};AUTO_SERVER=TRUE")
  }

  /** Microsoft SQL Server db. */
  def forMS(dbName: String, host: String, port: Int, user: String, password: String, failoverPartner: Option[String], sendStringParametersAsUnicode: Option[String]) =
    apply(s"jdbc:sqlserver://$host:$port;databaseName=$dbName;user=$user;password=$password"
          +
          (failoverPartner
             .map{ p => s";failoverPartner=$p"}
             .getOrElse(""))
          +
          (sendStringParametersAsUnicode
             .map{ p => s";sendStringParametersAsUnicode=$p"}
             .getOrElse("false"))
         )

  def forJtds(dbName: String, host: String, port: Int, user: String, password: String) =
    apply(s"jdbc:jtds:sqlserver://$host:$port/$dbName;user=$user;password=$password;")
   
  def forPostgres(dbName: String, host: String, port: Int, user: String, password: String) = 
    apply(s"jdbc:postgresql://$host:$port/$dbName?user=$user&password=$password")

  /** Try to parse a string to one of our supported database URLs. */
  def parse(s: String): Option[JdbcUrl] = {

    val H2Mem = """jdbc:h2:mem:([a-zA-Z0-9]+;DB_CLOSE_DELAY=-1)""".r

    val H2File = """jdbc:h2:file:([a-z0-9]+)""".r

    val H2SharedFile = """jdbc:h2:/var/tmp/([a-zA-Z0-9]+);AUTO_SERVER=TRUE""".r

    // ";Failover Partner=PartnerServerName"
    val Microsoft = """jdbc:sqlserver://([^:]+):([\d]+);databaseName=([^;]+);user=([^;]+);password=([^;]+);sendStringParametersAsUnicode=(true|false)""".r

    val MicrosoftWithFailover =
      """jdbc:sqlserver://([^:]+):([\d]+);databaseName=([^;]+);user=([^;]+);password=([^;]+);failoverPartner=([^;]+);sendStringParametersAsUnicode=(true|false)""".r

    val Jtds = """jdbc:jtds:sqlserver://([^:]+):([\d]+)/([^;]+);user=([^;]+);password=([^;]+)""".r
    
    val PostGres = """jdbc:postgresql://([^:]+):([\d]+)/([^;]+);user=([^;]+);password=([^;]+)""".r

    s match {
      case H2Mem(dbName) => Some(forH2(dbName))

      case H2File(file) => Some(forH2(new File(file)))

      case H2SharedFile(name) => Some(forSharedOnDiskH2(name))

      case Microsoft(host, port, dbName, user, password, sendStringParametersAsUnicode) =>
        Some(forMS(dbName, host, Integer.parseInt(port), user, password, None, if ("" == sendStringParametersAsUnicode) None else Some(sendStringParametersAsUnicode)))

      case MicrosoftWithFailover(host, port, dbName, user, password, partner, sendStringParametersAsUnicode) =>
        Some(forMS(dbName, host, Integer.parseInt(port), user, password, Some(partner), if ("" == sendStringParametersAsUnicode) None else Some(sendStringParametersAsUnicode)))

      case Jtds(host, port, dbName, user, password) =>
        Some(forJtds(dbName, host, Integer.parseInt(port), user, password))
       
      case PostGres(host, port, dbName, user, password) =>
        Some(forPostgres(dbName, host, Integer.parseInt(port), user, password))

      case _ => None
    }
  }

  // This should be used for testing almost always. A fresh db (and name) is created
  // by each call to this.
  private def defaultForH2Testing = forH2(UUID.randomUUID.toString)

  // Under rare circumstances this can be used for testing but only by one developer at a time.
  // TODO: do such a test with the  recent static query changes
  private def defaultForMsTesting = forMS("master", "172.18.0.2", 1433, "sa", "timeTrade22!", None, Some("true"))
}
