package util

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

    return dec.toString() + split_tag + th.toString() + split_tag + th_tag + split_tag + body
  }
}
