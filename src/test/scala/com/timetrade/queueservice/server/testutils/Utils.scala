/** Copyright(c) 2013-2016 by TimeTrade Systems.  All Rights Reserved. */
package com.timetrade.queueservice.server.testutils

import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel

object Utils {

  private val LOCAL_HOST_IP = "127.0.0.1"

  def temporaryServerAddress(interface: String = LOCAL_HOST_IP): InetSocketAddress = {
    val serverSocket = ServerSocketChannel.open()
    try {
      serverSocket.socket.bind(new InetSocketAddress(interface, 0))
      val port = serverSocket.socket.getLocalPort
      new InetSocketAddress(interface, port)
    } finally serverSocket.close()
  }

  def temporaryServerAddresses(interface: String = LOCAL_HOST_IP, n: Int): IndexedSeq[InetSocketAddress] = {
    val pairs = (1 to n)
      .map { _ =>
        val serverSocket = ServerSocketChannel.open()
        serverSocket.socket.bind(new InetSocketAddress(interface, 0))
        (
          serverSocket,
          new InetSocketAddress(interface, serverSocket.socket.getLocalPort)
        )
    }
    pairs foreach { _._1.close() }
    pairs map { _._2 }
  }

  def temporaryServerPorts(interface: String = LOCAL_HOST_IP, n: Int): IndexedSeq[Int] =
    temporaryServerAddresses(interface, n) map { _.getPort }

  def temporaryServerHostnameAndPort(interface: String = LOCAL_HOST_IP): (String, Int) = {
    val socketAddress = temporaryServerAddress(interface)
    socketAddress.getHostName -> socketAddress.getPort
  }
}
