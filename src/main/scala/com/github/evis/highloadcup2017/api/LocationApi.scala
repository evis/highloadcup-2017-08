package com.github.evis.highloadcup2017.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.evis.highloadcup2017.dao.LocationDao
import com.github.evis.highloadcup2017.model.{Location, LocationUpdate}

class LocationApi(locationDao: LocationDao) extends ApiMarshallers {
  val route: Route =
    pathPrefix("locations") {
      path("new") {
        entity(as[Location]) { location =>
          complete(locationDao.create(location))
        }
      } ~ path(IntNumber) { id =>
        get {
          complete(locationDao.read(id))
        } ~ post {
          entity(as[LocationUpdate]) { update =>
            complete(locationDao.update(id, update))
          }
        }
      }
    }
}
