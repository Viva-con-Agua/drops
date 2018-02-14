package models

import javax.inject.Inject
import services.{TemplateHandler}
case class TemplateHtml (
  template: String
) 
/*extends TemplateHandler{
  def getHtml(templateName: String): String = {
    //templateHandler.responseHandler(templateName)
    templateName
  }
}*/
object TemplateHtml extends TemplateHandler {
 //def apply(template: String): TemplateHtml =
 //   TemplateHtml(template)

 def getHtml(templateName: String): String = {
   templateName
 }
}
