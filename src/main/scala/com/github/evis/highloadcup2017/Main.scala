package com.github.evis.highloadcup2017

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.github.evis.highloadcup2017.api.{Api, UserApi}
import com.github.evis.highloadcup2017.dao.inmemory.InMemoryUserDao

object Main extends App {
  implicit val system = ActorSystem("http-server")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val userDao = new InMemoryUserDao

  new InitialDataLoader(userDao).load("/tmp/data.zip")

  val userApi = new UserApi(userDao)
  val api = new Api(userApi)

  Http().bindAndHandle(api.route, "localhost", 8080)
}