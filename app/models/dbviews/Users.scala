package models.dbviews

import java.util.UUID

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

case class Users (
                 user: Option[UserView],
                 profile: Option[ProfileView],
                 loginInfo: Option[LoginInfoView],
                 supporterView: Option[SupporterView],
                 supporterCrewView: Option[SupporterCrewView]
                 )extends ViewObject{
  def getValue(viewname: String): ViewBase = {
    viewname match {
      case "user" => user.get
      case "profile" => profile.get
      case "loginInfo" => loginInfo.get
      case "supporter" => supporterView.get
      case "supporterCrew" => supporterCrewView.get
    }
  }

  def isFieldDefined(viewname: String): Boolean = {
    viewname match {
      case "user" => user.isDefined
      case "profile" => profile.isDefined
      case "loginInfo" => loginInfo.isDefined
      case "supporter" => supporterView.isDefined
      case "supporterCrew" => supporterCrewView.isDefined
    }
  }
}

object Users{
  def apply(tuple: (Option[UserView], Option[ProfileView], Option[LoginInfoView], Option[SupporterView], Option[SupporterCrewView])) : Users =
    Users(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)

  implicit val usersWrites : OWrites[Users] = (
    (JsPath \ "user").writeNullable[UserView] and
      (JsPath \ "profile").writeNullable[ProfileView] and
      (JsPath \ "loginInfo").writeNullable[LoginInfoView] and
      (JsPath \ "supporter").writeNullable[SupporterView] and
      (JsPath \ "supporterCrew").writeNullable[SupporterCrewView]
  )(unlift(Users.unapply))

  implicit val usersReads: Reads[Users] = (
    (JsPath \ "user").readNullable[UserView] and
      (JsPath \ "profile").readNullable[ProfileView] and
      (JsPath \ "loginInfo").readNullable[LoginInfoView] and
      (JsPath \ "supporter").readNullable[SupporterView] and
      (JsPath \ "supporterCrew").readNullable[SupporterCrewView]
  ).tupled.map(Users( _ ))
}

case class UserView(
                   publicId : Option[Map[String, UUID]],
                   roles : Option[Map[String, String]],
                   updated: Option[Map[String, List[Long]]],
                   created: Option[Map[String, List[Long]]]
                   ) extends ViewBase {
  def getValue(fieldname: String, index: Int): Any = {
    fieldname match {
      case "publicId" => publicId.get.get(index.toString).get
      case "roles" => roles.get.get(index.toString).get
      case "updated" => updated.get.get(index.toString).get
      case "created" => created.get.get(index.toString).get
    }
  }

  def isFieldDefined(fieldname: String, index: Int): Boolean = {
    fieldname match {
      case "publicId" => {
        publicId.isDefined match {
          case true => publicId.get.keySet.contains(index.toString)
          case false => false
        }
      }
      case "roles" => {
        roles.isDefined match {
          case true => roles.get.keySet.contains(index.toString)
          case false => false
        }
      }
      case "updated" =>
        updated.isDefined match {
          case true => updated.get.keySet.contains(index.toString)
          case false => false
        }
      case "created" =>
        created.isDefined match {
          case true => created.get.keySet.contains(index.toString)
          case false => false
        }
    }
  }
}

object UserView{
  def apply(tuple: (Option[Map[String, UUID]], Option[Map[String, String]], Option[Map[String, List[Long]]], Option[Map[String, List[Long]]])) : UserView =
    UserView(tuple._1, tuple._2, tuple._3, tuple._4)

  implicit val userViewWrites : OWrites[UserView] = (
    (JsPath \ "publicId").writeNullable[Map[String, UUID]] and
      (JsPath \ "roles").writeNullable[Map[String, String]] and
      (JsPath \ "updated").writeNullable[Map[String, List[Long]]] and
      (JsPath \ "created").writeNullable[Map[String, List[Long]]]
    )(unlift(UserView.unapply))

