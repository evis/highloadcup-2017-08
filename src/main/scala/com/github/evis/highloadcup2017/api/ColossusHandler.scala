package com.github.evis.highloadcup2017.api

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorRef, PoisonPill}
import colossus.protocols.http.Http
import colossus.protocols.http.HttpMethod.{Get, Post}
import colossus.protocols.http.UrlParsing.{Root, _}
import colossus.service.Callback
import colossus.service.GenRequestHandler.PartialHandler
import com.github.evis.highloadcup2017.dao.{LocationDao, UserDao, VisitDao}
import com.github.evis.highloadcup2017.model.{Location, LocationAvgRequest, LocationUpdate, User, UserUpdate, UserVisitsRequest, Visit, VisitUpdate}
import com.typesafe.scalalogging.StrictLogging
import spray.json._

import scala.annotation.tailrec
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

class ColossusHandler(userDao: UserDao,
                      locationDao: LocationDao,
                      visitDao: VisitDao,
                      postActor: ActorRef,
                      initMaxUserId: Int,
                      initMaxLocationId: Int,
                      initMaxVisitId: Int,
                      isRateRun: Boolean) extends Decoders with Encoders with Parsers with StrictLogging {

  def httpHandle: PartialHandler[Http] = {
    // creates
    case req@Post on Root / "users" / "new" =>
      posts.getAndIncrement()
      req.body.as[User] match {
        case Success(user) =>
          tryUpdateMax(maxUserIdCounter, user.id)
          maxUserIdCounter.getAndIncrement()
          postActor ! user
          cleanIfPostsDone()
          Callback.successful(req.ok("{}"))
        case Failure(_) =>
          Callback.successful(req.badRequest(""))
      }
    case req@Post on Root / "locations" / "new" =>
      posts.getAndIncrement()
      req.body.as[Location] match {
        case Success(location) =>
          tryUpdateMax(maxLocationIdCounter, location.id)
          maxLocationIdCounter.getAndIncrement()
          postActor ! location
          cleanIfPostsDone()
          Callback.successful(req.ok("{}"))
        case Failure(_) =>
          cleanIfPostsDone()
          Callback.successful(req.badRequest(""))
      }
    case req@Post on Root / "visits" / "new" =>
      posts.getAndIncrement()
      req.body.as[Visit] match {
        case Success(visit) =>
          tryUpdateMax(maxVisitIdCounter, visit.id)
          maxVisitIdCounter.getAndIncrement()
          postActor ! visit
          cleanIfPostsDone()
          Callback.successful(req.ok("{}"))
        case Failure(_) =>
          cleanIfPostsDone()
          Callback.successful(req.badRequest(""))
      }
    // updates
    case req@Post on Root / "users" / Integer(id) =>
      posts.getAndIncrement()
      req.body.as[UserUpdate] match {
        case Success(update) =>
          if (maxUserIdCounter.get() >= id) {
            postActor ! (id, update)
            cleanIfPostsDone()
            Callback.successful(req.ok("{}"))
          } else {
            cleanIfPostsDone()
            Callback.successful(req.notFound(""))
          }
        case Failure(_) =>
          cleanIfPostsDone()
          Callback.successful(req.badRequest(""))
      }
    case req@Post on Root / "locations" / Integer(id) =>
      posts.getAndIncrement()
      req.body.as[LocationUpdate] match {
        case Success(update) =>
          if (maxLocationIdCounter.get() >= id) {
            postActor ! (id, update)
            cleanIfPostsDone()
            Callback.successful(req.ok("{}"))
          } else {
            cleanIfPostsDone()
            Callback.successful(req.notFound(""))
          }
        case Failure(_) =>
          cleanIfPostsDone()
          Callback.successful(req.badRequest(""))
      }
    case req@Post on Root / "visits" / Integer(id) =>
      posts.getAndIncrement()
      req.body.as[VisitUpdate] match {
        case Success(update) =>
          if (maxVisitIdCounter.get() >= id) {
            postActor ! (id, update)
            cleanIfPostsDone()
            Callback.successful(req.ok("{}"))
          } else {
            cleanIfPostsDone()
            Callback.successful(req.notFound(""))
          }
        case Failure(_) =>
          cleanIfPostsDone()
          Callback.successful(req.badRequest(""))
      }
    // reads
    case req@Get on Root / "users" / Integer(id) =>
      // it's volatile!
      //noinspection ScalaUselessExpression
      maxVisitId
      if (id <= maxUserId)
        Callback.successful(req.ok(userDao.json(id)))
      else
        Callback.successful(req.notFound(""))
    case req@Get on Root / "locations" / Integer(id) =>
      // it's volatile!
      //noinspection ScalaUselessExpression
      maxVisitId
      if (id <= maxLocationId)
        Callback.successful(req.ok(locationDao.json(id)))
      else
        Callback.successful(req.notFound(""))
    case req@Get on Root / "visits" / Integer(id) =>
      if (id <= maxVisitId)
        Callback.successful(req.ok(visitDao.json(id)))
      else
        Callback.successful(req.notFound(""))
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

  private def cleanIfPostsDone() = {
    if (posts.get() == maxPostsAmount) {
      logger.debug("End of phase 2")
      postActor ! PoisonPill
      maxUserId = maxUserIdCounter.get()
      maxLocationId = maxLocationIdCounter.get()
      maxVisitId = maxVisitIdCounter.get()
    }
  }

  private def tryUpdateMax(max: AtomicInteger, newMax: Int) {
    @tailrec
    def cycle(): Unit = {
      val prev = max.get()
      if (newMax > prev && !max.compareAndSet(prev, newMax))
        cycle()
    }

    cycle()
  }

  private val maxUserIdCounter = new AtomicInteger(initMaxUserId)
  private val maxLocationIdCounter = new AtomicInteger(initMaxLocationId)
  private val maxVisitIdCounter = new AtomicInteger(initMaxVisitId)

  private var maxUserId = initMaxUserId
  private var maxLocationId = initMaxLocationId
  // it's volatile, so, we will read it every time we want to read maxUserId or maxLocationId
  @volatile private var maxVisitId = initMaxVisitId

  private val posts = new AtomicInteger()

  private val maxPostsAmount = if (isRateRun) 120000 else 1000
}
