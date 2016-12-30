package daos

import models.ObjectIdWrapper

import scala.concurrent.Future

/**
  * Created by johann on 29.12.16.
  */
trait ObjectIdResolver {
  def getObjectId(id: String):Future[Option[ObjectIdWrapper]]
}
