/** Copyright(c) 2013-2014 by TimeTrade Systems.  All Rights Reserved. */
package com.timetrade.queueservice.server.core.persistence

/** Settings pertaining to storage.
  *
  * @param url the JdbcUrl for the database
  * @param maxConnectionPoolSize maximum size of the c3p0 connection pool
  */
case class PersistenceSettings(url: JdbcUrl, maxConnectionPoolSize: Option[Int], postHistoryUrl: JdbcUrl)

/** Companion object. Provides values suitable for most automated tests. */
object PersistenceSettings {
  
  def defaultForTestingPostGres(name: String) =
    PersistenceSettings(JdbcUrl.defaultForTesting,
                        None, JdbcUrl.parse(name).get)
                        
  // Note that each call gets a new database.
  def defaultForTesting =
    PersistenceSettings(JdbcUrl.defaultForTesting,
                        // Don't use connection pooling when running automated tests:
                        None, JdbcUrl.defaultForTesting)
}

