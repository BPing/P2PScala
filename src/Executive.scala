/**
  * Created by dreamer on 2015/12/21.
  */
package professional {

class Executive {
  private[professional] var workDetails = null
  private[Executive] var secrets = null

  def help(another: Executive) {
    println(another.workDetails)
    println(another.secrets)
  }
}

}