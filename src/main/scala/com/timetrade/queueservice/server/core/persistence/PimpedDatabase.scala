/** Copyright(c) 2013-2014 by TimeTrade Systems.  All Rights Reserved. */
package com.timetrade.queueservice.server.core.persistence

import scala.slick.jdbc.JdbcBackend.Database

import com.mchange.v2.c3p0.ComboPooledDataSource

/** Object using the case-class pattern to enrich the Slick Database object to provide
  * convenient functions.
  */
object PimpedDatabase {

  implicit class _PimpedDatabase(val db: Database.type) extends AnyVal {

    /** Create a Database from a JdbcUrl with optional connection pooling
      * @param jdbcUrl The URL for the database
      * @param maxConnectionPoolSize if specified a connectionpool is created, otherwise not
      */
    def forJdbcUrl(jdbcUrl: JdbcUrl, maxConnectionPoolSize: Option[Int]): Database = {
      maxConnectionPoolSize
        .map { size =>
          db.forDataSource({
            // Construct a DataSource which uses c3p0's connection pooling.
            val cpds = new ComboPooledDataSource
            cpds.setDriverClass(
              jdbcUrl.matchingJdbcDriver.getOrElse(sys.error(s"No JDBC driver for ${jdbcUrl.s}")))
            cpds.setJdbcUrl(jdbcUrl.s)
            cpds.setMaxPoolSize(size)
            cpds
          })
        }
        .getOrElse (
          Database.forURL(
            jdbcUrl.s,
            driver = jdbcUrl.matchingJdbcDriver.getOrElse(sys.error(s"No JDBC driver for ${jdbcUrl.s}"))))
    }
  }
}
