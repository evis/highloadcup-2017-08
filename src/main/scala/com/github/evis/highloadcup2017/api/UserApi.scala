package com.github.evis.highloadcup2017.api

import java.time.Instant

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.evis.highloadcup2017.dao.{UserDao, VisitDao}
import com.github.evis.highloadcup2017.model._

class UserApi(userDao: UserDao, visitDao: VisitDao, postActor: ActorRef) extends ApiBase {
  val route: Route =
    pathPrefix("users") {
      path("new") {
        entity(as[User]) { user =>
          postActor ! user
          complete("{}")
        }
      } ~ path(IntNumber) { id =>
        get {
          complete(userDao.read(id))
        } ~ post {
          entity(as[UserUpdate]) { update =>
            val result = userDao.read(id).map(_ => "{}")
            result.foreach(_ => postActor ! (id, update))
            complete(result)
          }
        }
      } ~ path(IntNumber / "visits") { id =>
        parameters("fromDate".as[Instant].?, "toDate".as[Instant].?, "country".?, "toDistance".as[Int].?) {
          (fromDate, toDate, country, toDistance) =>
            complete {
              visitDao.userVisits(UserVisitsRequest(
                user = id,
                fromDate,
                toDate,
                country,
                toDistance,
              ))
            }
        }
      }
    }
}
