package daos

import java.util.UUID

import scala.concurrent.Future

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection

import com.mohiva.play.silhouette.api.LoginInfo

import models.{Crew}
import models.Crew._

trait CrewDao {
  def find(crewName: String):Future[Option[Crew]]
  def save(crew: Crew):Future[Crew]
  def list : Future[List[Crew]]

  trait CrewWS {
    def list(queryExtension: JsObject, limit : Int, sort: JsObject):Future[List[Crew]]
  }
  val ws : CrewWS
}

class MongoCrewDao extends CrewDao {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  val crews = reactiveMongoApi.db.collection[JSONCollection]("crews")

  def find(crewName: String):Future[Option[Crew]] =
    crews.find(Json.obj(
      "name" -> crewName
    )).one[Crew]

  def save(crew: Crew):Future[Crew] =
    crews.insert(crew).map(_ => crew)

  def list = ws.list(Json.obj(), 20, Json.obj())//crews.find(Json.obj()).cursor[Crew]().collect[List]()

  class MongoCrewWS extends CrewWS {
    override def list(queryExtension: JsObject, limit: Int, sort: JsObject): Future[List[Crew]] =
      crews.find(queryExtension).sort(sort).cursor[Crew]().collect[List](limit)
  }

  val ws = new MongoCrewWS
}
