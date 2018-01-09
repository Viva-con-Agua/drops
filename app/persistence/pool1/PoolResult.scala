package persistence.pool1

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, OWrites, Reads}

class PoolResult(val code: Int, val message: String, val context: String) {
  override def toString : String = {
    "Code: " + code + "; Message: " + message + "; Context: " + context
  }
}

case class PoolFailure(override val code: Int, override val message: String, override val context: String) extends PoolResult(code, message, context)
case class PoolSuccess(override val code: Int, override val message: String, override val context: String) extends PoolResult(code, message, context)

object PoolResult {

  /**
    * Creates a new Pool Result instance from a given tuple.
    *
    * Todo: Check for response codes! 200 the only valid code for a success response?
    * @param params
    * @return
    */
  def apply(params : (Int, String, String)) : PoolResult = params match { case (code, message, context) =>
      code match {
        case 200 => PoolSuccess(code, message, context)
        case _ => PoolFailure(code, message, context)
      }
  }

  implicit val poolResultWrites : OWrites[PoolResult] = (
    (JsPath \ "code").write[Int] and
      (JsPath \ "message").write[String] and
      (JsPath \ "context").write[String]
    )((result) => (result.code, result.message, result.context))

  implicit val poolResultReads : Reads[PoolResult] = (
    (JsPath \ "code").read[Int] and
      (JsPath \ "message").read[String] and
      (JsPath \ "context").read[String]
    ).tupled.map(PoolResult( _ ))
}