package com.timetrade.queueservice.server.core.persistence

/** Names of Slick drivers we support for DB access */
object SlickDriverNames {

  /** The H2 driver. */
  val H2DriverName = "org.h2.Driver"

  /** The JTDS driver. */
  val JTDSDriverName = "net.sourceforge.jtds.jdbc.Driver"

  /** The MSSQL driver. */
  val MSSQLDriverName = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
  
  /** PostGreSQL driver. **/
  val PostGresDriverName = "org.postgresql.Driver"
}
