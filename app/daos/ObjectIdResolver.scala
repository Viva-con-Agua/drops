package daos

import java.util.UUID

import models.ObjectIdWrapper

import scala.concurrent.Future

/**
  * Created by johann on 29.12.16.
  */
trait ObjectIdResolver {
  def getObjectId(id: UUID):Future[Option[ObjectIdWrapper]]
  def getObjectId(name: String):Future[Option[ObjectIdWrapper]]
}
