package util

import java.net._

/**
  *
  * @param bindAddress
  */
@throws(classOf[SocketException])
class UDPSocket(val bindAddress: SocketAddress) extends DatagramSocket(bindAddress) {

  @throws(classOf[SocketException])
  def this() {
    this(new InetSocketAddress(0))
  }

  @throws(classOf[SocketException])
  def this(port: Int) {
    this(new InetSocketAddress(port))
  }

  @throws(classOf[SocketException])
  def this(port: Int, laddr: InetAddress) {
    this(new InetSocketAddress(laddr, port))
  }

}
