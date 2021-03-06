package com.timetrade.queueservice.server.testtraits

import java.io.File
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.timetrade.queueservice.netutils.NetUtils
import com.timetrade.queueservice.server.testutils.Utils

/** Trait to allow us to have control over the configuration of the ActorSystem with which
  * a given test is run. In general an ActorSystem configured with the production "application.conf"
  * may not be suitable.
  */
trait ConfigurableActorSystems {

  private lazy val DefaultConfigForTests = {
    val file = new File("src/test/resources/test-application.conf")
    require(file.isFile)
    ConfigFactory.parseFile(file)
  }

  /** Create an actor system configured with "test-application.conf" */
  def defaultActorSystem(): ActorSystem = defaultActorSystem(getClass.getSimpleName)

  /** Create an actor system configured with "test-application.conf" */
  def defaultActorSystem(name: String): ActorSystem = ActorSystem(name, DefaultConfigForTests)

  /** Create an actor system with clustering features enabled. */
  def actorSystemWithClustering(): ActorSystem = actorSystemWithClustering(getClass.getSimpleName)

  /** Create an actor system with clustering features enabled. */
  def actorSystemWithClustering(name: String): ActorSystem =
    actorSystemsWithClustering(name, 1).head

  /** Create a collection of actor systems, all seed nodes, with clustering features enabled. */
  def actorSystemsWithClustering(name: String, count: Int): Seq[ActorSystem] = {

    require(count > 0)

    val hostname = "localhost"

    // Find ports to use for Akka transport.
    val ports = Utils.temporaryServerPorts(n = count)

    // Check ports are available
    ports foreach { port => require (!NetUtils.isServerListening(hostname, port)) }

    // Make them all seeds
    val seedNodeText =
      ports
        .map { port => s""" "akka.tcp://${name}@${hostname}:${port}" """ }
        .mkString(", ")

    ports
      .map { port =>
        val configFragment =
          s"""|akka {
              |  loggers = ["akka.event.slf4j.Slf4jLogger"]
              |  logger-startup-timeout = 60 seconds
              |  
              |  log-dead-letters-during-shutdown = false
              |  
              |  loglevel = DEBUG
              |  
              |  actor {
              |    provider = "akka.cluster.ClusterActorRefProvider"
              |  }
              |  remote {
              |    log-remote-lifecycle-events = off
              |    netty.tcp {
              |      hostname = "${hostname}"
              |      port = ${port}
              |    }
              |  }
              |  cluster {
              |    seed-nodes = [ ${seedNodeText} ]
              |    auto-down-unreachable-after = 10s
              |    min-nr-of-members = ${count}
              |  }
              |}""".stripMargin
            .stripMargin

        ActorSystem(name,
                    ConfigFactory.parseString(configFragment)
                      .withFallback(DefaultConfigForTests))
        }
  }
}

object ConfigurableActorSystems extends ConfigurableActorSystems
