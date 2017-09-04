package models

import java.text.{DateFormat, SimpleDateFormat}
import java.util.{Date, Locale, UUID}

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import play.api.libs.json._

import scala.util.Random

/**
  * Created by johann on 31.12.16.
  */
case class DummyUser(supporter: Supporter, profile: Profile, user : User)

object DummyUser {
  def apply(json: JsValue, specialRoles : Boolean = false)(implicit passwordHasher: PasswordHasher) : DummyUser = {
    val supporter = Supporter(
      firstName = Some((json \ "name" \ "first").as[String].capitalize),
      lastName = Some((json \ "name" \ "last").as[String].capitalize),
      fullName = Some((json \ "name" \ "first").as[String].capitalize + " " + (json \ "name" \ "last").as[String].capitalize),
      username = Some((json \ "login" \ "username").as[String]),
      mobilePhone = Some((json \ "cell").as[String]),
      placeOfResidence = Some((json \ "location" \ "city").as[String].capitalize),
      birthday = {
        val dateString = (json \ "dob").as[String]
        val format: DateFormat = new SimpleDateFormat("yyyy-MM-d hh:mm:ss", Locale.ENGLISH)
        val date: Date = format.parse(dateString)
        Some(date.getTime())
      },
      sex = Some((json \ "gender").as[String]),
      crew = None,
      pillars = {
        val pillars = Pillar.getAll
        val indices = DummyUser.randomSubset(pillars.size)
        indices.foldLeft[Set[Pillar]](Set())(
          (res, i) => res + pillars.toList(i)
        )
      }
    )
    val profile = Profile(
      loginInfo = LoginInfo(CredentialsProvider.ID, (json \ "email").as[String]),
      confirmed = true,
      email = Some((json \ "email").as[String]),
      supporter = supporter,
      passwordInfo = Some(passwordHasher.hash((json \ "login" \ "password").as[String])),
      oauth1Info = None,
      avatarUrl = Some((json \ "picture" \ "medium").as[String])
    )

    val user = User(
      id = UUID.randomUUID(),
      profiles = List(profile),
      roles = {
        val roles = Role.getAll
        val indices = DummyUser.randomSubset(roles.size)
        indices.foldLeft[Set[Role]](Set(RoleSupporter))(
          (resRoles, i) => specialRoles match {
            case true => resRoles + roles.toList(i)
            case false => resRoles
          }
        )
      }
    )
    DummyUser(supporter, profile, user)
  }

  def randomSubset(setLength : Int) : Set[Int] = {
    val rand = new Random(System.currentTimeMillis())

    (for(
      i <- 1 to rand.nextInt(setLength)
    ) yield {
      rand.nextInt(setLength)
    }).toSet
  }

  def setRandomCrew(user: DummyUser, crews : Set[Crew]) : DummyUser = {
    val rand = new Random(System.currentTimeMillis())
    val index = rand.nextInt(crews.size)
    val active = math.random < 0.4 // yields true with a probability of 0.40

    val newSupporter = user.supporter.copy(crew = Some(crews.toList(index)))
    val newProfile = user.profile.copy(supporter = newSupporter)
    val newUser = user.user.copy(profiles = List(newProfile))

    DummyUser(newSupporter, newProfile, newUser)
  }
}
