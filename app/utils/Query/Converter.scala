package utils.Query

import play.Logger
import play.api.libs.json.JsObject
import slick.driver.MySQLDriver.api._
import slick.jdbc.SQLActionBuilder

object Converter {
  def astToSQL(ast: QueryAST) : SQLActionBuilder = {
    val query = "SELECT * FROM Crews WHERE " + ast.toSqlStatement

    sql"#$query"
  }
}
