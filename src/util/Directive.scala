package util

import java.net.{DatagramPacket, InetAddress}

/**
  * 指令
  */
object Directive {
  //注册信息
  val REGISTER: Int = 10

  val UNREGISTER: Int = 11

  //请求连接其他对等客户端
  val CONNECT_PEAR_REQUEST: Int = 12

  //可以连接
  val CAN_CONNECT_PEAR: Int = 13

  val CAN_NOT_CONNECT_PEAR: Int = 14

  //ping
  val PING: Int = 15

  //pong
  val PONG: Int = 16

  //打洞
  val HOLING: Int = 17

  val HOLE_END: Int = 18

  //用户列表
  val USER_LIST: Int = 19

  //
  val CONNECT_SUCCESS: Int = 20

  val CLOSE_CONNECT: Int = 21

  val SENT_MSG: Int = 22


  /**
    * 构建信息字符串
    *
    * @param dec       指令
    * @param th        主|次(1|0) 线程
    * @param th_tag    00|other
    * @param body
    * @param split_tag 分隔符
    * @return
    */
  def directiveBuilder(dec: Int, th: Int, th_tag: String, body: String, split_tag: String): String = {
    val str = dec.toString() + split_tag + th.toString() + split_tag + th_tag + split_tag + body
    util.log(0, str)
    return str
  }


  /**
    * 构建udp包
    *
    * @param dec
    * @param th
    * @param th_tag
    * @param body
    * @param address
    * @param port
    * @return
    */
  def msgBuilder(dec: Int, th: Int, th_tag: String, body: String, address: InetAddress = null, port: Int = 0): DatagramPacket = {
    val msgByte: Array[Byte] = this.directiveBuilder(dec, th, th_tag, body, util._split_tag).getBytes()
    return new DatagramPacket(msgByte, msgByte.length, address, port)
  }

  def msgPong(dst: DatagramPacket): DatagramPacket = {
    return this.msgBuilder(Directive.PONG, 1, "00", "", dst.getAddress(), dst.getPort())
  }

  def msgRegisterSuccessfully(dst: DatagramPacket, msg: String = ""): DatagramPacket = {
    return this.msgBuilder(Directive.CONNECT_SUCCESS, 1, "00", msg, dst.getAddress(), dst.getPort())
  }

  def msgClose(dst: DatagramPacket, msg: String = ""): DatagramPacket = {
    return this.msgBuilder(Directive.CLOSE_CONNECT, 1, "00", msg, dst.getAddress(), dst.getPort())
  }

  def msgCanPeer(address: InetAddress, port: Int, msg: String = ""): DatagramPacket = {
    return this.msgBuilder(Directive.CAN_CONNECT_PEAR, 1, "00", msg, address, port)
  }

  def msgPeerRes(address: InetAddress, port: Int, msg: String = ""): DatagramPacket = {
    return this.msgBuilder(Directive.CONNECT_PEAR_REQUEST, 1, "00", msg, address, port)
  }

  //  def msgCanNotPeer(address: InetAddress, port: Int, msg: String = ""): DatagramPacket = {
  //    return this.msgBuilder(Directive.CAN_NOT_CONNECT_PEAR, 1, "00", msg, address, port)
  //  }

  def msgCanNotPeer(dst: DatagramPacket, msg: String = ""): DatagramPacket = {
    return this.msgBuilder(Directive.CAN_NOT_CONNECT_PEAR, 1, "00", msg, dst.getAddress(), dst.getPort())
  }

  def msgHoling(address: InetAddress, port: Int, msg: String = ""): DatagramPacket = {
    return this.msgBuilder(Directive.HOLING, 1, "00", msg, address, port)
  }


  def msgHoleEnd(address: InetAddress, port: Int, msg: String = ""): DatagramPacket = {
    return this.msgBuilder(Directive.HOLE_END, 1, "00", msg, address, port)
  }

  def msgRegister(address: InetAddress, port: Int, msg: String = ""): DatagramPacket = {
    return this.msgBuilder(Directive.REGISTER, 1, "00", msg, address, port)
  }

  def msgUserList(dst: DatagramPacket, msg: String = ""): DatagramPacket = {
    return this.msgBuilder(Directive.USER_LIST, 1, "00", msg, dst.getAddress(), dst.getPort())
  }

  def msgUserList1(address: InetAddress, port: Int, msg: String = ""): DatagramPacket = {
    return this.msgBuilder(Directive.USER_LIST, 1, "00", msg, address, port)
  }

  /**
    * 普通信息发送
    *
    * @param address
    * @param port
    * @param msg
    * @return
    */
  def msgSent(address: InetAddress, port: Int, msg: String = ""): DatagramPacket = {
    return this.msgBuilder(Directive.SENT_MSG, 1, "00", msg, address, port)
  }
}
