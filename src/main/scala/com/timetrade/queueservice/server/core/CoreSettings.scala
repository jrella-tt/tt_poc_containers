/** Copyright(c) 2013-2016 by TimeTrade Systems.  All Rights Reserved. */
package com.timetrade.queueservice.server.core

import java.util.concurrent.atomic.AtomicInteger

import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationInt
import scala.concurrent.stm.TMap
import scala.concurrent.stm.atomic

import akka.actor.ActorSystem

import com.timetrade.queueservice.server.core.activation.ActorPlacementStrategy
import com.timetrade.queueservice.server.core.activation.LocationActorToOther
import com.timetrade.queueservice.server.core.persistence.Datastore
import com.timetrade.queueservice.server.core.persistence.JdbcUrl
import com.timetrade.queueservice.server.core.persistence.PersistenceSettings
import com.timetrade.queueservice.server.core.persistence.PostGresDatastore

/** Contains settings pertaining to Core but not storage. */
case class CoreSettings(

  /** Needed only for testing to prevent two subsequent Spray route tests from
    * trying to re-use a name.
    */
  clusterSingletonManagerNameSuffix: String,

  /** The period of idleness after which a queue may deactivate itself.
    * Idleness means no events arriving for it and no one reading its output.
    */
  queueIdleTimeout: FiniteDuration,

  /** The limit to the amount of time we will wait for a queue to activate when requested to.
    */
  queueActivationTimeout: FiniteDuration,

  /** The limit to the amount of time we will wait for a queue to deactivate when requested to.
    */
  queueDeactivationTimeout: FiniteDuration,

  /** The base, minimum duration for which a ticket is valid.
    */
  ticketLifetime: FiniteDuration,

  /** We add a random adjustment, of up to this duration, to the actual lifetime of a new ticket.
    * This is intended to prevent a large set of tickets from all expiring at the same time, thus
    * causing a "thundering herd" of clients creating new tickets.
    */
  ticketLifetimeDeltaMax: FiniteDuration,

  /** Amount of time between when we delete expired tickets.
    */
  periodBetweenExpiredTicketEvictions: FiniteDuration,

  /** If <code>true</code>, attempt to periodically rebalance work across a
    * cluster.
    */
  clusterLoadRebalanceAttempt: Boolean,
  
  /** Amount of time between out attempt to rebalance the work across a loaded cluster.
    */
  clusterLoadRebalanceAttemptPeriod: FiniteDuration,

  /** Cluster node capacities are between the values of 1.0 (which signifies
    * a completely unloaded node) and 0.0 (which signifies a completely
    * loaded node).  The capacity difference between the most- and least-
    * loaded nodes can be interpreted as an imbalance in load across the
    * cluster.
    *
    * We're not interested in attempting to rebalance unless the most-loaded
    * node's capacity is <= this threshold.
    */
  clusterLoadRebalanceMostBusyThreshold: Double,
  
  /** We're also not interested in attempting to rebalance unless the capacity
    * difference between least- and most-loaded nodes >= this threshold.
    */
  clusterLoadRebalanceRangeThreshold: Double,
  
  /** Amount of time a client request (long poll, or event source) can remain un-responded.
    */
  clientTimeout: FiniteDuration,

  /** Number of actors in the pool performing ActiveQueueRegistrar operations. */
  activeQueueRegistrarPoolSize: Int,

  /** Number of actors in the pool performing TicketingActor operations. */
  ticketingActorPoolSize: Int,

  /** Settings pertaining to persistence. */
  persistenceSettings: PersistenceSettings,

  /** How placement of actors on cluster nodes is decided. */
  actorPlacementStrategy: ActorPlacementStrategy,

  /** Interval between server-pushed updates of cluster-status, to feeds of same. */
  clusterStatusRefreshInterval: FiniteDuration,

  /** The configured Akka addresses of nodes in the cluster. */
  configuredClusterAddresses: Seq[String],

  /** Maximum size of chunks when we are transmitting large payloads in chunks. */
  maxMsgChunkSize: Int,

  /** A value used in the computation of cluster node capacity.  Should be between 1.0 and 0.5,
    * where 1.0 means that the node carrying cluster singletons should be treated the same as any
    * other node, and a value of 0.5 means that node should be treated as 50% busier than it really
    * is.
    */
  clusterSingletonResidentNodeCapacityBias: Double
) {
  /** Get a Datastore based on the settings. */
  def datastore(implicit system: ActorSystem) = CoreSettings.getSingleton(persistenceSettings)
  
  /** Get a PostGres Datastore based on the settings */
  def postDataStore(implicit system: ActorSystem) = CoreSettings.getPostSingleton(persistenceSettings)
}

