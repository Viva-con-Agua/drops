package models.converter

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import models._
import models.database._

object UserConverter {
  def buildUserListFromResult(result: Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB])]) : List[User] = {
    //foldLeft[Return Data Type](Init Value)(parameter) => function
    val userList = result.seq.foldLeft[List[User]](Nil)((userList, dbEntry) =>{
      val supporter : Supporter = dbEntry._3.toSupporter
      val loginInfo : LoginInfo = dbEntry._4.toLoginInfo
      val passwordInfo : Option[PasswordInfo] = if(dbEntry._5.isDefined){
        Option(dbEntry._5.get.toPasswordInfo)
      }else{
        None
      }

      val oauth1Info : Option[OAuth1Info] = if(dbEntry._6.isDefined){
        Option(dbEntry._6.get.toOAuth1Info)
      }else{
        None
      }

      val profile : Profile = Profile(loginInfo, dbEntry._2.confirmed, dbEntry._2.email, supporter, passwordInfo, oauth1Info)

      if(userList.length != 0 && userList.last.id == dbEntry._1.id){
        //tail = use all elements except the head element
        //reverse.tail.reverse = erease last element from list
        userList.reverse.tail.reverse ++ List(userList.last.copy(profiles = userList.last.profiles ++ List(profile)))
      }else{
        var roles = Set[Role]()
        dbEntry._1.roles.split(",").foreach(role => {roles += Role(role)})
        userList ++ List(User(dbEntry._1.publicId, List(profile), roles))
      }
    })
    userList
  }
  
  def buildUserStubListFromUserList(userList : List[User]) : List[UserStub] = {
    val userStubList : List[UserStub] = List[UserStub]()
    userList.foreach(user => {
      userStubList ++ List(user.toUserStub)
    })

    userStubList
  }

  def buildProfileListFromResult(result: Seq[(ProfileDB, SupporterDB)]) : List[Profile] = ???

  def buildProfileFromResult(result: Seq[(ProfileDB, SupporterDB)]) : Option[Profile] = ???
}
