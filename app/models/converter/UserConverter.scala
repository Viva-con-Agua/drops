package models.converter

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import models._
import models.database._

object UserConverter {
  /**
    * @deprecated
    * @param result
    * @return
    */
  def buildUserListFromResult(result: Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])]) : List[User] = {
    result.foldLeft[List[User]](Nil)((userList, dbEntry) => {

      val profile : Option[Profile] = toProfileOption(Seq((dbEntry._2, dbEntry._3, dbEntry._4, dbEntry._5, dbEntry._6, dbEntry._7, dbEntry._8)))

      userList.lastOption match {
        case Some(user) if user.id == dbEntry._1.id =>
          userList.takeWhile(_.id != user.id) ++ List(user.copy(profiles = user.profiles ++ profile.map(List( _ )).getOrElse(Nil)))
        case _ => userList ++ List(
          User(
            dbEntry._1.publicId,
            profile.map(List( _ )).getOrElse(Nil),
            dbEntry._1.updated,
            dbEntry._1.created,
            dbEntry._1.roles.split(",").map(Role( _ )).toSet
          )
        )
      }
    })
  }

  def convertTuple(result: Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])]) : List[User] = {
    merge(result.map(t =>
      ~> (t._1, toProfileOption(Seq((t._2, t._3, t._4, t._5, t._6, t._7, t._8))))
    )).toList
  }

  def convertPair(result: Seq[(UserDB, Seq[Profile])]): List[User] = {
    merge(result.foldLeft[Seq[(UserDB, Option[Profile])]](Seq())((list, pair) =>
      list ++ pair._2.map(profile => (pair._1, Some(profile)))
    ).map(t =>
      ~> (t._1, t._2)
    )).toList
  }

  def merge(users: Seq[User]): Seq[User] = users.foldLeft[Seq[User]](Seq())((list, user) => {
    list.map(_ match {
      case u: User if u.id == user.id => u.copy(profiles = u.profiles ++ user.profiles)
      case u: User => u
    })
    list.find(_.id == user.id) match {
      case Some(u) => list
      case _ => list ++ List(user)
    }
  })

  def ~> (dbUser: UserDB, profile: Option[Profile]) : User = {
    User(
      dbUser.publicId,
      profile.map(List( _ )).getOrElse(Nil),
      dbUser.updated,
      dbUser.created,
      dbUser.roles.split(",").map(Role( _ )).toSet
    )
  }
  
  def buildUserStubListFromUserList(userList : List[User]) : List[UserStub] = {
    val userStubList : List[UserStub] = List[UserStub]()
    userList.foreach(user => {
      userStubList ++ List(user.toUserStub)
    })

    userStubList
  }


  def toProfileOption(result: Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])]) : Option[Profile] =
    toProfileList(result).headOption

  def toProfileList(result: Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])]) : List[Profile] = {
    result.map(res => {
      val role : Option[Role] = res._6.flatMap(supporterCrew =>
        res._7.flatMap(crew =>
          supporterCrew.pillar.map(pillar => Role[(Crew, Pillar)]((crew, Pillar(pillar))))
        )
      )
      val supporter : Supporter = res._2.toSupporter(res._7.map(crew => (crew, role)))
      val loginInfo : LoginInfo = res._3.toLoginInfo
      val passwordInfo : Option[PasswordInfo] = if(res._4.isDefined){
        Option(res._4.get.toPasswordInfo)
      }else{
        None
      }

      val oauth1Info : Option[OAuth1Info] = if(res._5.isDefined){
        Option(res._5.get.toOAuth1Info)
      }else{
        None
      }

      Profile(loginInfo, res._1.confirmed, res._1.email, supporter, passwordInfo, oauth1Info)
    }).toList
  }
}
