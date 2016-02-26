package tracer

import java.net.{InetAddress, DatagramPacket, InetSocketAddress}

import util.{util, Directive, UDPSocket}

import scala.collection.mutable.Map

/**
  * 中心服务
  */
class Tracer {

  /** UAC信息跟踪列表 */
  private var _trace_list: Map[String, UacInfo] = Map()

  private var _server: UDPSocket = null

  private val _port: Int = 4321

  private val _split_tag: String = "::"

  private val _body_split_tag: String = "--"

  /**
    * 探测是否能P2P连接
    * 目前无法区分Symmetric NAT|Cone NAT，默认为Cone NAT
    *
    * @param A 主动请求连接方
    * @param B 被请求连接方
    * @return Boolean
    */
  def detect(A: String, B: String): Boolean = {
    if (this._server == null) return false

    if (!this._trace_list.contains(A) || !this._trace_list.contains(B)
      || this._trace_list(A).status || this._trace_list(B).status
    ) {
      return false
    }

    return true
  }

  /**
    * 记录
    * 更新跟踪列表UAC信息
    *
    * @param name     客户端唯一标识
    * @param pul
    * @param loc
    * @param register =true 注册或者注销 true|false
    * @param update   =true 信息更新是否 true|false
    * @return Boolean
    */
  def record(name: String, pul: InetSocketAddress, loc: InetSocketAddress, register: Boolean = true, update: Boolean = true): Boolean = {
    try {
      if (register) {
        if (this._trace_list.contains(name)) {
          if (update) {
            this._trace_list(name).localAddress = loc
            this._trace_list(name).publicAddress = pul
          }
          this._trace_list(name).status = true
        } else {
          this._trace_list += (name -> new UacInfo(pul, loc)) //注册新用户
        }
      } else if (this._trace_list.contains(name)) {
        this._trace_list(name).status = false //注销
      }
    } catch {
      case ex: Exception => {
        util.log(0, ex.getMessage())
      }
        return false
    }

    return true
  }

  /**
    * 更新Uac活跃时间
    *
    * @param name
    */
  def updateUacTime(name: String): Unit = {
    if (this._trace_list.contains(name)) {
      this._trace_list(name).heartbeatTime = util.getDateTime()
    }
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
    val msgByte: Array[Byte] = Directive.directiveBuilder(dec, th, th_tag, body, this._split_tag).getBytes()
    return new DatagramPacket(msgByte, msgByte.length, address, port)
  }

  def msgPong(dst: DatagramPacket): DatagramPacket = {
    return msgBuilder(Directive.PONG, 1, "00", "", dst.getAddress(), dst.getPort())
  }

  def msgRegisterSuccessfully(dst: DatagramPacket, msg: String = ""): DatagramPacket = {
    return msgBuilder(Directive.CONNECT_SUCCESS, 1, "00", msg, dst.getAddress(), dst.getPort())
  }

  def msgClose(dst: DatagramPacket, msg: String = ""): DatagramPacket = {
    return msgBuilder(Directive.CLOSE_CONNECT, 1, "00", msg, dst.getAddress(), dst.getPort())
  }

  def msgCanPeer(address: InetAddress, port: Int, msg: String = ""): DatagramPacket = {
    return msgBuilder(Directive.CAN_CONNECT_PEAR, 1, "00", msg, address, port)
  }

  def msgCanNotPeer(address: InetAddress, port: Int, msg: String = ""): DatagramPacket = {
    return msgBuilder(Directive.CAN_NOT_CONNECT_PEAR, 1, "00", msg, address, port)
  }

  def msgCanNotPeer(dst: DatagramPacket, msg: String = ""): DatagramPacket = {
    return msgBuilder(Directive.CAN_NOT_CONNECT_PEAR, 1, "00", msg, dst.getAddress(), dst.getPort())
  }

  def msgHoling(address: InetAddress, port: Int, msg: String = ""): DatagramPacket = {
    return msgBuilder(Directive.HOLING, 1, "00", msg, address, port)
  }


  def msgHoleEnd(address: InetAddress, port: Int, msg: String = ""): DatagramPacket = {
    return msgBuilder(Directive.HOLE_END, 1, "00", msg, address, port)
  }

  private val _registerErr = Array[String](
    " the body of Directive.REGISTER's message is error！",
    " Directive.REGISTER fail "
  )

  /**
    * 服务启动
    */
  def start(): Unit = {

    val buf = new Array[Byte](1024);
    val recPacket = new DatagramPacket(buf, buf.length)

    this._server = new UDPSocket(this._port)
    val Server = this._server

    while (true) {
      Server.receive(recPacket)
      val receiveMessage = new String(recPacket.getData())
      val msgArr = receiveMessage.split(this._split_tag)

      util.toInt(msgArr(0)) match {

        case Directive.PING => {
          //name
          this.updateUacTime(msgArr(3))
          Server.send(this.msgPong(recPacket))
        }

        case Directive.REGISTER => {
          //name--host--port
          val bodyArr = msgArr(3).split(this._body_split_tag)
          if (bodyArr.length < 3) {
            util.log(Directive.REGISTER, msgArr(3) + this._registerErr(0))
            Server.send(this.msgClose(recPacket, this._registerErr(0)))
          }

          val UacName = bodyArr(0) + bodyArr(1) + bodyArr(2)

          if (this.record(UacName, new InetSocketAddress(recPacket.getAddress(), recPacket.getPort()),
            new InetSocketAddress(bodyArr(1), util.toInt(bodyArr(2))))) {
            Server.send(this.msgRegisterSuccessfully(recPacket, UacName))
          } else {
            util.log(Directive.REGISTER, this._registerErr(0))
            Server.send(this.msgClose(recPacket, UacName + this._registerErr(1)))
          }
        }

        case Directive.UNREGISTER => {
          //name
          if (this.record(msgArr(3), null, null, false)) {
            Server.send(this.msgClose(recPacket))
          }

        }

        case Directive.CONNECT_PEAR_REQUEST => {
          //nameA--nameB
          val bodyArr = msgArr(3).split(this._body_split_tag)
          if (!this.detect(bodyArr(0), bodyArr(1)) || bodyArr(0).equals(bodyArr(1))) {
            Server.send(this.msgCanNotPeer(recPacket, "A or B not exist or A==B"))
          }
          val UacB = this._trace_list(bodyArr(1))
          Server.send(this.msgHoling(UacB.publicAddress.getAddress(), UacB.publicAddress.getPort(), msgArr(3)))
        }

        case Directive.HOLE_END => {
          //nameA--nameB
          val bodyArr = msgArr(3).split(this._body_split_tag)
          val UacA = this._trace_list(bodyArr(0))
          Server.send(this.msgHoleEnd(UacA.publicAddress.getAddress(), UacA.publicAddress.getPort(), msgArr(3)))
        }


      }

    }


  }

}
