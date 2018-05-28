package models.dbviews

abstract class ViewBase(){
  def getValue (fieldname: String, index: Int): Any
  def isFieldDefined(fieldname: String, index: Int): Boolean
}


