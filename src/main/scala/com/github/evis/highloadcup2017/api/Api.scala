package com.github.evis.highloadcup2017.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

class Api(userApi: UserApi, locationApi: LocationApi, visitApi: VisitApi) {
  def route: Route = rejectEmptyResponse {
    userApi.route ~ locationApi.route ~ visitApi.route
  }
}
