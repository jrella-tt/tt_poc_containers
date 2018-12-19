/** Copyright(c) 2013-2014 by TimeTrade Systems.  All Rights Reserved. */
package com.timetrade.queueservice.server.core

import java.util.concurrent.atomic.AtomicInteger

import scala.collection.immutable.Seq
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._

import akka.ConfigurationException
import akka.actor.ActorSystem
import akka.actor.Address
import akka.actor.PoisonPill
import akka.actor.actorRef2Scala
import akka.cluster.Cluster
import akka.cluster.singleton.ClusterSingletonManager
import akka.cluster.singleton.ClusterSingletonManagerSettings
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.Materializer


import com.timetrade.queueservice.server.core.persistence.CustomAlgorithmsDAO
import com.timetrade.queueservice.server.core.algorithms.CustomAlgorithmsRespositoryOperations

/** Provides Core layer of the application.
  *
  * @constructor  creates a new instance
  * @param datastore the Datastore instance to use
  * @param settings CoreSettings to use
  * @param actorSystem the application's ActorSystem
  * @param materializer the application's Materializer
  */
class Core(val settings: CoreSettings)
          (implicit val actorSystem: ActorSystem, val materializer: Materializer)
  extends  CustomAlgorithmsRespositoryOperations {

  import actorSystem.dispatcher

  protected lazy val log = Logging.getLogger(actorSystem, classOf[Core])

  val datastore = settings.datastore
  
  lazy val customAlgorithmsDAO = new CustomAlgorithmsDAO(datastore)

  log.info("Config origin: {}", actorSystem.settings.config.origin.description)
  log.info("CoreSettings: {}", settings)

}

/** Companion object, provides factory method. */
object Core {

  /** Return settings suitable for automated test runs.
    *
    * Note: the following parameter is not implicit to force the test writer
    * to create an ActorSystem to avoid running the risk of using an implicit one
    * brought into scope by some test trait being used.
    *
    * @param system the test instance's ActorSystem
    */
  def defaultForTesting(system: ActorSystem) =
    new Core(settings = CoreSettings.defaultForTesting)(system, ActorMaterializer()(system))

  // Return settings suitable for automated test runs.
  // Use a new and unique suffix for cluster singleton manager names.
  def defaultForTestingWithFreshSingletonNames(system: ActorSystem) =
    new Core(
      settings = CoreSettings.defaultForTestingWithUniqueSingletonNameSuffix())(system, ActorMaterializer()(system))

}
