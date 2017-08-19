package com.github.evis.highloadcup2017

import java.nio.file.NoSuchFileException
import java.time.Instant

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import better.files.File
import com.github.evis.highloadcup2017.api.{Api, LocationApi, UserApi, VisitApi}
import com.github.evis.highloadcup2017.dao.{LocationDao, UserDao, VisitDao}

object Main extends App {
  if (args.length < 1)
    throw new Exception("Usage: Main port")

  val port = args(0).toInt
  implicit val system = ActorSystem("http-server")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val generationInstant = try {
    Instant.ofEpochSecond(
      File("/tmp/data/options.txt").lineIterator.next().toInt)
  } catch {
    case _: NoSuchFileException => Instant.now()
  }

  val userDao = new UserDao(generationInstant)
  val locationDao = new LocationDao(generationInstant)
  val visitDao = new VisitDao(locationDao, generationInstant)

  new InitialDataLoader(userDao, locationDao, visitDao).load("/tmp/data/data.zip")

  val userApi = new UserApi(userDao, visitDao)
  val locationApi = new LocationApi(locationDao)
  val visitApi = new VisitApi(visitDao)
  val api = new Api(userApi, locationApi, visitApi)

  Http().bindAndHandle(api.route, "0.0.0.0", port)
}
