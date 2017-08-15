package com.github.evis.highloadcup2017.api

import akka.http.scaladsl.server.Route

import scala.concurrent.ExecutionContext

class Api(userApi: UserApi) {
  def route(implicit ec: ExecutionContext): Route = userApi.route
}
