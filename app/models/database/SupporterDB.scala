package models.database

import play.api.{Logger, Play}
import models.{Crew, Role, Supporter, Address}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

/**
  * Definition of the database supporter model
  * @param id
  * @param firstName
  * @param lastName
  * @param fullName
  * @param mobilePhone
  * @param placeOfResidence
  * @param birthday
  * @param sex
  * @param profileId
  */
case class SupporterDB(
                id: Long,
                firstName: Option[String],
                lastName: Option[String],
                fullName: Option[String],
                mobilePhone: Option[String],
                placeOfResidence: Option[String],
                birthday: Option[Long],
                sex: Option[String],
                profileId: Long
                ) {
  def toSupporter( crew: Option[Crew], crewRole: Option[(Crew, Seq[Role])] = None, address: Set[Address] = Set(), active : Option[String], nvmDate : Option[Long]): Supporter =
    Supporter(firstName, lastName, fullName, mobilePhone, placeOfResidence, birthday, sex, crew, crewRole.map(_._2.toSet).getOrElse(Set()), address, active, nvmDate)
}

object SupporterDB extends ((Long, Option[String], Option[String], Option[String], Option[String], Option[String], Option[Long], Option[String], Long) => SupporterDB ){
  def apply(tuple: (Long, Option[String], Option[String], Option[String], Option[String], Option[String], Option[Long], Option[String], Long)) : SupporterDB =
    SupporterDB(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tuple._9)

  def apply(id: Long, supporter: Supporter, profileId: Long) : SupporterDB =
    SupporterDB(id, supporter.firstName, supporter.lastName, supporter.fullName, supporter.mobilePhone, supporter.placeOfResidence, supporter.birthday, supporter.sex, profileId)
  

  def read(entries: Seq[(SupporterDB, Option[SupporterCrewDB], Option[Crew], Option[AddressDB])]): Seq[Supporter] = {
    // for the reason that each Profile has only one supporter, we use the head of the sorted Seq
    val supporterDB: SupporterDB = entries.groupBy(_._1).toSeq.map(sortedCurrent => sortedCurrent._1).head
    // group all addresses and build a Set[Address] 
    val address: Set[Address] = entries.groupBy(_._4).toSeq.filter(_._1.isDefined).map(current => current._1.head.toAddress).toSet
    //build input for SupporterCrewDB.read
    val supporterCrews = entries.map(current => current._2.flatMap(sc => current._3.map(crew => (sc, crew))))

    // Get active flag from the crew
    val active = entries.filter(_._2.isDefined).foldLeft[Option[String]](None)((active, entry) => entry._2.get.active)
    // Get date for non voting membership
    val nvmDate = entries.filter(_._2.isDefined).foldLeft[Option[Long]](None)((nvmDate, entry) => entry._2.get.nvmDate)

    val crew = entries.groupBy(_._3).toSeq.map(current => current._1)
    //return Seq[Supporter]
    Seq(supporterDB.toSupporter(crew.head, SupporterCrewDB.read(supporterCrews), address, active, nvmDate))

  }

  implicit val supporterWrites : OWrites[SupporterDB] = (
    (JsPath \ "id").write[Long] and
      (JsPath \ "fistName").writeNullable[String] and
      (JsPath \ "lastName").writeNullable[String] and
      (JsPath \ "fullName").writeNullable[String] and
      (JsPath \ "mobilePhone").writeNullable[String] and
      (JsPath \ "placeOfResidence").writeNullable[String] and
      (JsPath \ "birthday").writeNullable[Long] and
      (JsPath \ "sex").writeNullable[String] and
      (JsPath \ "profileId").write[Long]
  )(unlift(SupporterDB.unapply))

  implicit val supporterReads : Reads[SupporterDB] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "fistName").readNullable[String] and
      (JsPath \ "lastName").readNullable[String] and
      (JsPath \ "fullName").readNullable[String] and
      (JsPath \ "mobilePhone").readNullable[String] and
      (JsPath \ "placeOfResidence").readNullable[String] and
      (JsPath \ "birthday").readNullable[Long] and
      (JsPath \ "sex").readNullable[String] and
      (JsPath \ "profileId").read[Long]
    ).tupled.map((supporter) => if(supporter._1.isEmpty)
    SupporterDB(0, supporter._2, supporter._3, supporter._4, supporter._5, supporter._6, supporter._7, supporter._8, supporter._9)
  else SupporterDB(supporter._1.get, supporter._2, supporter._3, supporter._4, supporter._5, supporter._6, supporter._7, supporter._8, supporter._9))
}
