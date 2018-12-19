package com.timetrade.queueservice.server.core.persistence

import scala.util.control.Exception.nonFatalCatch

import akka.event.LoggingAdapter

/** Exception logging within the persistence package. */
trait ExceptionLogging {

  private[persistence] val log: LoggingAdapter

  /** Run a code block and log exceptions at ERROR level.
    *
    * @tparam T the return type of the code block to be run
    * @param block the code block to be run
    * @return the value returned by the code block if it does not throw
    */
  def withExceptionsLogged[T](block: => T): T = {
    val catchInstance = nonFatalCatch withApply { t =>
      log.error(t, "Exception accessing persisted data")
      throw t
    }
    catchInstance(block)
  }
}
