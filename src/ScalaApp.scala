
import tracer.Tracer
import util.util

object ScalaApp {
  def main(args: Array[String]): Unit = {
    val instance = new Tracer()
    util.log(0, "start tracer")
    instance.start()
  }

}



