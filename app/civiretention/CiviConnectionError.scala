package civiretention

/**
  * Created by johann on 18.09.17.
  */
case class CiviConnectionError(msg: String) extends Exception {
  override def getMessage: String = msg
}
