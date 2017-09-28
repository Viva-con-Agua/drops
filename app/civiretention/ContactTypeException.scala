package civiretention

/**
  * Created by johann on 19.09.17.
  */
case class ContactTypeException(private val message: String = "", private val cause: Throwable = None.orNull) extends Exception(message, cause)