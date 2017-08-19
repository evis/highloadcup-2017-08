package com.github.evis.highloadcup2017

import java.time.Instant

import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.language.implicitConversions

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

  implicit val userUpdateFormat: RootJsonReader[UserUpdate] = new RootJsonReader[UserUpdate] {
    override def read(json: JsValue): UserUpdate = {
      implicit val fields = json.asJsObject.fields
      UserUpdate(
        getField[String]("email"),
        getField[String]("first_name"),
        getField[String]("last_name"),
        getField[Gender]("gender"),
        getField[Instant]("birth_date")
      )
    }
  }

  implicit val locationFormat: RootJsonFormat[Location] = jsonFormat5(Location)

  implicit val locationUpdateFormat: RootJsonReader[LocationUpdate] = new RootJsonReader[LocationUpdate] {
    override def read(json: JsValue): LocationUpdate = {
      implicit val fields = json.asJsObject.fields
      LocationUpdate(
        getField[String]("place"),
        getField[String]("country"),
        getField[String]("city"),
        getField[Int]("distance")
      )
    }
  }

  implicit val visitFormat: RootJsonFormat[Visit] = jsonFormat(Visit.apply,
    "id", "location", "user", "visited_at", "mark")

  implicit val visitUpdateFormat: RootJsonReader[VisitUpdate] = new RootJsonReader[VisitUpdate] {
    override def read(json: JsValue): VisitUpdate = {
      implicit val fields = json.asJsObject.fields
      VisitUpdate(
        getField[Int]("location"),
        getField[Int]("user"),
        getField[Instant]("visited_at"),
        getField[Int]("mark")
      )
    }
  }

  implicit val userVisitFormat: RootJsonFormat[UserVisit] = new RootJsonFormat[UserVisit] {
    override def write(userVisit: UserVisit): JsValue = JsObject(
      "mark" -> JsNumber(userVisit.mark),
      "visited_at" -> JsNumber(userVisit.visitedAt.getEpochSecond),
      "place" -> JsString(userVisit.place)
    )

    override def read(json: JsValue): UserVisit = ???
  }

  implicit val userVisitsFormat: RootJsonWriter[UserVisits] = jsonFormat1(UserVisits)

  implicit def instantToUserVisit(instant: Instant): UserVisit = UserVisit(
    -1, instant, null, null, -1
  )

  implicit val userVisitOrdering: Ordering[UserVisit] =
    Ordering.by(_.visitedAt)

  private def getField[T: JsonReader](name: String)(implicit fields: Map[String, JsValue]) = fields.get(name) match {
    case Some(JsNull) => deserializationError("null is forbidden")
    case Some(x) => Some(x.convertTo[T])
    case None => None
  }
}
