package utils.Query

import play.Logger
import play.api.libs.json.JsObject
import slick.driver.MySQLDriver.api._
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}

case class Page(limit: Long, offset: Option[Long] = None) {
  def toSqlStatement : SQLActionBuilder = {
    offset.map((o) => sql""" OFFSET #$o""") match {
      case Some(sql) => Converter.concat(sql""" LIMIT #$limit""", sql)
      case _ => sql""" LIMIT #$limit"""
    }
  }
}

case class Converter(view: String, ast: Option[QueryAST], page: Option[Page]) {
  private val SELECT = sql"""SELECT * FROM #$view"""
  private val COUNT = sql"""SELECT COUNT(*) AS User_count FROM #$view"""

  private def generate(function: SQLActionBuilder) : SQLActionBuilder = {
    val filtered = ast.map((f) => Converter.concat(sql""" WHERE """, f.toSqlStatement)) match {
      case Some(f) => Converter.concat(function, f)
      case None => function
    }
    page match {
      case Some(p) => Converter.concat(filtered, p.toSqlStatement)
      case _ => filtered
    }
  }

  def toStatement : SQLActionBuilder = generate(SELECT)
  def toCountStatement : SQLActionBuilder = generate(COUNT)
}

object Converter {

  /**
    * Use [[Converter]] instance and [[Converter.toStatement]] instead!
    *
    * @param ast
    * @param view
    * @return
    * @deprecated
    */
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
