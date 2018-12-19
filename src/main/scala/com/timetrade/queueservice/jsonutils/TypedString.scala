/** Copyright(c) 2013 by TimeTrade Systems.  All Rights Reserved. */
package com.timetrade.queueservice.jsonutils

/** See comments for TypedStringJsonProtocol. */
trait TypedString {
  def s: String

  override def toString = s
}
