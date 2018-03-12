package models.database

import models.Supporter
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
                profileId: Long,
                crewId: Option[Long]
                ) {
  def toSupporter: Supporter = Supporter(firstName, lastName, fullName, mobilePhone, placeOfResidence, birthday, sex)
}

object SupporterDB extends ((Long, Option[String], Option[String], Option[String], Option[String], Option[String], Option[Long], Option[String], Long, Option[Long]) => SupporterDB ){
  def apply(tuple: (Long, Option[String], Option[String], Option[String], Option[String], Option[String], Option[Long], Option[String], Long, Option[Long])) : SupporterDB =
    SupporterDB(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tuple._9, tuple._10)

  def apply(id: Long, supporter: Supporter, profileId: Long) : SupporterDB =
    SupporterDB(id, supporter.firstName, supporter.lastName, supporter.fullName, supporter.mobilePhone, supporter.placeOfResidence, supporter.birthday, supporter.sex, profileId, None)

  implicit val supporterWrites : OWrites[SupporterDB] = (
    (JsPath \ "id").write[Long] and
      (JsPath \ "fistName").writeNullable[String] and
      (JsPath \ "lastName").writeNullable[String] and
      (JsPath \ "fullName").writeNullable[String] and
      (JsPath \ "mobilePhone").writeNullable[String] and
      (JsPath \ "placeOfResidence").writeNullable[String] and
      (JsPath \ "birthday").writeNullable[Long] and
      (JsPath \ "sex").writeNullable[String] and
      (JsPath \ "profileId").write[Long] and
      (JsPath \ "crewId").writeNullable[Long]
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
      (JsPath \ "profileId").read[Long] and
      (JsPath \ "crewId").readNullable[Long]
    ).tupled.map((supporter) => if(supporter._1.isEmpty)
    SupporterDB(0, supporter._2, supporter._3, supporter._4, supporter._5, supporter._6, supporter._7, supporter._8, supporter._9, supporter._10)
  else SupporterDB(supporter._1.get, supporter._2, supporter._3, supporter._4, supporter._5, supporter._6, supporter._7, supporter._8, supporter._9, supporter._10))
}