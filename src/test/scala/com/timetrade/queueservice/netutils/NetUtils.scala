/** Copyright(c) 2013-2014 by TimeTrade Systems.  All Rights Reserved. */
package com.timetrade.queueservice.netutils

import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.UnknownHostException
import scala.annotation.tailrec
import collection.JavaConversions.enumerationAsScalaIterator
import akka.http.scaladsl.model.Uri

/**
 * Networking utility functions.
 */
object NetUtils {

  def localHostIpAddress: String = {
    // Try to get the IP address from inspection of the defined network interfaces.
    getLocalIpAddressFromNetwork.getOrElse {
      try {
        // The following line may return "127.0.0.1" even if it succeeds.
        InetAddress.getLocalHost().getHostAddress()
      } catch {
        case e: UnknownHostException => "127.0.0.1"
      }
    }
  }

  /** @return the IP address, as a string, of the local host as defined in the network interfaces,
   *  or None if none can be divined from there
   */
  private def getLocalIpAddressFromNetwork: Option[String] = {
    val ipAddresses = NetworkInterface.getNetworkInterfaces.flatMap{ ni =>
      ni.getInetAddresses().filter{ na =>
        ! na.isLoopbackAddress() && na.isSiteLocalAddress() && ! (na.getHostAddress().indexOf(":") > -1)
      }
    }
    ipAddresses.toSeq.headOption.map{ na => na.getHostAddress() }
  }

  def localHostName: Option[String] = {
    try {
      Some(InetAddress.getLocalHost.getHostName)
    } catch {
      case _: Exception => None
    }
  }

  def findLocalFreePort(startingPort: Int, range: Int): Option[Int] = {
    if (range == 0) {
      None
    } else {
      if (!isServerListening("localhost", startingPort)) {
        Some(startingPort)
      } else {
        findLocalFreePort(startingPort+1, range-1)
      }
    }
  }

  def isServerListening(host: String, port: Int): Boolean = {
    @tailrec
    def canCreateSocket(retries: Int): Boolean = {
      val ok =
        try {
          new Socket(host, port).close()
          // Success: server is listening.
          true
        } catch {
          case exc: IOException => false
        }
      if (ok) {
        true
      }
      else if (retries == 0) {
        false
      }
      else {
        canCreateSocket(retries-1)
      }
    }

    canCreateSocket(3)
  }

 /** Decide what port is implied by a URL. */
  def impliedPort(uri: Uri) =
    if (uri.authority.port <= 0)
      if (uri.scheme == "https") 443 else 80
    else uri.authority.port
}
