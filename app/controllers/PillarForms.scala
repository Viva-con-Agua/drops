package controllers

/**
  * Created by johann on 21.11.16.
  */

import play.api._
import play.api.data.Form
import play.api.data.Forms._
import models.Crew

object PillarForms {

  case class PillarData(education: Boolean, operation: Boolean, finance: Boolean, network: Boolean) {
    def toMap : Map[String, Boolean] = Map(
      "education" -> education,
      "operation" -> operation,
      "finance" -> finance,
      "network" -> network
    )
  }

  def define = Form(mapping(
    "education" -> boolean,
    "operation" -> boolean,
    "finance" -> boolean,
    "network" -> boolean
  )(PillarData.apply)(PillarData.unapply))
}
