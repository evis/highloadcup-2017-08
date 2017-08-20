package com.github.evis.highloadcup2017.api

import java.time.Instant

import colossus.protocols.http.Http
import colossus.protocols.http.HttpMethod.{Get, Post}
import colossus.protocols.http.UrlParsing.{Root, _}
import colossus.service.Callback
import colossus.service.GenRequestHandler.PartialHandler
import com.github.evis.highloadcup2017.dao.{LocationDao, UserDao, VisitDao}
import com.github.evis.highloadcup2017.model.{Location, LocationAvgRequest, LocationUpdate, User, UserUpdate, UserVisitsRequest, Visit, VisitUpdate}
import spray.json._

import scala.util.{Failure, Success}

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
          Callback.successful(req.notFound(""))
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
      val fromDate = params.getFirstAs[Instant]("fromDate")
      val toDate = params.getFirstAs[Instant]("toDate")
      val country = params.getFirst("country")
      val distance = params.getFirstAs[Int]("distance")
      fromDate.flatMap(_ => toDate).flatMap(_ => distance) match {
        case Success(_) =>
          visitDao.userVisits(UserVisitsRequest(
            id, fromDate.toOption, toDate.toOption, country, distance.toOption
          )) match {
            case Some(visits) =>
              Callback.successful(req.ok(visits.toJson.compactPrint))
            case None =>
              Callback.successful(req.notFound(""))
          }
        case Failure(_) =>
          Callback.successful(req.badRequest(""))
      }
    // location average
    case req@Get on Root / "locations" / Integer(id) / "avg" =>
      val params = req.head.parameters
      val fromDate = params.getFirstAs[Instant]("fromDate")
      val toDate = params.getFirstAs[Instant]("toDate")
      val fromAge = params.getFirstAs[Int]("fromAge")
      val toAge = params.getFirstAs[Int]("toAge")
      val gender = params.getFirstAs[Char]("gender")
      fromDate.flatMap(_ => toDate).flatMap(_ => fromAge).flatMap(_ => toAge)
        .flatMap(_ => gender) match {

        case Success(_) =>
          visitDao.locationAvg(LocationAvgRequest(
            id, fromDate.toOption, toDate.toOption, fromAge.toOption, toAge.toOption, gender.toOption
          )) match {
            case Some(avg) =>
              Callback.successful(req.ok(avg.toJson.compactPrint))
            case None =>
              Callback.successful(req.notFound(""))
          }
        case Failure(_) =>
          Callback.successful(req.badRequest(""))
      }
  }
}
