/** Copyright(c) 2013-2016 by TimeTrade Systems.  All Rights Reserved. */
package com.timetrade.queueservice.server.metrics

import nl.grons.metrics.scala.InstrumentedBuilder

import com.codahale.metrics.Meter
import com.codahale.metrics.MetricRegistry

/** If one wants to count an operation, extend this trait, define and use the desired counters as
  *  for example:
  *      val successes = CodahaleMeter("operation", "successes")
  *      val failures = CodahaleMeter("operation", "failures")
  *
  *      successes.inc()
  *      failures.count {
  *          ...some function...
  *      }
  */
trait CodahaleMetrics extends InstrumentedBuilder {
  override val metricRegistry = CodahaleMetrics.registry

  /** Constructs a new meter for the class which extends this trait.  Enforces a convention that
   *  the name cannot have periods ("."; this function translates them to dashes), so that we may
   *  infer the name from the metric's JSON form, which is formed by CodaHale in a classpath-like
   *  way.
   *
   *  E.g., if this trait is extended by class com.timetrade.queueservice.FooBar, and the name of
   *  a meter is "freak.outs" and the label prefix is "galore", then the JSON name of the meter
   *  will be "com.timetrade.queueservice.FooBar.freak-outs.galore".
   */
  def CodahaleMeter(labelPrefix: String, name: String) =
    metrics.meter(sanitizedName(name), sanitizedLabel(labelPrefix))

  /** @return name, with periods (and any other problematic characters) translated to dashes
   */
  private def sanitizedName(name: String) = {
    name.map { c => if (c == '.') '-' else c }
  }

  /** @return label, with periods (and any other problematic characters) translated to dashes */
  private def sanitizedLabel(label: String) = {
    if (label == null) label else label.map { c => if (c == '.') '-' else c }
  }
}

/** Contains code and data to support metric collection. */
object CodahaleMetrics {

  /** Application-wide registry for metrics. */
  lazy val registry = new MetricRegistry()
}
