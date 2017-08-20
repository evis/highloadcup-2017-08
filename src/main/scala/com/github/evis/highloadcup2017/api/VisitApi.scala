package com.github.evis.highloadcup2017.api

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.evis.highloadcup2017.dao.VisitDao
import com.github.evis.highloadcup2017.model.{Visit, VisitUpdate}

class VisitApi(visitDao: VisitDao, postActor: ActorRef) extends ApiBase {
  val route: Route =
    pathPrefix("visits") {
      path("new") {
        entity(as[Visit]) { visit =>
          postActor ! visit
          complete("{}")
        }
      } ~ path(IntNumber) { id =>
        get {
          complete(visitDao.read(id))
        } ~ post {
          entity(as[VisitUpdate]) { update =>
            val result = visitDao.read(id).map(_ => "{}")
            result.foreach(_ => postActor ! (id, update))
            complete(result)
          }
        }
      }
    }
}
