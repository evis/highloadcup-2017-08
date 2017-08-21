package com.github.evis.highloadcup2017.api

import colossus.protocols.http.Http
import colossus.protocols.http.HttpMethod.{Get, Post}
import colossus.protocols.http.UrlParsing.{Root, _}
import colossus.service.Callback
import colossus.service.GenRequestHandler.PartialHandler
import com.github.evis.highloadcup2017.dao.{LocationDao, UserDao, VisitDao}
import com.github.evis.highloadcup2017.model.{Location, LocationAvgRequest, LocationUpdate, User, UserUpdate, UserVisitsRequest, Visit, VisitUpdate}
import spray.json._

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

class ColossusHandler(userDao: UserDao,
                      locationDao: LocationDao,
                      visitDao: VisitDao) extends Decoders with Parsers {
  def httpHandle: PartialHandler[Http] = {
    // creates
    case req@Post on Root / "users" / "new" =>
      req.body.as[User] match {
        case Success(user) =>
          userDao.create(user)
          Callback.successful(req.ok("{}"))
        case Failure(_) =>
          Callback.successful(req.badRequest(""))
      }
    case req@Post on Root / "locations" / "new" =>
      req.body.as[Location] match {
        case Success(location) =>
          locationDao.create(location)
          Callback.successful(req.ok("{}"))
        case Failure(_) =>
          Callback.successful(req.badRequest(""))
      }
    case req@Post on Root / "visits" / "new" =>
      req.body.as[Visit] match {
        case Success(visit) =>
          visitDao.create(visit)
          Callback.successful(req.ok("{}"))
        case Failure(_) =>
          Callback.successful(req.badRequest(""))
      }
    // updates
    case req@Post on Root / "users" / Integer(id) =>
      req.body.as[UserUpdate] match {
        case Success(update) =>
          userDao.update(id, update) match {
            case Some(_) =>
              Callback.successful(req.ok("{}"))
            case None =>
              Callback.successful(req.notFound(""))
          }
        case Failure(_) =>
          Callback.successful(req.badRequest(""))
      }
    case req@Post on Root / "locations" / Integer(id) =>
      req.body.as[LocationUpdate] match {
        case Success(update) =>
          locationDao.update(id, update) match {
            case Some(_) =>
              Callback.successful(req.ok("{}"))
            case None =>
              Callback.successful(req.notFound(""))
          }
        case Failure(_) =>
          Callback.successful(req.badRequest(""))
      }
    case req@Post on Root / "visits" / Integer(id) =>
      req.body.as[VisitUpdate] match {
        case Success(update) =>
          visitDao.update(id, update) match {
            case Some(_) =>
              Callback.successful(req.ok("{}"))
            case None =>
              Callback.successful(req.notFound(""))
          }
        case Failure(_) =>
          Callback.successful(req.badRequest(""))
      }
    // reads
    case req@Get on Root / "users" / Integer(id) =>
      userDao.read(id) match {
        case Some(user) =>
          Callback.successful(req.ok(user.toJson.compactPrint))
        case None =>
          Callback.successful(req.notFound(""))
      }
    case req@Get on Root / "locations" / Integer(id) =>
      locationDao.read(id) match {
        case Some(location) =>
          Callback.successful(req.ok(location.toJson.compactPrint))
        case None =>
          Callback.successful(req.notFound(""))
      }
    case req@Get on Root / "visits" / Integer(id) =>
      visitDao.read(id) match {
        case Some(visit) =>
          Callback.successful(req.ok(visit.toJson.compactPrint))
        case None =>
          Callback.successful(req.notFound(""))
      }
    // user visits
    case req@Get on Root / "users" / Integer(id) / "visits" =>
      val params = req.head.parameters
      val fromDate = params.optParam[Int]("fromDate")
      val toDate = params.optParam[Int]("toDate")
      val country = params.optParam[String]("country")
      val toDistance = params.optParam[Int]("toDistance")
      if (anyFailed(fromDate, toDate, country, toDistance)) {
        Callback.successful(req.badRequest(""))
      } else {
        implicit def tryOptionToOption[T](obj: Try[Option[T]]): Option[T] = obj.getOrElse(None)

        visitDao.userVisits(UserVisitsRequest(
          id, fromDate, toDate, country, toDistance
        )) match {
          case Some(visits) =>
            Callback.successful(req.ok(visits.toJson.compactPrint))
          case None =>
            Callback.successful(req.notFound(""))
        }
      }
    // location average
    case req@Get on Root / "locations" / Integer(id) / "avg" =>
      val params = req.head.parameters
      val fromDate = params.optParam[Int]("fromDate")
      val toDate = params.optParam[Int]("toDate")
      val fromAge = params.optParam[Int]("fromAge")
      val toAge = params.optParam[Int]("toAge")
      val gender = params.optParam[Char]("gender")
      if (anyFailed(fromDate, toDate, fromAge, toAge, gender)) {
        Callback.successful(req.badRequest(""))
      } else {
        implicit def tryOptionToOption[T](obj: Try[Option[T]]): Option[T] = obj.getOrElse(None)

        visitDao.locationAvg(LocationAvgRequest(
          id, fromDate, toDate, fromAge, toAge, gender
        )) match {
          case Some(avg) =>
            Callback.successful(req.ok(avg.toJson.compactPrint))
          case None =>
            Callback.successful(req.notFound(""))
        }
      }
    case req =>
      Callback.successful(req.notFound(""))
  }
}
