/** Copyright(c) 2013 by TimeTrade Systems.  All Rights Reserved. */
package com.timetrade.queueservice.jsonutils

import spray.json.DeserializationException
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsonFormat
import spray.json.pimpAny
import spray.json.pimpString

/** Provides succinct JSON conversion for case classes which are "typed wrapped strings",
  * i.e. just a single String field called "s", wrapped into a case class to improve
  * type safety.
  * E.g. of use:
  * case class Surname(s: String) extends TypedString
  * object Surname extends TypedStringJsonProtocol {
  *   implicit val _ = jsonFormat(apply)
  * }
  *
  * The JSON generated will be a simple JSON string and not an object.
  * Similarly a JSON string can be parsed to
  */
trait TypedStringJsonProtocol {

  def jsonFormat[T <: TypedString](maker: String => T): JsonFormat[T] =
    new JsonFormat[T] {
      override def read(json: JsValue): T = json match {
        case JsString(s) => maker(s)
        case _ =>  throw new DeserializationException("String expected")
      }

    override def write(ws: T): JsValue = JsString(ws.s)
  }
}
