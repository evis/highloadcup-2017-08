package com.github.evis.highloadcup2017.api

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorRef, PoisonPill}
import com.github.evis.highloadcup2017.dao.{Dao, LocationDao, UserDao, VisitDao}
import com.github.evis.highloadcup2017.model.{Entity, EntityUpdate}
import com.typesafe.scalalogging.StrictLogging
import org.rapidoid.buffer.Buf
import org.rapidoid.bytes.BytesUtil
import org.rapidoid.bytes.BytesUtil.startsWith
import org.rapidoid.http.HttpStatus.ASYNC
import org.rapidoid.http.MediaType.APPLICATION_JSON
import org.rapidoid.http.impl.lowlevel.HttpIO
import org.rapidoid.http.{AbstractHttpServer, HttpStatus}
import org.rapidoid.net.abstracts.Channel
import org.rapidoid.net.impl.RapidoidHelper
import spray.json.JsonParser.ParsingException
import spray.json._

import scala.annotation.tailrec
import scala.util.control.NonFatal

class RapidoidHandler(userDao: UserDao,
                      locationDao: LocationDao,
                      visitDao: VisitDao,
                      postActor: ActorRef,
                      initMaxUserId: Int,
                      initMaxLocationId: Int,
                      initMaxVisitId: Int,
                      isRateRun: Boolean)
  extends AbstractHttpServer("Rapidoid", "", "", true) with StrictLogging with JsonFormats {

  override def handle(ctx: Channel, buf: Buf, helper: RapidoidHelper): HttpStatus = {
    val startsWithUsers = startsWith(buf.bytes(), helper.path, Users, true)
    val startsWithLocations = startsWith(buf.bytes(), helper.path, Locations, true)
    val startsWithVisits = startsWith(buf.bytes(), helper.path, Visits, true)

    val path = BytesUtil.get(buf.bytes(), helper.path)
    val body = BytesUtil.get(buf.bytes(), helper.body)

    def doGet() =
      if (startsWithUsers) doGetUsers()
      else if (startsWithLocations) doGetLocations()
      else if (startsWithVisits) doGetVisits()
      else sendNotFound()

    def doPost() = {
      try {
        val json = body.parseJson
        posts.getAndIncrement()
        val result =
          if (startsWithUsers) doPostUsers(json)
          else if (startsWithLocations) doPostLocations(json)
          else if (startsWithVisits) doPostVisits(json)
          else sendNotFound()
        cleanIfPostsDone()
        result
      } catch {
        case _: DeserializationException => sendBadRequest()
        case _: ParsingException => sendBadRequest()
      }
    }

    def doGetUsers() =
      if (path.endsWith("/visits")) doGetUserVisits()
      else doGetEntity(userDao, Users, maxUserId)

    def doGetLocations() =
      if (path.endsWith("/avg")) doGetLocationAvg()
      else doGetEntity(locationDao, Locations, maxLocationId)

    def doGetVisits() = doGetEntity(visitDao, Visits, maxVisitId)

    def doGetUserVisits() = {
      try {
        val id = extractUserId
        if (id == -1)
          sendNotFound()
        else {
          sendBadRequest() // TODO
        }
      } catch {
        case NonFatal(_) => sendNotFound()
      }
    }

    def doGetLocationAvg() = {
      sendBadRequest() // TODO
    }

    def doPostUsers(json: JsValue) =
      doCreateOrUpdateEntity(json, userFormat, userUpdateReader, Users, maxUserIdCounter)

    def doPostLocations(json: JsValue) =
      doCreateOrUpdateEntity(json, locationFormat, locationUpdateReader, Locations, maxLocationIdCounter)

    def doPostVisits(json: JsValue) =
      doCreateOrUpdateEntity(json, visitFormat, visitUpdateReader, Visits, maxVisitIdCounter)

    def doCreateOrUpdateEntity[E <: Entity, U <: EntityUpdate](json: JsValue,
                                                               entityReader: JsonReader[E],
                                                               updateReader: JsonReader[U],
                                                               prefix: Array[Byte],
                                                               maxIdCounter: AtomicInteger) =
      if (path.endsWith("/new"))
        doCreateEntity(json, entityReader, maxIdCounter)
      else
        doUpdateEntity(json, updateReader, prefix, maxIdCounter)

    def doCreateEntity[T <: Entity](json: JsValue,
                                    entityReader: JsonReader[T],
                                    maxIdCounter: AtomicInteger) = {
      val user = entityReader.read(json)
      tryUpdateMax(maxUserIdCounter, user.id)
      postActor ! user
      cleanIfPostsDone()
      sendOk()
    }

    def doUpdateEntity[U <: EntityUpdate](json: JsValue,
                                          updateReader: JsonReader[U],
                                          prefix: Array[Byte],
                                          maxIdCounter: AtomicInteger) = {
      val update = updateReader.read(json)
      try {
        val id = getEntityId(prefix)
        if (maxUserIdCounter.get() >= id) {
          postActor ! (id, update)
          sendOk()
        } else sendNotFound()
      } catch {
        case _: NumberFormatException => sendNotFound()
      }
    }

    def doGetEntity(dao: Dao, prefix: Array[Byte], maxId: Int) =
      try doGetEntityImpl(dao, getEntityId(prefix), maxId)
      catch { case NonFatal(_) => sendNotFound() }

    def doGetEntityImpl(dao: Dao, id: Int, maxId: Int) = {
      // it's volatile!
      //noinspection ScalaUselessExpression
      maxVisitId
      if (id <= maxId)
        ok(ctx, false, dao.json(id), APPLICATION_JSON)
      else
        sendNotFound()
    }

    def sendOk() = ok(ctx, false, okBody, APPLICATION_JSON)

    def sendNotFound() = {
      startResponse(ctx, 404, false)
      HttpIO.INSTANCE.writeContentLengthHeader(ctx, 0)
      ctx.close()
      ASYNC
    }

    def sendBadRequest() = {
      startResponse(ctx, 400, false)
      HttpIO.INSTANCE.writeContentLengthHeader(ctx, 0)
      ctx.close()
      ASYNC
    }

    def getEntityId(prefix: Array[Byte]) = path.substring(prefix.length).toInt

    def extractUserId = path match {
      case extractUserIdPattern(idStr) => idStr.toInt
      case _ => -1
    }

    def extractLocationId = path match {
      case extractLocationIdPattern(idStr) => idStr.toInt
      case _ => -1
    }

    if (helper.isGet.value) doGet()
    else if (matches(buf, helper.verb, Post)) doPost()
    else sendNotFound()
  }

  private val Users = "/users/".getBytes
  private val Locations = "/locations/".getBytes
  private val Visits = "/visits/".getBytes

  private val okBody = "{}".getBytes
  private val emptyBody = Array.emptyByteArray
  private val badRequest = fullResp(400, emptyBody)

  private val Post = "POST".getBytes

  private val extractUserIdPattern = "/users/(\\d+)/visits".r
  private val extractLocationIdPattern = "/locations/(\\d+)/avg".r

  private def cleanIfPostsDone() = {
    if (posts.get() == maxPostsAmount) {
      logger.debug("End of phase 2")
      postActor ! PoisonPill
      maxUserId = maxUserIdCounter.get()
      maxLocationId = maxLocationIdCounter.get()
      maxVisitId = maxVisitIdCounter.get()
    }
  }

  private def tryUpdateMax(max: AtomicInteger, newMax: Int) {
    @tailrec
    def cycle(): Unit = {
      val prev = max.get()
      if (newMax > prev && !max.compareAndSet(prev, newMax))
        cycle()
    }

    cycle()
  }

  private val maxUserIdCounter = new AtomicInteger(initMaxUserId)
  private val maxLocationIdCounter = new AtomicInteger(initMaxLocationId)
  private val maxVisitIdCounter = new AtomicInteger(initMaxVisitId)

  private var maxUserId = initMaxUserId
  private var maxLocationId = initMaxLocationId
  // it's volatile, so, we will read it every time we want to read maxUserId or maxLocationId
  @volatile private var maxVisitId = initMaxVisitId

  private val posts = new AtomicInteger()

  private val maxPostsAmount = if (isRateRun) 12000 else 1000
}
