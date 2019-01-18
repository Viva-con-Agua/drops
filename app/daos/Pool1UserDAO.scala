package daos

import java.util.UUID

import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import com.mohiva.play.silhouette.api.LoginInfo
import daos.schema.Pool1UserTableDef
import models._
import models.Pool1User
import models.database.Pool1UserDB
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._

trait Pool1UserDao {
  def find(email: String): Future[Option[Pool1User]]
  def save(user: Pool1User): Future[Pool1User]
  def confirm(email: String): Future[Option[Pool1User]]
}

class MariadbPool1UserDao extends Pool1UserDao{
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val pool1Users = TableQuery[Pool1UserTableDef]

  override def find(email: String): Future[Option[Pool1User]] =
    dbConfig.db.run(pool1Users.filter(p1U => p1U.email === email).result).map(_.headOption.map(_.toPool1User))

  def find(id: Long): Future[Option[Pool1User]] =
    dbConfig.db.run(pool1Users.filter(p1U => p1U.id === id).result).map(_.headOption.map(_.toPool1User))

  override def save(user: Pool1User): Future[Pool1User] =
    dbConfig.db.run((pool1Users returning pool1Users.map(_.id)) += Pool1UserDB(user)).flatMap((id) => find(id)).map(_.get)

  override def confirm(email: String): Future[Option[Pool1User]] = {
    val updatePool1User = for {p1U <- pool1Users.filter(_.email === email)} yield p1U.confirmed
    dbConfig.db.run(updatePool1User.update(true)).flatMap(_ => find(email))
  }
}