  implicit val userViewReads: Reads[UserView] = (
    (JsPath \ "publicId").readNullable[Map[String, UUID]].orElse(
      (JsPath \ "publicId").readNullable[UUID].map(_.map(p => Map("0" -> p)))) and
      (JsPath \ "roles").readNullable[Map[String, String]].orElse(
        (JsPath \ "roles").readNullable[String].map(_.map(n => Map("0" -> n)))) and
      (JsPath \ "updated").readNullable[Map[String, List[Long]]].orElse(
        (JsPath \ "updated").readNullable[Map[String, Long]].map(_.map(p => p.mapValues((v) => List(v)))).orElse(
          (JsPath \ "updated").readNullable[List[Long]].map(_.map(p => Map("0" -> p))).orElse(
        (JsPath \ "updated").readNullable[Long].map(_.map(p => Map("0" -> List(p))))))) and
      (JsPath \ "created").readNullable[Map[String, List[Long]]].orElse(
        (JsPath \ "created").readNullable[Map[String, Long]].map(_.map(p => p.mapValues((v) => List(v)))).orElse(
          (JsPath \ "created").readNullable[List[Long]].map(_.map(p => Map("0" -> p))).orElse(
        (JsPath \ "created").readNullable[Long].map(_.map(p => Map("0" -> List(p)))))))
  ).tupled.map(UserView( _ ))
}

case class ProfileView(
                      confirmed: Option[Map[String, Boolean]],
                      email: Option[Map[String, String]]
                      ) extends ViewBase {
  def getValue(fieldname: String, index: Int): Any = {
    fieldname match {
      case "confirmed" => confirmed.get.get(index.toString).get
      case "email" => email.get.get(index.toString).get
    }
  }

  def isFieldDefined(fieldname: String, index: Int): Boolean = {
    fieldname match {
      case "confirmed" => {
        confirmed.isDefined match {
          case true => confirmed.get.keySet.contains(index.toString)
          case false => false
        }
      }

      case "email" => {
        email.isDefined match {
          case true => email.get.keySet.contains(index.toString)
          case false => false
        }
      }
    }
  }
}

object ProfileView{
  def apply(tuple: (Option[Map[String, Boolean]], Option[Map[String, String]])) : ProfileView =
    ProfileView(tuple._1, tuple._2)

  implicit val profileViewWrites : OWrites[ProfileView] = (
    (JsPath \ "confirmed").writeNullable[Map[String, Boolean]] and
      (JsPath \ "email").writeNullable[Map[String, String]]
    )(unlift(ProfileView.unapply))

  implicit val profileViewReads : Reads[ProfileView] = (
    (JsPath \ "confirmed").readNullable[Map[String, Boolean]].orElse(
      (JsPath \ "confirmed").readNullable[Boolean].map(_.map(p => Map("0" -> p))))and
      (JsPath \ "email").readNullable[Map[String, String]].orElse(
        (JsPath \ "email").readNullable[String].map(_.map(n => Map("0" -> n))))
    ).tupled.map(ProfileView( _ ))
}

case class LoginInfoView(
                        providerId: Option[Map[String, String]],
                        providerKey: Option[Map[String, String]]
                        ) extends ViewBase {
  def getValue(fieldname: String, index: Int): Object = {
    fieldname match {
      case "providerId" => providerId.get.get(index.toString).get
      case "providerKey" => providerKey.get.get(index.toString).get
    }
  }

  def isFieldDefined(fieldname: String, index: Int): Boolean = {
    fieldname match {
      case "providerId" => {
        providerId.isDefined match {
          case true => providerId.get.keySet.contains(index.toString)
          case false => false
        }
      }

      case "providerKey" => {
        providerKey.isDefined match {
          case true => providerKey.get.keySet.contains(index.toString)
          case false => false
        }
      }
    }
  }
}

object LoginInfoView{
  def apply(tuple: (Option[Map[String, String]], Option[Map[String, String]])): LoginInfoView =
    LoginInfoView(tuple._1, tuple._2)

  implicit val loginInfoViewWrites : OWrites[LoginInfoView] = (
    (JsPath \ "providerId").writeNullable[Map[String, String]] and
      (JsPath \ "providerKey").writeNullable[Map[String, String]]
    )(unlift(LoginInfoView.unapply))

