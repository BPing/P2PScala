package util

import java.text.{SimpleDateFormat, DateFormat}
import java.util.Date

/**
  *
  */
object util {
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
  def isBiggerThreshold(ds: String, de: String, threshold: Int): Boolean = {
    val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val dateStart = df.parse(ds)
    val dateEnd = df.parse(ds)
    return ((dateEnd.getTime - dateStart.getTime()) / 1000).toInt < threshold
  }

}
