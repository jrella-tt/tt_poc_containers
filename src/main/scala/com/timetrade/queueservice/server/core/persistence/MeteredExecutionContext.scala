/** Copyright(c) 2014-2016 by TimeTrade Systems.  All Rights Reserved. */
package com.timetrade.queueservice.server.core.persistence

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext
import scala.concurrent.stm._
import scala.concurrent.stm.atomic

import akka.actor.ActorSystem

import com.timetrade.queueservice.server.metrics.CodahaleMetrics

import nl.grons.metrics.scala.Meter

/** Provides an execution context with a thread pool, which we meter for requests that will probably
  *  block because the pool is too small.
  *
  * @constructor
  * @param threads the underlying thread pool executor to use
  * @param blockagesMeter the meter to measure how often executions are delayed for want of a thread
  */
class MeteredExecutionContext private (threads: ThreadPoolExecutor, blockagesMeter: Meter) extends ExecutionContext {

  // Delegate to this, real execution context.
  private val ec = ExecutionContext.fromExecutorService(threads)

  override def execute(runnable: Runnable): Unit = ec.execute(runnable)

  override def reportFailure(t: Throwable): Unit = ec.reportFailure(t)

  override def prepare(): ExecutionContext = {
    // We're about to attempt to get a thread.  Does it look likely to block?
    // (This is not guaranteed to be accurate, since getActiveCount is only approximate.)
    if (threads.getActiveCount() >= threads.getMaximumPoolSize()) {
      blockagesMeter.mark
    }
    ec.prepare
  }

  /** Shutdown and delete (disassociate from ActorSystem) the underlying thread pool.
    */
  def shutdown()(implicit system: ActorSystem) = {
    threads.shutdownNow()
    MeteredExecutionContext.disassociate()
  }
}

/** Companion object provides factory method.
  */
object MeteredExecutionContext extends CodahaleMetrics {

  /** If unspecified, the maximum number of threads which this execution context may use. */
  private val DefaultMaxConcurrency = 25

  /** If unspecified, the maximum number of seconds before idle threads are reaped. */
  private val DefaultKeepAliveTime = 60L

  private val tmap = TMap[String, MeteredExecutionContext]()

  /** Get the instance for the current actor system, constructing it if necessary.
    * @param maxConcurrency    the maximum concurrency for the thread pool
    * @param keepAliveTime maximum number of seconds before idle threads are reaped.
    * @param blockagesMeterLabelPrefix name prefix for meter
    * @param system the current ActorSystem
    */
  def perActorSystem(maxConcurrency: Int = DefaultMaxConcurrency,
                     maxLifetimeSeconds: Long = DefaultKeepAliveTime,
                     blockagesMeterLabelPrefix: String = "Metered Thread Pool")
                     (implicit system: ActorSystem)
  : MeteredExecutionContext
  =  atomic { implicit txn =>

    if (!tmap.contains("system")) {
      // A pool of cached threads, limited in size, idle threads with limited lifetime.
      val threads: java.util.concurrent.ThreadPoolExecutor = new ThreadPoolExecutor(
        0,
        maxConcurrency,
        maxLifetimeSeconds, TimeUnit.SECONDS,
        new LinkedBlockingQueue[Runnable]());

      // A meter to record when requests to the thread pool might be blocked due to insufficient concurrency.
      val blockagesMeter = CodahaleMeter(blockagesMeterLabelPrefix, "thread pool saturations")

      tmap += ("system" -> new MeteredExecutionContext(threads, blockagesMeter))
    }
    tmap("system")
  }

  def disassociate()(implicit system: ActorSystem): Unit = atomic { implicit txn => tmap -= system.name }
}