  implicit val loginInfoViewReads : Reads[LoginInfoView] = (
    (JsPath \ "providerId").readNullable[Map[String, String]].orElse(
      (JsPath \ "providerId").readNullable[String].map(_.map(p => Map("0" -> p))))and
      (JsPath \ "providerKey").readNullable[Map[String, String]].orElse(
        (JsPath \ "providerKey").readNullable[String].map(_.map(n => Map("0" -> n))))
    ).tupled.map(LoginInfoView( _ ))
}

case class SupporterView(
                        firstName : Option[Map[String, String]],
                        lastName : Option[Map[String, String]],
                        fullName : Option[Map[String, String]],
                        mobilePhone: Option[Map[String, String]],
                        placeOfResidence: Option[Map[String, String]],
                        birthday: Option[Map[String, List[Long]]],
                        sex: Option[Map[String, String]]
                        ) extends ViewBase {
  def getValue(fieldname: String, index: Int): Any = {
    fieldname match {
      case "firstName" => firstName.get.get(index.toString).get
      case "lastName" => lastName.get.get(index.toString).get
      case "fullName" => fullName.get.get(index.toString).get
      case "mobilePhone" => mobilePhone.get.get(index.toString).get
      case "placeOfResidence" => placeOfResidence.get.get(index.toString).get
      case "birthday" => birthday.get.get(index.toString).get
      case "sex" => sex.get.get(index.toString).get
    }
  }

  def isFieldDefined(fieldname: String, index: Int): Boolean = {
    fieldname match {
      case "firstName" => {
        firstName.isDefined match {
          case true => firstName.get.keySet.contains(index.toString)
          case false => false
        }
      }

      case "lastName" => {
        lastName.isDefined match {
          case true => lastName.get.keySet.contains(index.toString)
          case false => false
        }
      }

      case "fullName" => {
        fullName.isDefined match {
          case true => fullName.get.keySet.contains(index.toString)
          case false => false
        }
      }

      case "mobilePhone" => {
        mobilePhone.isDefined match {
          case true => mobilePhone.get.keySet.contains(index.toString)
          case false => false
        }
      }

      case "placeOfResidence" => {
        placeOfResidence.isDefined match {
          case true => placeOfResidence.get.keySet.contains(index.toString)
          case false => false
        }
      }

      case "birthday" => {
        birthday.isDefined match {
          case true => birthday.get.keySet.contains(index.toString)
          case false => false
        }
      }

      case "sex" => {
        sex.isDefined match {
          case true => sex.get.keySet.contains(index.toString)
          case false => false
        }
      }
    }
  }
}

object SupporterView{
  def apply(tuple: (Option[Map[String, String]], Option[Map[String, String]], Option[Map[String, String]], Option[Map[String, String]], Option[Map[String, String]], Option[Map[String, List[Long]]], Option[Map[String, String]])): SupporterView =
    SupporterView(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7)

  implicit val supporterViewWrites : OWrites[SupporterView] = (
    (JsPath \ "firstName").writeNullable[Map[String, String]] and
      (JsPath \ "lastName").writeNullable[Map[String, String]] and
      (JsPath \ "fullName").writeNullable[Map[String, String]] and
      (JsPath \ "mobilePhone").writeNullable[Map[String, String]] and
      (JsPath \ "placeOfResidence").writeNullable[Map[String, String]] and
      (JsPath \ "birthday").writeNullable[Map[String, List[Long]]] and
      (JsPath \ "sex").writeNullable[Map[String, String]]
    )(unlift(SupporterView.unapply))

