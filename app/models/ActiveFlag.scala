package models

import play.api.libs.json._
import play.api.libs.functional.syntax._




object ActiveFlag {
  implicit class StringImprovements(val s: String) {
    def check = s match {
      case "inactive" => "inactive"
      case "requested" => "requested"
      case "active" => "active"
      case _ => "ERROR: No Flag"
    }
  }
}


