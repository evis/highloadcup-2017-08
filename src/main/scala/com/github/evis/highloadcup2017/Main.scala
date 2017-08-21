package com.github.evis.highloadcup2017

import java.nio.file.NoSuchFileException
import java.time.{LocalDateTime, ZoneOffset}

import akka.actor.{ActorSystem, Props}
import better.files.File
import colossus.IOSystem
import colossus.core.ServerContext
import colossus.protocols.http
import colossus.protocols.http.server.{HttpServer, Initializer, RequestHandler}
import colossus.service.GenRequestHandler.PartialHandler
import com.github.evis.highloadcup2017.api._
import com.github.evis.highloadcup2017.dao.{LocationDao, UserDao, VisitDao}
import com.typesafe.scalalogging.StrictLogging

object Main extends App with StrictLogging {
  if (args.length < 1)
    throw new Exception("Usage: Main port")

  val port = args(0).toInt
  implicit val system = ActorSystem("http-server")

  val (generationDateTime, isRateRun) = try {
    val optionLines = File("/tmp/data/options.txt").lineIterator
    val generationTime = optionLines.next().toInt
    val isRateRun = optionLines.next() == "1"
    logger.info(s"Generation time is $generationTime")
    logger.info(s"RateRun is $isRateRun")
    (
      LocalDateTime.ofEpochSecond(
        generationTime, 0, ZoneOffset.UTC),
      isRateRun
    )
  } catch {
    case _: NoSuchFileException =>
      logger.warn("Options file not found! Default to (datetime.now(), true)")
      (LocalDateTime.now(ZoneOffset.UTC), true)
  }

  val userDao = new UserDao
  val locationDao = new LocationDao
  val visitDao = new VisitDao(userDao, locationDao, generationDateTime)
  userDao.setVisitDao(visitDao)
  locationDao.setVisitDao(visitDao)

  val postActor = system.actorOf(Props(new PostActor(userDao, locationDao, visitDao)), "post-actor")
  val (maxUserId, maxLocationId, maxVisitId) =
    new InitialDataLoader(userDao, locationDao, visitDao).load("/tmp/data/data.zip")

  implicit val ioSystem = IOSystem()

  val httpHandle = new ColossusHandler(
    userDao, locationDao, visitDao, postActor, maxUserId, maxLocationId, maxVisitId, isRateRun
  ).httpHandle

  HttpServer.start("server", port) {
    new Initializer(_) {
      override def onConnect: (ServerContext) => RequestHandler =
        new RequestHandler(_) {
          override protected def handle: PartialHandler[http.Http] = {
            httpHandle
          }
        }
    }
  }
}