  implicit val supporterViewReads : Reads[SupporterView] = (
    (JsPath \ "firstName").readNullable[Map[String, String]].orElse(
      (JsPath \ "firstName").readNullable[String].map(_.map(p => Map("0" -> p))))and
      (JsPath \ "lastName").readNullable[Map[String, String]].orElse(
        (JsPath \ "lastName").readNullable[String].map(_.map(n => Map("0" -> n))))and
      (JsPath \ "fullName").readNullable[Map[String, String]].orElse(
        (JsPath \ "fullName").readNullable[String].map(_.map(c => Map("0" -> c))))and
      (JsPath \ "mobilePhone").readNullable[Map[String, String]].orElse(
        (JsPath \ "mobilePhone").readNullable[String].map(_.map(c => Map("0" -> c))))and
      (JsPath \ "placeOfResidence").readNullable[Map[String, String]].orElse(
        (JsPath \ "placeOfResidence").readNullable[String].map(_.map(c => Map("0" -> c))))and
      (JsPath \ "birthday").readNullable[Map[String, List[Long]]].orElse(
        (JsPath \ "birthday").readNullable[Map[String, Long]].map(_.map(c => c.mapValues(v => List(v)))).orElse(
          (JsPath \ "birthday").readNullable[List[Long]].map(_.map(c => Map("0" -> c))).orElse(
        (JsPath \ "birthday").readNullable[Long].map(_.map(c => Map("0" -> List(c))))))) and
      (JsPath \ "sex").readNullable[Map[String, String]].orElse(
        (JsPath \ "sex").readNullable[String].map(_.map(c => Map("0" -> c))))

    ).tupled.map(SupporterView( _ ))
}

case class SupporterCrewView(
                              role : Option[Map[String, String]],
                              pillar : Option[Map[String, String]],
                              publicId : Option[Map[String, UUID]],
                              name : Option[Map[String, String]],
                              country : Option[Map[String, String]]
                            ) extends ViewBase {
  def getValue(fieldname: String, index: Int): Any = {
    fieldname match {
      case "role" => role.get.get(index.toString).get
      case "pillar" => pillar.get.get(index.toString).get
      case "publicId" => publicId.get.get(index.toString).get
      case "name" => name.get.get(index.toString).get
      case "country" => country.get.get(index.toString).get
    }
  }

  def isFieldDefined(fieldname: String, index: Int): Boolean = {
    fieldname match {
      case "role" => {
        role.isDefined match {
          case true => role.get.keySet.contains(index.toString)
          case false => false
        }
      }

      case "pillar" => {
        pillar.isDefined match {
          case true => pillar.get.keySet.contains(index.toString)
          case false => false
        }
      }

      case "publicId" => {
        publicId.isDefined match {
          case true => publicId.get.keySet.contains(index.toString)
          case false => false
        }
      }
      case "name" => {
        name.isDefined match {
          case true => name.get.keySet.contains(index.toString)
          case false => false
        }
      }
      case "country" => {
        country.isDefined match {
          case true => country.get.keySet.contains(index.toString)
          case false => false
        }
      }
    }
  }
}

object SupporterCrewView {
  def apply(t: (Option[Map[String, String]], Option[Map[String, String]], Option[Map[String, UUID]], Option[Map[String, String]], Option[Map[String, String]])): SupporterCrewView =
    SupporterCrewView(t._1, t._2, t._3, t._4, t._5)

  implicit val supporterCrewViewWrites : OWrites[SupporterCrewView] = (
    (JsPath \ "role").writeNullable[Map[String, String]] and
      (JsPath \ "pillar").writeNullable[Map[String, String]] and
      (JsPath \ "publicId").writeNullable[Map[String, UUID]] and
      (JsPath \ "name").writeNullable[Map[String, String]] and
      (JsPath \ "country").writeNullable[Map[String, String]]
    )(unlift(SupporterCrewView.unapply))

  implicit val supporterCrewViewReads : Reads[SupporterCrewView] = (
    (JsPath \ "role").readNullable[Map[String, String]].orElse(
      (JsPath \ "role").readNullable[String].map(_.map(p => Map("0" -> p)))) and
      (JsPath \ "pillar").readNullable[Map[String, String]].orElse(
        (JsPath \ "pillar").readNullable[String].map(_.map(n => Map("0" -> n)))) and
      (JsPath \ "publicId").readNullable[Map[String, UUID]].orElse(
        (JsPath \ "publicId").readNullable[UUID].map(_.map(p => Map("0" -> p)))) and
      (JsPath \ "name").readNullable[Map[String, String]].orElse(
        (JsPath \ "name").readNullable[String].map(_.map(n => Map("0" -> n)))) and
      (JsPath \ "country").readNullable[Map[String, String]].orElse(
        (JsPath \ "country").readNullable[String].map(_.map(n => Map("0" -> n))))

    ).tupled.map(SupporterCrewView( _ ))
}