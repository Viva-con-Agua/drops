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
import play.Logger
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import slick.jdbc.{GetResult, PositionedParameters, SQLActionBuilder, SetParameter}

trait CrewDao extends ObjectIdResolver with CountResolver {
  def find(id: UUID):Future[Option[Crew]]
  def find(crewName: String):Future[Option[Crew]]
  def save(crew: Crew):Future[Crew]
  def update(crew: Crew):Future[Crew]
  def delete(crew: Crew):Future[Boolean]
  def listOfStubs : Future[List[CrewStub]]
  def list : Future[List[Crew]]
  def list_with_statement(statement : SQLActionBuilder):  Future[List[Crew]]
  def count_with_statement(statement : SQLActionBuilder) : Future[Long]


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

  def delete(crew: Crew):Future[Boolean] = ???
  def list = this.getCount.flatMap(c => ws.list(Json.obj(), c, Json.obj()))

  def listOfStubs = this.getCount.flatMap(c => ws.listOfStubs(Json.obj(), c, Json.obj()))

  class MongoCrewWS extends CrewWS {
    override def list(queryExtension: JsObject, limit: Int, sort: JsObject): Future[List[Crew]] =
      crews.find(queryExtension).sort(sort).cursor[Crew]().collect[List](limit)

    override def listOfStubs(queryExtension: JsObject, limit: Int, sort: JsObject): Future[List[CrewStub]] =
      crews.find(queryExtension).sort(sort).cursor[CrewStub]().collect[List](limit)
  }

  def list_with_statement(statement : SQLActionBuilder): Future[List[Crew]] = ???
  
  def count_with_statement(statement : SQLActionBuilder) : Future[Long] = ???
  val ws = new MongoCrewWS
}

class MariadbCrewDao extends CrewDao {


  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val crews = TableQuery[CrewTableDef]
  val cities = TableQuery[CityTableDef]

  implicit val getCrewResult = GetResult(r => CrewDB(r.nextLong, UUID.fromString(r.nextString), r.nextString))
  implicit val getCityResult = GetResult(r => CityDB(r.nextLong, r.nextString, r.nextString, r.nextLong))

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

  def findDBCrewModel(crewId : UUID) : Future[Option[CrewDB]] = {
    dbConfig.db.run(crews.filter(_.publicId === crewId).result).map(r => r.headOption)
  }

  override def save(crew: Crew): Future[Crew] = {
    dbConfig.db.run((crews returning crews.map(_.id)) += CrewDB(crew))
    .flatMap(id =>{
        crew.cities.foreach(city => {
          dbConfig.db.run((cities returning cities.map(_.id)) += CityDB(0, city.name, city.country, id))
        })
      find(crew.id)
    }).map(c => c.get)
  }

  override def update(crew: Crew): Future[Crew] = {
    var crew_id = 0L

    dbConfig.db.run((crews.filter(r => r.publicId === crew.id)).result)
        .flatMap(c => {
          crew_id = c.head.id
          dbConfig.db.run((crews.filter(_.id === c.head.id).update(CrewDB(c.head.id,crew.id, crew.name)))).flatMap(id => {
            // add new cities
            Future.sequence(crew.cities.map(city => {
              dbConfig.db.run((cities.filter(ci => ci.crewId === crew_id && ci.name === city.name && ci.country === city.country).exists.result.flatMap { exists =>
                if (!exists) {
                  cities returning cities.map(_.id) += CityDB(0, city.name, city.country, crew_id)
                } else {
                  DBIO.successful(None)
                }
              }))
            })).flatMap(_ => {
              // delete removed cities
              dbConfig.db.run(cities.filter(_.crewId === crew_id).result).map(
                // find all the cities currently saved for the crew inside the database
                _.map(r => CityDB( r.id, r.name, r.country, r.crewId ))
                  // filter the result for all cities saved inside the database, but not part of the cities of the current crew object
                  .filter(read => crew.cities.find(current => current.name == read.name && current.country == read.country).isEmpty)
                  .map(_.id)
              ).flatMap(toDelete => {
                Future.sequence(toDelete.map(id => dbConfig.db.run(cities.filter(_.id === id).delete)))
              })
            }).flatMap(_ => find(crew_id))
        })
        .map(c => c.get)
        })
  }

  override def delete(crew: Crew): Future[Boolean] = {
    dbConfig.db.run(crews.filter(c => c.publicId === crew.id).result)
      .flatMap(cr => {
        dbConfig.db.run(cities.filter(_.crewId === cr.head.id).delete)
        .flatMap {
          case x if crew.cities.size != x => Future.successful(false)
          case _ => dbConfig.db.run(crews.filter(c => c.publicId === crew.id).delete)
          .flatMap {
            case 0 => Future.successful(false)
            case _ => Future.successful(true)
          }
        }
      })
  
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

  def list_with_statement(statement : SQLActionBuilder) : Future[List[Crew]] = {
    var sql_action = statement.as[(CrewDB, CityDB)]
    dbConfig.db.run(sql_action).map(CrewConverter.buildCrewListFromResult(_))
  }
  def count_with_statement(statement : SQLActionBuilder) : Future[Long] = {
    val sql_action = statement.as[Long]
    dbConfig.db.run(sql_action).map(_.head)
  }


  override def getObjectId(id: UUID) : Future[Option[ObjectIdWrapper]] = {
    findDBCrewModel(id).map(_.map(c => ObjectIdWrapper(ObjectId(c.id.toString))))
  }

  override def getObjectId(name: String) : Future[Option[ObjectIdWrapper]] = getObjectId(UUID.fromString(name))

  override def getCount : Future[Int] = dbConfig.db.run(crews.length.result)

  class MariadbCrewWS extends CrewWS {
    override def listOfStubs(queryExtension: JsObject, limit: Int, sort: JsObject): Future[List[CrewStub]] = ???

    override def list(queryExtension: JsObject, limit: Int, sort: JsObject): Future[List[Crew]] = MariadbCrewDao.this.list
  }

  val ws = new MariadbCrewWS

}
