package com.github.evis.highloadcup2017.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.evis.highloadcup2017.dao.VisitDao
import com.github.evis.highloadcup2017.model.{Visit, VisitUpdate}

class VisitApi(visitDao: VisitDao) extends ApiBase {
  val route: Route =
    pathPrefix("visits") {
      path("new") {
        entity(as[Visit]) { visit =>
          complete(visitDao.create(visit))
        }
      } ~ path(IntNumber) { id =>
        get {
          complete(visitDao.read(id))
        } ~ post {
          entity(as[VisitUpdate]) { update =>
            complete(visitDao.update(id, update))
          }
        }
      }
    }
}
