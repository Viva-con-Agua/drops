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
import models.{Crew, CrewStub, ObjectIdWrapper}
import models.Crew._

trait CrewDao extends ObjectIdResolver with CountResolver {
  def find(id: UUID):Future[Option[Crew]]
  def find(crewName: String):Future[Option[Crew]]
  def save(crew: Crew):Future[Crew]
  def update(crew: Crew):Future[Crew]
  def listOfStubs : Future[List[CrewStub]]
  def list : Future[List[Crew]]

  trait CrewWS {
    def listOfStubs(queryExtension: JsObject, limit : Int, sort: JsObject):Future[List[CrewStub]]
    def list(queryExtension: JsObject, limit : Int, sort: JsObject):Future[List[Crew]]
  }
  val ws : CrewWS
}

class MongoCrewDao extends CrewDao {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  val crews = reactiveMongoApi.db.collection[JSONCollection]("crews")
  val users = reactiveMongoApi.db.collection[JSONCollection]("users")

  override def getCount: Future[Int] =
    crews.count()

  override def getObjectId(id: UUID): Future[Option[ObjectIdWrapper]] =
    crews.find(Json.obj("id" -> id), Json.obj("_id" -> 1)).one[ObjectIdWrapper]

  override def getObjectId(name: String): Future[Option[ObjectIdWrapper]] =
    crews.find(Json.obj("id" -> name), Json.obj("_id" -> 1)).one[ObjectIdWrapper]

  def find(id: UUID):Future[Option[Crew]] =
    crews.find(Json.obj(
      "id" -> id
    )).one[Crew]

  def find(crewName: String):Future[Option[Crew]] =
    crews.find(Json.obj(
      "name" -> crewName
    )).one[Crew]

  def save(crew: Crew):Future[Crew] =
    crews.insert(crew).map(_ => crew)

  def update(crew: Crew):Future[Crew] =
    crews.update(Json.obj("$or" -> Json.arr(
      Json.obj("id" -> crew.id),
      Json.obj("name" -> crew.name)
    )), crew).map(_ => crew)

  def list = this.getCount.flatMap(c => ws.list(Json.obj(), c, Json.obj()))

  def listOfStubs = this.getCount.flatMap(c => ws.listOfStubs(Json.obj(), c, Json.obj()))

  class MongoCrewWS extends CrewWS {
    override def list(queryExtension: JsObject, limit: Int, sort: JsObject): Future[List[Crew]] =
      crews.find(queryExtension).sort(sort).cursor[Crew]().collect[List](limit)

    override def listOfStubs(queryExtension: JsObject, limit: Int, sort: JsObject): Future[List[CrewStub]] =
      crews.find(queryExtension).sort(sort).cursor[CrewStub]().collect[List](limit)
  }

  val ws = new MongoCrewWS
}
