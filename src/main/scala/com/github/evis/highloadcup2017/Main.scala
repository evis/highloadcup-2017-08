package com.github.evis.highloadcup2017

import java.nio.file.NoSuchFileException
import java.time.Instant

import akka.actor.{ActorSystem, Props}
import better.files.File
import colossus.IOSystem
import colossus.core.ServerContext
import colossus.protocols.http
import colossus.protocols.http.server.{HttpServer, Initializer, RequestHandler}
import colossus.service.GenRequestHandler.PartialHandler
import com.github.evis.highloadcup2017.api._
import com.github.evis.highloadcup2017.dao.{LocationDao, UserDao, VisitDao}

object Main extends App {
  if (args.length < 1)
    throw new Exception("Usage: Main port")

  val port = args(0).toInt
  implicit val system = ActorSystem("http-server")

  val generationInstant = try {
    Instant.ofEpochSecond(
      File("/tmp/data/options.txt").lineIterator.next().toInt)
  } catch {
    case _: NoSuchFileException => Instant.now()
  }

  val userDao = new UserDao(generationInstant)
  val locationDao = new LocationDao(generationInstant)
  val visitDao = new VisitDao(userDao, locationDao, generationInstant)
  userDao.setVisitDao(visitDao)
  locationDao.setVisitDao(visitDao)

  val postActor = system.actorOf(Props(new PostActor(userDao, locationDao, visitDao)), "post-actor")
  new InitialDataLoader(userDao, locationDao, visitDao).load("/tmp/data/data.zip")

  implicit val ioSystem = IOSystem()

  val httpHandle = new ColossusHandler(userDao, locationDao, visitDao).httpHandle

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
