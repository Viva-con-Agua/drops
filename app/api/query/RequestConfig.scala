package api.query

/**
  * Created by johann on 13.01.17.
  */
trait RequestConfig {
  val filterByPage : Boolean
  val filterBySearch : Boolean
  val filterByGroup : Boolean
  val sortBy : Boolean
}

object UserRequest extends RequestConfig {
  override val filterByGroup: Boolean = true
  override val filterBySearch: Boolean = true
  override val filterByPage: Boolean = true

  override val sortBy: Boolean = true
}

object CrewRequest extends RequestConfig {
  override val filterByGroup: Boolean = false
  override val filterBySearch: Boolean = true
  override val filterByPage: Boolean = true

  override val sortBy: Boolean = true
}

object TaskRequest extends RequestConfig {
  override val filterByPage: Boolean = false
  override val filterBySearch: Boolean = false
  override val filterByGroup: Boolean = false
  override val sortBy: Boolean = false
}
