import java.util.{Date, Locale}
import java.text.DateFormat
import java.text.DateFormat._

object ScalaApp {
  def main(args: Array[String]): Unit = {

    val now = new Date
    val df = DateFormat.getDateTimeInstance()
    val datetime=df format now
    println(datetime)


  }
}



