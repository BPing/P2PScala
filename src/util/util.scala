package util

import java.net.InetSocketAddress
import java.text.{SimpleDateFormat, DateFormat}
import java.util.Date

/**
  * 公共函数和公共配置
  */
object util {

  val _split_tag: String = "::"

  val _body_split_tag: String = "--"

  val _ping_interval: Long = 10 //ms

  val _server_hostname: String = "127.0.0.1"

  val _server_port: Int = 4321

  /**
    *
    * @param s
    * @return
    */
  def toInt(s: String): Int = {
    try {
      s.toInt
    } catch {
      case e: Exception => 0
    }
  }

  /**
    *
    * @param code
    * @param msg
    */
  def log(code: Int, msg: String): Unit = {
    println(msg)
  }

  /**
    * 获取时间
    *
    * @return 如：2016-2-26 15:41:15
    */
  def getDateTime(): String = {
    val now = new Date
    val df = DateFormat.getDateTimeInstance()
    return df format now
  }

  /**
    * 时间段是否超过给定的阀值
    *
    * @param ds        如：2016-2-26 15:41:15
    * @param de        如：2016-2-26 15:41:15
    * @param threshold 阀值 (s)
    * @return
    */
  def isBiggerThreshold(ds: String, de: String, threshold: Long): Boolean = {
    val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val dateStart = df.parse(ds)
    val dateEnd = df.parse(ds)
    return ((dateEnd.getTime - dateStart.getTime()) / 1000) < threshold
  }


  /**
    * 转化成字符串 如：127.0.0.1:4321 ip:port
    *
    * @param inetSocketAddress
    * @return
    */
  def addressToString(inetSocketAddress: InetSocketAddress): String = {
    return inetSocketAddress.getHostName() + "/" + inetSocketAddress.getPort()
  }

  /**
    *
    * @param str
    * @return
    */
  def stringToAddress(str: String): InetSocketAddress = {
    val arr = str.split("/")
    if (arr.length != 2) {
      return new InetSocketAddress("0.0.0.0", 0)
    }
    return new InetSocketAddress(arr(0), util.toInt(arr(1)))
  }


}
