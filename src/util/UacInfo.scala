package util

import java.net.InetSocketAddress

/**
  * Uac信息
  *
  * @param pul
  * @param loc
  */
class UacInfo(pul: InetSocketAddress, loc: InetSocketAddress) {

  //Nat公网ip：port
  var publicAddress: InetSocketAddress = pul

  //私有ip：port
  var localAddress: InetSocketAddress = loc

  /**
    * 活动状态
    */
  var status: Boolean = true

  //是否可以点对点连接
  var peerStatus: Boolean = false

  var heartbeatTime: String = util.getDateTime()

}
