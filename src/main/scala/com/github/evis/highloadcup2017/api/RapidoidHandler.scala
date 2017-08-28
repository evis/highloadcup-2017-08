package com.github.evis.highloadcup2017.api

import java.net.URLDecoder
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorRef
import com.github.evis.highloadcup2017.dao.{Dao, LocationDao, UserDao, VisitDao}
import com.github.evis.highloadcup2017.model.{Entity, EntityUpdate, UserVisit}
import com.typesafe.scalalogging.StrictLogging
import org.rapidoid.buffer.Buf
import org.rapidoid.bytes.BytesUtil
import org.rapidoid.bytes.BytesUtil.startsWith
import org.rapidoid.http.HttpStatus.DONE
import org.rapidoid.http.MediaType.APPLICATION_JSON
import org.rapidoid.http.impl.lowlevel.HttpIO
import org.rapidoid.http.{AbstractHttpServer, HttpStatus}
import org.rapidoid.net.abstracts.Channel
import org.rapidoid.net.impl.RapidoidHelper
import org.rapidoid.util.Constants.CR_LF
import spray.json.JsonParser.ParsingException
import spray.json._

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
    implicit val implicitCtx = ctx
    implicit val implicitHelper = helper

    val startsWithUsers = startsWith(buf.bytes(), helper.path, Users, true)
    val startsWithLocations = startsWith(buf.bytes(), helper.path, Locations, true)
    val startsWithVisits = startsWith(buf.bytes(), helper.path, Visits, true)

    //noinspection AccessorLikeMethodIsEmptyParen
    def getPath() = BytesUtil.get(buf.bytes(), helper.path)

    def doGet() =
      if (startsWithUsers) doGetUsers()
      else if (startsWithLocations) doGetLocations()
      else if (startsWithVisits) doGetVisits()
      else sendNotFound()

    def doPost() = {
      val postsAmount = posts.incrementAndGet()
      val body = BytesUtil.get(buf.bytes(), helper.body)
      val json = body.parseJson
      val result =
        if (startsWithUsers) doPostUsers(json)
        else if (startsWithLocations) doPostLocations(json)
        else if (startsWithVisits) doPostVisits(json)
        else sendNotFound()
      cleanIfPostsDone(postsAmount)
      result
    }

    def doGetUsers() = {
      val path = getPath()
      if (path.endsWith("/visits")) doGetUserVisits(path)
      else doGetEntity(userDao, Users, maxUserId, path)
    }

    def doGetLocations() = {
      val path = getPath()
      if (path.endsWith("/avg")) doGetLocationAvg(path)
      else doGetEntity(locationDao, Locations, maxLocationId, path)
    }

    def doGetVisits() = doGetEntity(visitDao, Visits, maxVisitId, getPath())

    def doGetUserVisits(path: String) = {
      val id = extractUserId(path)
      val params = extractParams
      // it's volatile!
      //noinspection ScalaUselessExpression
      maxVisitId
      if (id <= maxUserId)
        try {
          sendUserVisits(visitDao.userVisits(
            id,
            params.get("fromDate").map(_.toInt),
            params.get("toDate").map(_.toInt),
            params.get("country").map(_.toString),
            params.get("toDistance").map(_.toInt)
          ))
        } catch {
          case _: NumberFormatException => deserializationError("")
        }
      else sendNotFound()
    }

    def doGetLocationAvg(path: String) = {
      val id = extractLocationId(path)
      val params = extractParams
      // it's volatile!
      //noinspection ScalaUselessExpression
      maxVisitId
      if (id <= maxLocationId)
        try {
          sendOk(visitDao.locationAvg(
            id,
            params.get("fromDate").map(_.toInt),
            params.get("toDate").map(_.toInt),
            params.get("fromAge").map(_.toInt),
            params.get("toAge").map(_.toInt),
            params.get("gender").map(s =>
              if (s.isEmpty || s.length > 1 || (s.head != 'm' && s.head != 'f'))
                deserializationError("")
              else
                s.head
            )
          ))
        } catch {
          case _: NumberFormatException => deserializationError("")
        }
      else sendNotFound()
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
                                                               maxIdCounter: AtomicInteger) = {
      val path = getPath()
      if (path.endsWith("/new"))
        doCreateEntity(json, entityReader, maxIdCounter)
      else
        doUpdateEntity(json, updateReader, prefix, maxIdCounter, path)
    }

    def doCreateEntity[T <: Entity](json: JsValue,
                                    entityReader: JsonReader[T],
                                    maxIdCounter: AtomicInteger) = {
      val entity = entityReader.read(json)
      val result = sendOk()
      maxIdCounter.getAndIncrement()
      postActor ! entity
      result
    }

    def doUpdateEntity[U <: EntityUpdate](json: JsValue,
                                          updateReader: JsonReader[U],
                                          prefix: Array[Byte],
                                          maxIdCounter: AtomicInteger,
                                          path: String) = {
      val update = updateReader.read(json)
      val id = getEntityId(prefix, path)
      if (maxIdCounter.get() >= id) {
        val result = sendOk()
        postActor ! (id, update)
        result
      } else sendNotFound()
    }

    def doGetEntity[T <: WithFiller](dao: Dao[T], prefix: Array[Byte], maxId: Int, path: String) =
      doGetEntityImpl(dao, getEntityId(prefix, path), maxId)

    def doGetEntityImpl[T <: WithFiller](dao: Dao[T], id: Int, maxId: Int) = {
      // it's volatile!
      //noinspection ScalaUselessExpression
      maxVisitId
      if (id <= maxId)
        sendByteOk(dao.read(id))
      else
        sendNotFound()
    }

    def sendOk(body: Array[Byte] = okBody) =
      ok(ctx, helper.isKeepAlive.value, body, APPLICATION_JSON)

    def sendByteOk[T <: WithFiller](obj: T) = {
      val buffer = threadBuffer.get()
      obj.fill(buffer)
      val length = buffer.position()
      sendOkWithLength(buffer.array(), length)
    }

    def sendOkWithLength(body: Array[Byte], length: Int) = {
      startResponse(ctx, helper.isKeepAlive.value)
      ctx.write(CONTENT_TYPE_TXT)
      ctx.write(APPLICATION_JSON.getBytes)
      ctx.write(CR_LF)
      HttpIO.INSTANCE.writeContentLengthHeader(ctx, length)
      ctx.write(CR_LF)
      ctx.write(body, 0, length)
      DONE
    }

    def sendUserVisits(visits: Iterable[UserVisit]) = {
      val buffer = threadBuffer.get()
      buffer.position(0)
      buffer.put(jsonVisitsPrefix)
      if (visits.nonEmpty) {
        visits.head.fill(buffer)
        visits.tail.foreach { visit =>
          buffer.put(comma)
          visit.fill(buffer)
        }
      }
      buffer.put(jsonVisitsPostfix)
      sendOkWithLength(buffer.array(), buffer.position())
    }

    def sendNotFound() =
      send(404)

    def sendBadRequest() =
      send(400)

    def send(code: Int) = {
      startResponse(ctx, code, helper.isKeepAlive.value)
      HttpIO.INSTANCE.writeContentLengthHeader(ctx, 0)
      ctx.write(CR_LF)
      DONE
    }

    def getEntityId(prefix: Array[Byte], path: String) = path.substring(prefix.length).toInt

    def extractUserId(path: String) = path match {
      case extractUserIdPattern(idStr) => idStr.toInt
      case _ => -1
    }

    def extractLocationId(path: String) = path match {
      case extractLocationIdPattern(idStr) => idStr.toInt
      case _ => -1
    }

    def extractParams = {
      var keyBuilder = new StringBuilder
      var valueBuilder = new StringBuilder
      var beforeQM = true
      var beforeEQ = true
      val uri = URLDecoder.decode(BytesUtil.get(buf.bytes(), helper.uri), "UTF-8")
      val init = uri.foldLeft(Map.empty[String, String]) { (acc, c) =>
        if (beforeQM) {
          if (c == '?') {
            beforeQM = false
            acc
          } else {
            acc
          }
        } else {
          if (beforeEQ) {
            if (c == '=') {
              beforeEQ = false
              acc
            } else {
              keyBuilder.append(c)
              acc
            }
          } else {
            if (c == '&') {
              beforeEQ = true
              val key = keyBuilder.toString()
              val value = valueBuilder.toString()
              keyBuilder = new StringBuilder
              valueBuilder = new StringBuilder
              acc + (key -> value)
            } else {
              valueBuilder.append(c)
              acc
            }
          }
        }
      }
      if (keyBuilder.nonEmpty && valueBuilder.nonEmpty)
        init + (keyBuilder.toString() -> valueBuilder.toString())
      else
        init
    }

    try {
      if (helper.isGet.value) doGet()
      else if (matches(buf, helper.verb, Post)) doPost()
      else sendNotFound()
    } catch {
      case _: DeserializationException => sendBadRequest()
      case _: ParsingException => sendBadRequest()
      case _: NumberFormatException => sendNotFound()
    }
  }

  private val Users = "/users/".getBytes
  private val Locations = "/locations/".getBytes
  private val Visits = "/visits/".getBytes

  private val okBody = "{}".getBytes

  private val jsonVisitsPrefix = """{"visits":[""".getBytes
  private val jsonVisitsPostfix = "]}".getBytes
  private val comma = ','.toByte

  private val Post = "POST".getBytes

  private val extractUserIdPattern = "/users/(\\d+)/visits".r
  private val extractLocationIdPattern = "/locations/(\\d+)/avg".r

  private def cleanIfPostsDone(postsAmount: Int) = {
    if (postsAmount == maxPostsAmount) {
      logger.debug("End of phase 2")
      maxUserId = maxUserIdCounter.get()
      maxLocationId = maxLocationIdCounter.get()
      maxVisitId = maxVisitIdCounter.get()
      System.gc()
    }
  }

  private val maxUserIdCounter = new AtomicInteger(initMaxUserId)
  private val maxLocationIdCounter = new AtomicInteger(initMaxLocationId)
  private val maxVisitIdCounter = new AtomicInteger(initMaxVisitId)

  private var maxUserId = initMaxUserId
  private var maxLocationId = initMaxLocationId
  // it's volatile, so, we will read it every time we want to read maxUserId or maxLocationId
  @volatile private var maxVisitId = initMaxVisitId

  private val posts = new AtomicInteger()

  private val maxPostsAmount = if (isRateRun) 40000 else 3000

  private val threadBuffer = new ThreadLocal[ByteBuffer]() {
    override def initialValue(): ByteBuffer = ByteBuffer.allocate(65536)
  }
}
