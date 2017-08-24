package com.github.evis.highloadcup2017.dao

import java.nio.ByteBuffer
import java.time._
import java.time.temporal.ChronoUnit.YEARS

import com.github.evis.highloadcup2017.api.JsonFormats
import com.github.evis.highloadcup2017.model._
import com.typesafe.scalalogging.StrictLogging
import spray.json._

import scala.collection.mutable
import scala.math.BigDecimal.RoundingMode.HALF_UP

class VisitDao(userDao: UserDao,
               locationDao: LocationDao,
               generationDateTime: LocalDateTime) extends JsonFormats with StrictLogging with Dao {
  private val visits = mutable.HashMap[Int, Visit]()

  // is it ok to hardcode size this way?
  private val jsons = Array.fill[Array[Byte]](1200000)(null)

  private implicit val keyOrdering = Ordering.by(Key.unapply)

  private val userVisits = mutable.TreeMap.apply[Key, UserVisit]()
  private val locationVisits = mutable.TreeMap.empty[Key, LocationVisit]
  // helper indexes for faster update
  private val userLocations = mutable.HashMap[Int, mutable.Set[Int]]()
  private val locationUsers = mutable.HashMap[Int, mutable.Set[Int]]()

  def create(visit: Visit): Unit = {
    // should put visit if location or user not found?
    visits += visit.id -> visit
    jsons.update(visit.id, visit.toJson.compactPrint.getBytes)
    locationDao.read(visit.location) match {
      case Some(location) =>
        userLocations.getOrElseUpdate(visit.user, mutable.Set()) += visit.location
        locationUsers.getOrElseUpdate(visit.location, mutable.Set()) += visit.user
        userVisits.put(Key(visit.user, visit.visitedAt, visit.id), UserVisit(
          visit.id,
          visit.location,
          visit.mark,
          visit.visitedAt,
          location.place,
          location.country,
          location.distance,
          UserVisit.genJson(visit.mark, visit.visitedAt, location.place)
        ))
      case None =>
        // handle it with exception handler?
        throw new Exception(s"Location ${visit.location} not found")
    }
    userDao.read(visit.user).foreach { user =>
      locationVisits.put(Key(visit.location, visit.visitedAt, visit.id), LocationVisit(
        visit.user,
        visit.id,
        visit.mark,
        visit.visitedAt,
        // switch to datetimes everywhere?
        LocalDateTime.ofEpochSecond(user.birthDate, 0, ZoneOffset.UTC).until(generationDateTime, YEARS).toInt,
        user.gender
      ))
    }
  }

  def read(id: Int): Option[Visit] = visits.get(id)

  def json(id: Int): Array[Byte] = jsons(id)

  //noinspection UnitInMap
  def update(id: Int, update: VisitUpdate): Option[Unit] =
    read(id).map { visit =>
      val updated = visit `with` update
      visits.put(id, updated)
      jsons.update(id, updated.toJson.compactPrint.getBytes)

      // can not always update it
      userLocations(visit.user) -= visit.location
      locationUsers(visit.location) -= visit.user
      val newUser = update.user.getOrElse(visit.user)
      val newLocation = update.location.getOrElse(visit.location)
      userLocations.getOrElseUpdate(newUser, mutable.Set()) += newLocation
      locationUsers.getOrElseUpdate(newLocation, mutable.Set()) += newUser

      val oldUserId = visit.user
      val oldTimestamp = visit.visitedAt
      val oldUserKey = Key(oldUserId, oldTimestamp, visit.id)
      val oldUserVisit = userVisits(oldUserKey)
      val newUserId = update.user.getOrElse(oldUserId)
      val newTimestamp = update.visitedAt.getOrElse(oldTimestamp)
      val newUserKey = Key(newUserId, newTimestamp, visit.id)
      val newUserVisit = oldUserVisit.`with`(update, locationDao)
      if (oldUserKey != newUserKey) {
      } else {
        userVisits.remove(oldUserKey)
      }
      userVisits.put(newUserKey, newUserVisit)

      val oldLocationId = visit.location
      val oldLocationKey = Key(oldLocationId, oldTimestamp, visit.id)
      val oldLocationVisit = locationVisits(oldLocationKey)
      val newLocationId = update.location.getOrElse(oldLocationId)
      val newLocationKey = Key(newLocationId, newTimestamp, visit.id)
      val newLocationVisit = oldLocationVisit.`with`(update, userDao, generationDateTime)
      if (oldLocationKey != newLocationKey) {
        locationVisits.remove(oldLocationKey)
      }
      locationVisits.put(newLocationKey, newLocationVisit)
    }

  def updateLocation(locationId: Int, update: LocationUpdate): Unit = {
    locationUsers.get(locationId).foreach {
      _.foreach { user =>
        getAllUserVisits(user).view
          .withFilter(_._2.locationId == locationId)
          .map { case (key, visit) => (key, visit `with` update) }
          .toSeq
          .foreach { case (key, newVisit) => userVisits.put(key, newVisit) }
      }
    }
  }

  def updateUser(userId: Int, update: UserUpdate): Unit = {
    userLocations.get(userId).foreach {
      _.foreach { location =>
        getAllLocationVisits(location)
          .withFilter(_._2.userId == userId)
          .map { case (key, visit) => (key, visit.`with`(update, generationDateTime)) }
          .toSeq
          .foreach { case (key, newVisit) => locationVisits.put(key, newVisit) }
      }
    }
  }

  def userVisits(user: Int,
                 fromDate: Option[Int],
                 toDate: Option[Int],
                 country: Option[String],
                 toDistance: Option[Int]): Array[Byte] = {
    val filtered = getUserVisits(user, fromDate, toDate).valuesIterator
      .withFilter(userVisit =>
        toDistance.fold(true)(_ > userVisit.distance) &&
          country.fold(true)(_ == userVisit.country)
      ).map(_.json).toSeq
    // don't allocate each time?
    val buffer = ByteBuffer.allocate(
      filtered.map(_.length).sum // for jsons
        + (if (filtered.nonEmpty) filtered.size - 1 else 0) // for commas
        + jsonVisitsPrefix.length
        + jsonVisitsPostfix.length
    )
    buffer.put(jsonVisitsPrefix)
    if (filtered.nonEmpty) {
      buffer.put(filtered.head)
      filtered.tail.foreach { visit =>
        buffer.put(comma)
        buffer.put(visit)
      }
    }
    buffer.put(jsonVisitsPostfix)
    buffer.array()
  }

  private val jsonVisitsPrefix = """{"visits":[""".getBytes
  private val jsonVisitsPostfix = "]}".getBytes
  private val comma = ','.toByte

  def locationAvg(location: Int,
                  fromDate: Option[Int],
                  toDate: Option[Int],
                  fromAge: Option[Int],
                  toAge: Option[Int],
                  gender: Option[Char]): Array[Byte] = {
    val visits = getLocationVisits(location, fromDate, toDate).valuesIterator
    val found = visits.withFilter(visit =>
      fromAge.fold(true)(_ <= visit.age) &&
        toAge.fold(true)(_ > visit.age) &&
        gender.fold(true)(_ == visit.gender))
    val (count, sum) = found.foldLeft(0.0 -> 0.0) {
      case ((c, s), visit) => c + 1 -> (s + visit.mark)
    }
    val scale = 5
    val result =
      if (count == 0) "0.0".getBytes
      else BigDecimal(sum / count).setScale(scale, HALF_UP).toDouble.toString.getBytes
    // don't allocate each time?
    val buffer = ByteBuffer.wrap(Array.fill[Byte](
      jsonAvgPrefix.length
        + result.length // for double
        + 1 // for jsonAvgSuffix
    )(0))
    buffer.put(jsonAvgPrefix)
    buffer.put(result)
    buffer.put(jsonAvgSuffix)
    buffer.array()
  }

  private val jsonAvgPrefix = """{"avg":""".getBytes
  private val jsonAvgSuffix = '}'.toByte

  def cleanAfterPost(): Unit = {
    userLocations.clear()
    locationUsers.clear()
    visits.clear()
  }

  private def getAllUserVisits(user: Int) =
    userVisits.range(
      Key(user, Int.MinValue, Int.MinValue),
      Key(user, Int.MaxValue, Int.MaxValue))

  private def getAllLocationVisits(location: Int) =
    locationVisits.range(
      Key(location, Int.MinValue, Int.MinValue),
      Key(location, Int.MaxValue, Int.MaxValue))

  private def getUserVisits(user: Int, fromDate: Option[Int], toDate: Option[Int]) =
    getByKeyAndDate(userVisits, user, fromDate, toDate)

  private def getLocationVisits(location: Int, fromDate: Option[Int], toDate: Option[Int]) =
    getByKeyAndDate(locationVisits, location, fromDate, toDate)

  private def getByKeyAndDate[V](map: mutable.TreeMap[Key, V], key: Int, fromDate: Option[Int], toDate: Option[Int]) =
    map.range(
      Key(key, fromDate.getOrElse(Int.MinValue), Int.MinValue),
      Key(key, toDate.getOrElse(Int.MaxValue), Int.MinValue))
}

case class Key(key: Int, timestamp: Int, visitId: Int)
