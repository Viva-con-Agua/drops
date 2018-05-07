package models.views

abstract class ViewObject(){
  def getValue (viewname: String): ViewBase
  def isFieldDefined(viewname: String): Boolean
}
