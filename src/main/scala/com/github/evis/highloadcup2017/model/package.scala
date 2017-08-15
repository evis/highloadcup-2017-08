package com.github.evis.highloadcup2017

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

  implicit val userFormat: RootJsonFormat[User] = jsonFormat(User.apply,
    "id", "email", "first_name", "last_name", "gender", "birth_date")
}
