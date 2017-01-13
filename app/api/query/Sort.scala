package api.query

import play.api.libs.json.Json

/**
  * Created by johann on 13.01.17.
  */
trait Sort {
  val dir : String
}

object Sort {
  def apply(dir: String) = dir match {
    case Asc.dir => Asc
    case Desc.dir => Desc
    case _ => throw new Exception // Todo: Use meaningful exception!
  }
  def unapply(arg: Sort): Option[String] = Some(arg.dir)

  implicit val sortJsonFormat = Json.format[Sort]
}

object Asc extends Sort {
  val dir = "asc"
}

object Desc extends Sort {
  val dir = "desc"
}