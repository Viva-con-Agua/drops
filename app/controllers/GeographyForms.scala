package controllers

/**
  * Created by johann on 21.11.16.
  */

import play.api._
import play.api.data.Form
import play.api.data.Forms._
import models.Crew

object CrewForms {

  def geoForm = Form(mapping(
    "country" -> nonEmptyText,
    "city" -> nonEmptyText,
    "active" -> boolean
  )(Crew.apply)(Crew.unapply))
}
