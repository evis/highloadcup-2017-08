package com.github.evis.highloadcup2017.api

import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import com.github.evis.highloadcup2017.dao.NotFoundException

import scala.concurrent.ExecutionContext

class Api(userApi: UserApi) {
  def route(implicit ec: ExecutionContext): Route = handleExceptions(exceptionHandler) {
    userApi.route
  }

  val exceptionHandler = ExceptionHandler {
    case _: NotFoundException =>
      complete(NotFound)
  }
}
