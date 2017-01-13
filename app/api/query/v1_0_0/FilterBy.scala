package api.query.v1_0_0

import api.query._

/**
  * Created by johann on 13.01.17.
  */
case class FilterBy(page: Option[Page], search: Option[Set[Search]], groups : Option[Set[Group]])
