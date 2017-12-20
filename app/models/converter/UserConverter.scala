package models.converter

import com.mohiva.play.silhouette.api.LoginInfo
import models.{Profile, Supporter, User}
import models.database.{LoginInfoDB, ProfileDB, SupporterDB, UserDB}

object UserConverter {
  def buildUserListFromResult(result: Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB)]) : List[User] = {
    //foldLeft[Return Data Type](Init Value)(parameter) => function
    val userList = result.seq.foldLeft[List[User]](Nil)((userList, dbEntry) =>{
      val supporter : Supporter = dbEntry._3.toSupporter
      val loginInfo : LoginInfo = dbEntry._4.toLoginInfo
      val profile : Profile = Profile(loginInfo, dbEntry._2.confirmed, dbEntry._2.email, supporter)

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
