package models.converter

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import daos.CrewDao
import javax.inject.Inject
import models._
import models.database._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class UserConverter @Inject()(crewDao: CrewDao) {
  //  /**
  //    * @deprecated
  //    * @param result
  //    * @return
  //    */
  //  def buildUserListFromResult(result: Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])]) : List[User] = {
  //    result.foldLeft[List[Future[User]]](Nil)((userList, dbEntry) => {
  //
  //      val profile : Future[Option[Profile]] = toProfileOption(Seq((dbEntry._2, dbEntry._3, dbEntry._4, dbEntry._5, dbEntry._6, dbEntry._7, dbEntry._8)))
  //
  //      userList.lastOption match {
  //        case Some(user) if user.id == dbEntry._1.id => profile.map(p =>
  //          userList.takeWhile(_.id != user.id) ++ List(user.copy(profiles = user.profiles ++ p.map(List( _ )).getOrElse(Nil)))
  //        )
  //        case _ => userList ++ List(
  //          User(
  //            dbEntry._1.publicId,
  //            profile.map(List( _ )).getOrElse(Nil),
  //            dbEntry._1.updated,
  //            dbEntry._1.created,
  //            dbEntry._1.roles.split(",").map(Role( _ )).toSet
  //          )
  //        )
  //      }
  //    })
  //  }

  def convertTuple(result: Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])]) : Future[Seq[User]] = {
    Future.sequence(result.foldLeft[Map[UserDB,Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])]]](Map())((coll, t) => {
      coll.contains(t._1) match {
        case true => {
          val list : Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])] =
            coll.get(t._1) match {
              case Some(s) => s
              case None => Nil
            }
          val newProfile = (t._2, t._3, t._4, t._5, t._6, t._7, t._8)
          (coll - t._1) + (t._1 -> (list ++ Seq(newProfile)))
        }
        case false => coll + (t._1 -> Seq((t._2, t._3, t._4, t._5, t._6, t._7, t._8)))
      }
    }).map(pair => ~> (pair._1, toProfileList(pair._2))).toSeq)
  }

  def convertPair(result: Seq[(UserDB, Seq[Profile])]): List[User] = {
    result.map(pair => ~~>(pair._1, pair._2)).toList
    //    merge(result.foldLeft[Seq[(UserDB, Option[Profile])]](Seq())((list, pair) =>
    //      list ++ pair._2.map(profile => (pair._1, Some(profile)))
    //    ).map(t =>
    //      ~> (t._1, t._2)
    //    )).toList
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

  def ~> (dbUser: UserDB, profiles: Future[Seq[Profile]]) : Future[User] = {
    profiles.map(ps => ~~> (dbUser, ps))
  }

  def ~~> (dbUser: UserDB, profiles: Seq[Profile]) : User = {
    User(
      dbUser.publicId,
      profiles.toList,
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


  def toProfileOption(result: Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])]) : Future[Option[Profile]] =
    toProfileList(result).map(_.headOption)

  def toProfileList(result: Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])]) : Future[Seq[Profile]] = {
    Future.sequence(result.map(_._6).filter(_.isDefined).map(_.get).map((sc) => {
      crewDao.find(sc.crewId).map(_.flatMap(crew =>
        sc.pillar.map(pillar => Role[(Crew, Pillar)]((crew, Pillar(pillar))))
      ))
    })).map(_.filter(_.isDefined).map(_.get))
      .map((supporterRoles) => result.map(res => {
      val supporter : Supporter = res._2.toSupporter(res._7.map(crew => (crew, supporterRoles)))
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
    }))
  }
}