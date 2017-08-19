package com.github.evis.highloadcup2017.api

import java.time.Instant

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.evis.highloadcup2017.dao.{LocationDao, VisitDao}
import com.github.evis.highloadcup2017.model._

class LocationApi(locationDao: LocationDao, visitDao: VisitDao) extends ApiBase {
  val route: Route =
    pathPrefix("locations") {
      path("new") {
        entity(as[Location]) { location =>
          complete(locationDao.create(location))
        }
      } ~ pathPrefix(IntNumber) { id =>
        pathEndOrSingleSlash {
          get {
            complete(locationDao.read(id))
          } ~ post {
            entity(as[LocationUpdate]) { update =>
              complete(locationDao.update(id, update))
            }
          }
        } ~ path("avg") {
          parameters("fromDate".as[Instant].?, "toDate".as[Instant].?,
            "fromAge".as[Int].?, "toAge".as[Int].?, "gender".?) {
            (fromDate, toDate, fromAge, toAge, gender) =>
              complete {
                visitDao.locationAvg(LocationAvgRequest(
                  location = id,
                  fromDate,
                  toDate,
                  fromAge,
                  toAge,
                  // normally this mapping should be in parameter
                  gender.map(GenderEnum.withName)
                ))
              }
          }
        }
      }
    }
}