/** Companion object provides suitable instance for automated tests. */
object CoreSettings {

  /** We're using STM to hold a per-JVM singleton map of JDBC URLs to Datastores.
    * (In practice, we never expect this map to contain more than one entry.)
    *
    * We would place the Datastore reference in a CoreSettings object, but Datastore is not
    * serializable, and actors with references to CoreSettings are thus not serializable either,
    * which is a problem in a cluster.
    */
  private val tmap = TMap[JdbcUrl, Datastore]()
  
  private val postTmap = TMap[JdbcUrl, PostGresDatastore]()

  /** These defaults should only be used for unit testing which may not read the application.conf.
    * Otherwise the application.conf will always contain these settings.
    * Note: being a "def", this gets a fresh H2 database on each call.
    */
  def defaultForTesting =
    CoreSettings(
      clusterSingletonManagerNameSuffix = "",
      clientTimeout = 5.seconds,
      queueIdleTimeout = 30.seconds,
      queueActivationTimeout = 30.seconds,
      queueDeactivationTimeout = 30.seconds,
      ticketLifetime = 18.hours,
      ticketLifetimeDeltaMax = 1.hour,
      periodBetweenExpiredTicketEvictions = 4.minutes,
      clusterLoadRebalanceAttempt = true,
      clusterLoadRebalanceAttemptPeriod = 15.seconds,
      clusterLoadRebalanceMostBusyThreshold = 0.5,
      clusterLoadRebalanceRangeThreshold = 0.2,
      activeQueueRegistrarPoolSize = 1,
      ticketingActorPoolSize = 1,
      persistenceSettings = PersistenceSettings.defaultForTesting,
      actorPlacementStrategy = LocationActorToOther,
      clusterStatusRefreshInterval = 1.seconds,
      configuredClusterAddresses = Seq("akka.tcp://QueueServer@localhost:2551"),
      maxMsgChunkSize = 128*1024,
      clusterSingletonResidentNodeCapacityBias = 1.0
      )

  private val counter = new AtomicInteger(0)

  // Generate a set of defaults with a new suffix for singleton naming.
  def defaultForTestingWithUniqueSingletonNameSuffix() =
    defaultForTesting.copy(clusterSingletonManagerNameSuffix = counter.getAndIncrement.toString)

  /** Uses STM (Software Transactional Memory) to store a singleton Datastore for a given JDBC URL.
    * @return the Datastore appropriate for psersistenceSettings' JDBC URL
    */
  def getSingleton(persistenceSettings: PersistenceSettings)(implicit system: ActorSystem): Datastore = {
    atomic { implicit txn =>
      tmap.getOrElseUpdate(persistenceSettings.url, new Datastore(persistenceSettings, system.eventStream))
    }
  }
  
  def getPostSingleton(persistenceSettings: PersistenceSettings)(implicit system: ActorSystem): PostGresDatastore = {
    atomic{ implicit txn =>
      postTmap.getOrElseUpdate(persistenceSettings.postHistoryUrl, new PostGresDatastore(persistenceSettings, system.eventStream) )
    }
  }
  
}
