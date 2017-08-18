package com.github.evis.highloadcup2017.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.concurrent.ExecutionContext

class Api(userApi: UserApi, locationApi: LocationApi, visitApi: VisitApi) {
  def route(implicit ec: ExecutionContext): Route = rejectEmptyResponse {
    userApi.route ~ locationApi.route ~ visitApi.route
  }
}
