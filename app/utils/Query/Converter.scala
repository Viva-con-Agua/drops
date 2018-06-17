package utils.Query

import play.Logger
import play.api.libs.json.JsObject
import slick.driver.MySQLDriver.api._
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}

object Converter {
  def astToSQL(ast: QueryAST, view: String) : SQLActionBuilder = {
    //val query = "SELECT * FROM Crews WHERE " + ast.toSqlStatement
    val filter = ast.toSqlStatement
    concat(sql"""SELECT * FROM #$view""", Converter.concat(sql""" WHERE """, filter))
  }

  def concat(a: SQLActionBuilder, b: SQLActionBuilder): SQLActionBuilder = {
    SQLActionBuilder(a.queryParts ++ b.queryParts, new SetParameter[Unit] {
      def apply(p: Unit, pp: PositionedParameters): Unit = {
        a.unitPConv.apply(p, pp)
        b.unitPConv.apply(p, pp)
      }
    })
  }
}
