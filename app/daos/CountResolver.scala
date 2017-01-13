package daos

import scala.concurrent.Future

/**
  * Created by johann on 13.01.17.
  */
trait CountResolver {
  def getCount : Future[Int]
}
