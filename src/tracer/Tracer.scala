package tracer

import java.net.{InetAddress, DatagramPacket, InetSocketAddress}
import scala.collection.mutable.Map
import util.{UacInfo, util, Directive, UDPSocket}

object Tracer {
  def main(args: Array[String]) {
    val instance = new Tracer()
    util.log(0, "start tracer")
    instance.start()
  }
}

/**
  * 中心服务
  *
  * @author cbping
  */
class Tracer {

  /** UAC信息跟踪列表 */
  private var _trace_list: Map[String, UacInfo] = Map()

  private var _server: UDPSocket = null

  private val _port: Int = util._server_port

  /** 心跳失效时间 */
  private val _expire_heart_beat: Long = 60

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
      || !this._trace_list(A).status || !this._trace_list(B).status
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

  private val _registerErr = Array[String](
    " the body of Directive.REGISTER's message is error！",
    " Directive.REGISTER fail "
  )

  /**
    *
    */
  def checkActive(): Unit = {
    if (!this._trace_list.isEmpty) {
      this._trace_list.foreach(e => {
        val (k, v) = e
        if (v.status && util.isBiggerThreshold(v.heartbeatTime, util.getDateTime(), this._expire_heart_beat)) {
          v.status = false
        }
      })
    }

  }

  /**
    * 心跳处理
    */
  def heartBeatHandler(): Unit = {
    val handle = this
    new Thread(new Runnable {
      override def run(): Unit = {
        while (handle != null) {
          handle.checkActive()
          Thread.sleep(1000) //一秒检查一次
        }
      }
    }).start()
  }

  /**
    * 服务启动
    */
  def start(): Unit = {
    var Server: UDPSocket = null
    try {
      val buf = new Array[Byte](1024)
      val recPacket = new DatagramPacket(buf, buf.length)

      this._server = new UDPSocket(this._port)
      Server = this._server

      // this.heartBeatHandler()

      while (true) {
        util.cleanArrByte(buf)
        util.log(0, "waiting for the packet from the client")
        Server.receive(recPacket)
        val receiveMessage = new String(recPacket.getData())
        util.log(0, "the client msg: " + receiveMessage + "\n")

        try {

          val msgArr = receiveMessage.split(util._split_tag)
          val bodyArr = msgArr(3).split(util._body_split_tag) //name--other
          util.trimArr(msgArr)
          util.trimArr(bodyArr)
          //判断注册状态
          if ((!this._trace_list.contains(bodyArr(0)) || !this._trace_list(bodyArr(0)).status) && util.toInt(msgArr(0)) != Directive.REGISTER) {
            Server.send(Directive.msgClose(recPacket, "register first!"))
          } else {
            util.toInt(msgArr(0)) match {

              case Directive.PING => {
                //name
                this.updateUacTime(msgArr(3))
                Server.send(Directive.msgPong(recPacket))
              }

              case Directive.REGISTER => {
                //name--host--port
                if (bodyArr.length < 3) {
                  util.log(Directive.REGISTER, msgArr(3) + this._registerErr(0))
                  Server.send(Directive.msgClose(recPacket, this._registerErr(0)))
                } else {

                  val UacName = bodyArr(0)

                  if (this.record(UacName, new InetSocketAddress(recPacket.getAddress(), recPacket.getPort()),
                    new InetSocketAddress(bodyArr(1), util.toInt(bodyArr(2))))) {
                    Server.send(Directive.msgRegisterSuccessfully(recPacket, UacName))
                  } else {
                    util.log(Directive.REGISTER, this._registerErr(0))
                    Server.send(Directive.msgClose(recPacket, UacName + this._registerErr(1)))
                  }
                }
              }

              case Directive.UNREGISTER => {
                //name
                if (this.record(msgArr(3), null, null, false)) {
                  Server.send(Directive.msgClose(recPacket))
                }

              }

              case Directive.CONNECT_PEAR_REQUEST => {
                //nameA--nameB
                if (bodyArr.length < 2 || !this.detect(bodyArr(0), bodyArr(1)) || bodyArr(0).equals(bodyArr(1))) {
                  Server.send(Directive.msgCanNotPeer(recPacket, msgArr(0) + util._body_split_tag + "A or B not exist or A==B"))
                } else {

                  val UacA = this._trace_list(bodyArr(0))
                  val UacB = this._trace_list(bodyArr(1))
                  val msgStrB: String = msgArr(3) + util._body_split_tag + util.addressToString(UacA.publicAddress) + util._body_split_tag + util.addressToString(UacA.localAddress)
                  Server.send(Directive.msgHoling(UacB.publicAddress.getAddress(), UacB.publicAddress.getPort(), msgStrB))

                  val msgStrA: String = msgArr(3) + util._body_split_tag + util.addressToString(UacB.publicAddress) + util._body_split_tag + util.addressToString(UacB.localAddress)
                  Server.send(Directive.msgHoling(UacA.publicAddress.getAddress(), UacA.publicAddress.getPort(), msgStrA))
                }
              }

              case Directive.HOLE_END => {
                //nameA--nameB
                val UacA = this._trace_list(bodyArr(0))
                Server.send(Directive.msgHoleEnd(UacA.publicAddress.getAddress(), UacA.publicAddress.getPort(), msgArr(3)))

                val Uac = this._trace_list(bodyArr(1))
                Server.send(Directive.msgHoleEnd(UacA.publicAddress.getAddress(), UacA.publicAddress.getPort(), msgArr(3)))
              }

              case Directive.USER_LIST => {
                var msgStr: String = "list"
                if (!this._trace_list.isEmpty) {
                  this._trace_list.foreach(e => {
                    val (k, v) = e
                    msgStr = msgStr + util._body_split_tag + k
                  })
                }
                Server.send(Directive.msgUserList(recPacket, msgStr))
              }
            }
          }

        } catch {
          case ex: Exception => {
            Server.send(Directive.msgSent(recPacket.getAddress(), recPacket.getPort(), "the msg is wrong ,check first"))
            util.log(-1, "some exception happen:" + ex.getMessage())
            ex.printStackTrace()
          }
        }
      }

      Server.close()
    } catch {
      case ex: Exception => {
        util.log(-1, "some exception happen,please restart the tracer" + ex.getMessage())
      }
        if (null != Server)
          Server.close()
    }

  }

}
