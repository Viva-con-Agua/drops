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
import daos.schema.{CityTableDef, CrewTableDef}
import models.{Crew, CrewStub, ObjectId, ObjectIdWrapper}
import models.Crew._
import models.converter.CrewConverter
import models.database.{CityDB, CrewDB}
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._

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

class MariadbCrewDao extends CrewDao {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val crews = TableQuery[CrewTableDef]
  val cities = TableQuery[CityTableDef]

  override def find(id: UUID): Future[Option[Crew]] = {
    val action = for {
      (crew, city) <- (crews.filter(_.publicId === id)
        join cities on (_.id === _.crewId)
        )} yield (crew, city)

    dbConfig.db.run(action.result).map(CrewConverter.buildCrewObjectFromResult(_))
  }


  override def find(crewName: String): Future[Option[Crew]] = {
    val action = for {
      (crew, city) <- (crews.filter(_.name === crewName)
        join cities on (_.id === _.crewId)
        )} yield (crew, city)

    dbConfig.db.run(action.result).map(r =>{
      CrewConverter.buildCrewObjectFromResult(r)
    })
  }

  def find(id : Long) : Future[Option[Crew]] = {
    val action = for {
      (crew, city) <- (crews.filter(_.id === id)
        join cities on (_.id === _.crewId)
        )} yield (crew, city)

    dbConfig.db.run(action.result).map(CrewConverter.buildCrewObjectFromResult(_))
  }

  def findDBCrewModel(crewId : UUID) : Future[CrewDB] = {
    dbConfig.db.run(crews.filter(_.publicId === crewId).result).map(r => r.head)
  }

  override def save(crew: Crew): Future[Crew] = {
    dbConfig.db.run((crews returning crews.map(_.id)) += CrewDB(crew))
    .flatMap(id =>{
        crew.cities.foreach(city => {
          dbConfig.db.run((cities returning cities.map(_.id)) += CityDB(0, city, id))
        })
      find(crew.id)
    }).map(c => c.get)
  }

  override def update(crew: Crew): Future[Crew] = {
    var crew_id = 0L

    dbConfig.db.run((crews.filter(r => r.publicId === crew.id || r.name === crew.name)).result)
        .flatMap(c => {
          crew_id = c.head.id
          dbConfig.db.run((crews.filter(_.id === c.head.id).update(CrewDB(c.head.id,crew.id, crew.name, crew.country))))
        })
        .flatMap(_ => dbConfig.db.run((for{c <- cities.filter(_.name.inSet(crew.cities))}yield c.crewId).update(crew_id)))
        .flatMap(_ => find(crew.id))
        .map(c => c.get)
  }

  override def listOfStubs: Future[List[CrewStub]] = {
    list.map(crewList => {
      CrewConverter.buildCrewStubListFromCrewList(crewList)
    })
  }

  override def list: Future[List[Crew]] = {
    val action = for {
      (crew, city) <- (crews
        join cities on (_.id === _.crewId)
        )} yield (crew, city)

    dbConfig.db.run(action.result).map(CrewConverter.buildCrewListFromResult(_))
  }

  override def getObjectId(id: UUID) : Future[Option[ObjectIdWrapper]] = {
    findDBCrewModel(id).map(c => {
      Option(ObjectIdWrapper(ObjectId(c.id.toString)))
    })
  }

  override def getObjectId(name: String) : Future[Option[ObjectIdWrapper]] = getObjectId(UUID.fromString(name))

  override def getCount : Future[Int] = dbConfig.db.run(crews.length.result)

  class MariadbCrewWS extends CrewWS {
    override def listOfStubs(queryExtension: JsObject, limit: Int, sort: JsObject): Future[List[CrewStub]] = ???

    override def list(queryExtension: JsObject, limit: Int, sort: JsObject): Future[List[Crew]] = MariadbCrewDao.this.list
  }

  val ws = new MariadbCrewWS

}
