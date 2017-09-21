package services

import javax.inject.Inject

import daos.{AccessRightDao, TaskDao}
import models.{AccessRight, Task}
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class TaskService @Inject()(
  val taskDao : TaskDao,
  val accessRightDao : AccessRightDao) {

  def getWithAccessRights(id: Long): Future[JsObject] = {
    taskDao.find(id).flatMap(t =>
      accessRightDao.forTask(id).map(aR =>
        Json.toJson(t).asInstanceOf[JsObject] + ("accessRights" -> Json.toJson(aR))))
  }
}