package models.views

abstract  class ViewBase(){
  def getValue (fieldname: String): Any
  def isFieldDefined(fieldname: String): Boolean
}


