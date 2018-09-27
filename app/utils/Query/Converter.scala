package utils.Query

import play.Logger
import play.api.libs.json.JsObject
import slick.driver.MySQLDriver.api._
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}

case class ConverterException(msg: String) extends Exception

case class View(name : String, defaultSort : Sort) {
  def getSelectSQL : SQLActionBuilder = sql"""SELECT * FROM #$name"""
  def getCountSQL : SQLActionBuilder = sql"""SELECT COUNT(*) AS User_count FROM #$name"""
}

case class Page(limit: Long, offset: Option[Long] = None) {
  def toSqlStatement : SQLActionBuilder = {
    offset.map((o) => sql""" OFFSET #$o""") match {
      case Some(sql) => Converter.concat(sql""" LIMIT #$limit""", sql)
      case _ => sql""" LIMIT #$limit"""
    }
  }
}

case class Sort(attributes: List[String], dir: String = "ASC") {
  def toSqlStatement : SQLActionBuilder = {
    attributes.headOption.map((attribute) => sql""" ORDER BY #$attribute""").map((sqlHead) =>
    Converter.concat(attributes.tail.foldLeft[SQLActionBuilder](sqlHead)(
      (sql, attribute) => Converter.concat(sql, sql""", #$attribute""")
    ), sql""" #$dir""")).getOrElse(sql"""""")
  }
}

case class Converter(view: View, ast: Option[QueryAST], page: Option[Page]) {
  private val SELECT = view.getSelectSQL
  private val COUNT = view.getCountSQL

  private def generate(function: SQLActionBuilder) : SQLActionBuilder = {
    val filtered = ast.map((f) => Converter.concat(sql""" WHERE """, f.toSqlStatement)) match {
      case Some(f) => Converter.concat(function, f)
      case None => function
    }
    val sorted = Converter.concat(filtered, view.defaultSort.toSqlStatement)
    page match {
      case Some(p) => Converter.concat(sorted, p.toSqlStatement)
      case _ => sorted
    }
  }

  def toStatement : SQLActionBuilder = generate(SELECT)
  def toCountStatement : SQLActionBuilder = generate(COUNT)
}

object Converter {

  def apply(view: String, ast: Option[QueryAST], page: Option[Page]) : Converter = view match {
    case "Users" => Converter(View(view, Sort(List("User_id"))), ast, page)
    case "Crews" => Converter(View(view, Sort(List("Crew_id"))), ast, page)
    case _ => throw ConverterException(s"There exists no SQL View named '$view'")
  }

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
