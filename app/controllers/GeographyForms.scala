package controllers

/**
  * Created by johann on 21.11.16.
  */

import play.api._
import play.api.data.Form
import play.api.data.Forms._
import models.Crew

object CrewForms {

  case class CrewData(crewName : String, active: Boolean)

  def geoForm = Form(mapping(
    "crewName" -> nonEmptyText,
    "active" -> boolean
  )(CrewData.apply)(CrewData.unapply))
}
