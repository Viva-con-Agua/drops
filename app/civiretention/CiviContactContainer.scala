package civiretention

/**
  * Created by johann on 06.10.17.
  */
case class CiviContactContainer(contact: CiviContact, phones : List[CiviPhone], emails : List[CiviEmail], addresses: List[CiviAddress]) {
  /**
    * Todo: Check for criterion type_id == 1!
    * @return
    */
  def getMobilePhone : Option[CiviPhone] = phones.filter(_.phone_type_id == 1).sortBy(_.is_primary).headOption

  def getEmail : Option[CiviEmail] = emails.filter((email) => !email.on_hold && !email.is_bulkmail).sortBy(_.is_primary).headOption

  def getAddress : Option[CiviAddress] = addresses.sortBy(_.is_primary).headOption
}