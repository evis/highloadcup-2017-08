package com.github.evis.highloadcup2017.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.evis.highloadcup2017.dao.UserDao
import com.github.evis.highloadcup2017.model._

class UserApi(userDao: UserDao) extends ApiMarshallers {
  val route: Route =
    pathPrefix("users") {
      path("new") {
        entity(as[User]) { user =>
          complete(userDao.create(user))
        }
      } ~ path(IntNumber) { id =>
        get {
          complete(userDao.read(id))
        } ~ post {
          entity(as[UserUpdate]) { update =>
            complete(userDao.update(id, update))
          }
        }
      }
    }
}
