package com.timetrade.queueservice.server.core.persistence

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.blocking

/** Facilitates using Scala managed blocking. */
trait BlockingFutures {

  /** This allows us to designate a piece of code as potentially blocking which
    * may allow the runtime system to improve performance or avoid deadlocks.
    * See http://lampwww.epfl.ch/~phaller/doc/Combining_Concurrency.pdf
    *
    * This is also designed to force us to be explicit about which dispatcher a given blocking
    * future runs on, to facilitate quarantining of such blocking to certain dispatchers.
    *
    * @tparam T the type returned by the code block this is applied to
    * @param ec the ExecutionContext on which the Future is run
    * @param body the code block to run with the Future
    * @return the Future created th run the code block
    */
  def blockingFutureOn[T](ec: ExecutionContext)(body: => T): Future[T] =
    Future{ blocking[T](body) }(ec)
}

/** Companion object to allow import rather than mixin. */
object BlockingFutures extends BlockingFutures
