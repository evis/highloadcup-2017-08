package com.github.evis.highloadcup2017.api

import java.time.Instant

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.evis.highloadcup2017.dao.{LocationDao, VisitDao}
import com.github.evis.highloadcup2017.model._

class LocationApi(locationDao: LocationDao, visitDao: VisitDao, postActor: ActorRef) extends ApiBase {
  val route: Route =
    pathPrefix("locations") {
      path("new") {
        entity(as[Location]) { location =>
          postActor ! location
          complete("{}")
        }
      } ~ pathPrefix(IntNumber) { id =>
        pathEndOrSingleSlash {
          get {
            complete(locationDao.read(id))
          } ~ post {
            entity(as[LocationUpdate]) { update =>
              val result = locationDao.read(id).map(_ => "{}")
              result.foreach(_ => postActor ! (id, update))
              complete(result)
            }
          }
        } ~ path("avg") {
          parameters("fromDate".as[Instant].?, "toDate".as[Instant].?,
            "fromAge".as[Int].?, "toAge".as[Int].?, "gender".as[Char].?) {
            (fromDate, toDate, fromAge, toAge, gender) =>
              complete {
                visitDao.locationAvg(LocationAvgRequest(
                  location = id,
                  fromDate,
                  toDate,
                  fromAge,
                  toAge,
                  gender
                ))
              }
          }
        }
      }
    }
}
