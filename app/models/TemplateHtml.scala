package models

import javax.inject.Inject
import services.{TemplateHandler}

case class TemplateHtml @Inject()(
  templateHandler: TemplateHandler,
  template: String
  
){
  def getHtml(templateName: String): TemplateHtml = {
    TemplateHtml(templateHandler.getTemplate(templateName))
  }
}
object TemplateHtml {
  def apply(template: String): TemplateHtml =
    TemplateHtml(template)
}
