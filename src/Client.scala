import java.io._
import java.net._

/**
  * Created by dreamer on 2016/2/25.
  */
object Client {

  def main(args: Array[String]): Unit = {
    try {

      val socket: Socket = new Socket("127.0.0.1", 4700)

      val sin: BufferedReader = new BufferedReader(new InputStreamReader(System.in))


      val is: BufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))

      val os: PrintWriter = new PrintWriter(socket.getOutputStream())

      var line: String = null

      println("Server:" + is.readLine())

      line = sin.readLine()

      while (!line.equals("bye")) {
        os.print(line+"\n")
        os.flush()

        println("Client:" + line)
        println("Server:" + is.readLine())

        line = sin.readLine()
      }

      os.close()
      is.close()
      socket.close()

    } catch {
      case ex: Exception => {
        println("Exception" + ex)
      }
    }
  }


}
