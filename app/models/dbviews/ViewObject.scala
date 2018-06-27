package models.dbviews

abstract class ViewObject(){
  def getValue (viewname: String): ViewBase
  def isFieldDefined(viewname: String): Boolean
}
