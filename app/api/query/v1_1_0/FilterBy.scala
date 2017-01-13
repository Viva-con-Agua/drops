package api.query.v1_1_0

import api.query._

/**
  * Created by johann on 13.01.17.
  */
case class FilterBy(all : Option[Boolean], page: Option[Page], search: Option[Set[Search]], groups : Option[Set[Group]])
