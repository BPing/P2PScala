package peer

import java.io.{InputStreamReader, BufferedReader}
import java.net.{InetAddress, InetSocketAddress, DatagramPacket}

import util.{util, Directive, UDPSocket, UacInfo}

import scala.collection.mutable.Map


object Peer {
  def main(args: Array[String]) {
    val instance = new Peer()
    instance.start()
  }
}

/**
  * @author cbping
  */
class Peer {

  /** UAC信息跟踪列表 */
  private var _trace_list: Map[String, UacInfo] = Map()

  private var _client: UDPSocket = null

  private val _port: Int = 1012

  private var _own_id: String = ""

  def receive(): Unit = {
    try {
      val buf = new Array[Byte](1024)
      val recPacket = new DatagramPacket(buf, buf.length)
      this._client = new UDPSocket(this._port)
      val Client = this._client
      util.log(0, "waiting for the packet from the other")

      while (true) {
        util.cleanArrByte(buf)
        Client.receive(recPacket)
        val receiveMessage = new String(recPacket.getData())
        util.log(0, "the msg: " + receiveMessage)

        val msgArr = receiveMessage.split(util._split_tag)
        val bodyArr = msgArr(3).split(util._body_split_tag) //name--other

        util.toInt(msgArr(0)) match {
          case Directive.CAN_NOT_CONNECT_PEAR => {
            //nameA--nameB--(error message)
            util.log(0, msgArr(2))
          }

          case Directive.CLOSE_CONNECT => {
            //(error message)
            util.log(0, msgArr(0))
          }

          case Directive.USER_LIST => {
            //list--nameA--nameB...
            if (!msgArr(3).equals("list")) {
              for (client <- bodyArr) {
                if (!client.equals("list") && !this._trace_list.contains(client)) {
                  this._trace_list += (client -> new UacInfo(null, null))
                }
              }
            }
          }

          case Directive.CONNECT_SUCCESS => {
            //UacName
            this._own_id = msgArr(3).trim()
            util.log(0, "the ownId:" + this._own_id)
          }

          case Directive.HOLING => {
            //nameA--nameB--(public)(ip/port(A)||ip/port(B))--(private)(ip/port(A)||ip/port(B))
            var saveUac: String = ""
            if (this._own_id.equals(bodyArr(0))) {
              saveUac = bodyArr(1)
            } else {
              saveUac = bodyArr(0)
            }

            if (this._trace_list.contains(saveUac)) {
              this._trace_list(saveUac).publicAddress = util.stringToAddress(bodyArr(2))
              this._trace_list(saveUac).localAddress = util.stringToAddress(bodyArr(3))
            } else {
              this._trace_list += (saveUac -> new UacInfo(util.stringToAddress(bodyArr(2)), util.stringToAddress(bodyArr(3))))
            }
            //打洞
            Client.send(Directive.msgHoling(util.stringToAddress(bodyArr(2)).getAddress(), util.stringToAddress(bodyArr(2)).getPort(), "holing"))
          }
          case Directive.HOLE_END => {
            //nameA--nameB
            if (this._own_id.equals(bodyArr(0)) && this._trace_list.contains(bodyArr(1))) {
              this._trace_list(bodyArr(1)).peerStatus = true
            }
          }
          case _ => {
            util.log(-1, receiveMessage)
          }
        }
      }
    } catch {
      case ex: Exception => {
        util.log(-1, "some exception happen ,please restart the peer")
      }
    }
  }

  def sent(): Unit = {

    val sin: BufferedReader = new BufferedReader(new InputStreamReader(System.in))

    var line = sin.readLine()

    val severAddr = new InetSocketAddress(util._server_hostname, util._server_port) //服务端地址信息

    while (!line.equals("exit")) {
      val lineArr = line.split("#")
      try {

        lineArr(0).toLowerCase match {
          case "user" => {
            this._client.send(Directive.msgUserList1(severAddr.getAddress(), severAddr.getPort(), this._own_id))
          }
          case "peer" => {
            if (lineArr.length == 2) {
              this._client.send(Directive.msgPeerRes(severAddr.getAddress(), severAddr.getPort(), this._own_id + util._body_split_tag + lineArr(1)))
            } else {
              println("the number of args is wrong,eg:peer#nameB")
            }
          }

          case "sent" => {
            if (lineArr.length == 2) {

              this._client.send(Directive.msgSent(severAddr.getAddress(), severAddr.getPort(), this._own_id + util._body_split_tag + lineArr(1)))

            } else if (lineArr.length == 3) {

              if (this._trace_list.contains(lineArr(1))) {

                this._client.send(Directive.msgSent(this._trace_list(lineArr(1)).publicAddress.getAddress(), this._trace_list(lineArr(1)).publicAddress.getPort(), lineArr(2)))

              } else {

                println("the peer: " + lineArr(1) + " is not exist! please sent p2p request first")

              }
            } else {

              println("the number of args is wrong,eg:sent#nameB#msg(to peer) or sent#msg(to server)")

            }
          }
          case "register" => {
            if (lineArr.length == 2) {
              this._client.send(Directive.msgRegister(severAddr.getAddress(), severAddr.getPort(),
                lineArr(1) + util._body_split_tag + InetAddress.getLocalHost().getHostAddress() + util._body_split_tag + this._port))
            } else {
              println("the number of args is wrong,eg:register#ownNameB")
            }
          }

          case "help" => {
            println("1、user eg: user#msg\n" +
              "2、peer eg: peer#peerName \n" +
              "3、sent eg：sent#peerName#msg or sent#msg (default server) \n" +
              "4、register eg register#name \n")
          }

          case _ => {
            println("the directive is wrong! please input 'help' for help!")
          }

        }

      } catch {
        case ex: Exception => {
          util.log(-1, "exception::" + ex.getMessage())
        }
      }

      line = sin.readLine()
    }
    sin.close()
  }

  /**
    * 客户端启动
    */
  def start(): Unit = {
    util.log(0, "start client")

    val handler = this
    util.log(0, "start receive thread ")
    new Thread(new Runnable {
      override def run(): Unit = {
        handler.receive()
      }
    }).start()
    util.log(0, "start sent thread ")
    new Thread(new Runnable {
      override def run(): Unit = {
        handler.sent()
      }
    }).start()

  }
}
