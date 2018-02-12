package models.converter

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import models.{Profile, Supporter, User}
import models.database._

object UserConverter {
  def buildUserListFromResult(result: Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB])]) : List[User] = {
    //foldLeft[Return Data Type](Init Value)(parameter) => function
    val userList = result.seq.foldLeft[List[User]](Nil)((userList, dbEntry) =>{
      val supporter : Supporter = dbEntry._3.toSupporter
      val loginInfo : LoginInfo = dbEntry._4.toLoginInfo
      val passwordInfo : Option[PasswordInfo] = if(dbEntry._5.isDefined){
        Option(dbEntry._5.get.toPasswordInfo)
      }else{
        None
      }

      val profile : Profile = Profile(loginInfo, dbEntry._2.confirmed, dbEntry._2.email, supporter, passwordInfo)

      if(userList.length != 0 && userList.last.id == dbEntry._1.id){
        //tail = use all elements except the head element
        //reverse.tail.reverse = erease last element from list
        userList.reverse.tail.reverse ++ List(userList.last.copy(profiles = userList.last.profiles ++ List(profile)))
      }else{
        userList ++ List(User(dbEntry._1.publicId, List(profile), Set()))
      }
    })
    userList
  }
}
