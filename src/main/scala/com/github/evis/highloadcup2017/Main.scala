package com.github.evis.highloadcup2017

import java.time.Instant

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import better.files.File
import com.github.evis.highloadcup2017.api.{Api, UserApi}
import com.github.evis.highloadcup2017.dao.inmemory.InMemoryUserDao

object Main extends App {
  if (args.length < 1)
    throw new Exception("Usage: Main port")

  val port = args(0).toInt
  implicit val system = ActorSystem("http-server")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  private val generationInstant = Instant.ofEpochSecond(
    File("/tmp/data/options.txt").lineIterator.next().toInt)
  val userDao = new InMemoryUserDao(generationInstant)

  new InitialDataLoader(userDao).load("/tmp/data/data.zip")

  val userApi = new UserApi(userDao)
  val api = new Api(userApi)

  Http().bindAndHandle(api.route, "localhost", port)
}
