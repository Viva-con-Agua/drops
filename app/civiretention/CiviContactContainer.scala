package civiretention

/**
  * Created by johann on 06.10.17.
  */
case class CiviContactContainer(contact: CiviContact, phones : List[CiviPhone]) {
  /**
    * Todo: Check for criterion type_id == 1!
    * @return
    */
  def getMobilePhone : Option[CiviPhone] = phones.filter(_.phone_type_id == 1).sortBy(_.is_primary).headOption
}