package com.github.evis.highloadcup2017

import java.time.Instant

import spray.json.DefaultJsonProtocol._
import spray.json._

package object model {
  type Gender = GenderEnum.Value

  def toGender(s: String): Gender = GenderEnum.withName(s)

  val Male: Gender = GenderEnum.Male
  val Female: Gender = GenderEnum.Female

  implicit val genderFormat: JsonFormat[Gender] = new JsonFormat[Gender] {
    override def read(json: JsValue): Gender = json match {
      case JsString(s) => toGender(s)
      case _ => throw new Exception("Gender should be JsString")
    }

    override def write(obj: Gender): JsValue = JsString(obj.toString)
  }

  implicit val instantFormat: JsonFormat[Instant] = new JsonFormat[Instant] {
    override def read(json: JsValue): Instant = json match {
      case JsNumber(seconds) => Instant.ofEpochSecond(seconds.toInt)
      case _ => throw new Exception("Instant should be integer seconds")
    }

    override def write(instant: Instant): JsValue = JsNumber(instant.getEpochSecond)
  }

  implicit val userFormat: RootJsonFormat[User] = jsonFormat(User.apply,
    "id", "email", "first_name", "last_name", "gender", "birth_date")

  implicit val userUpdateFormat: RootJsonFormat[UserUpdate] = jsonFormat(UserUpdate.apply,
    "email", "first_name", "last_name", "gender", "birth_date")

  implicit val locationFormat: RootJsonFormat[Location] = jsonFormat5(Location)

  implicit val locationUpdateFormat: RootJsonFormat[LocationUpdate] = jsonFormat4(LocationUpdate)
}
