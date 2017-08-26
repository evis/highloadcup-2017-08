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

  // key of inner map is timestamp
  private val userVisitsIndex =
    mutable.Map.apply[Int, mutable.TreeMap[Int, Set[UserVisit]]]()
  private val locationVisits =
    mutable.Map.apply[Int, Set[LocationVisit]]()
  // helper indexes for faster update
  private val userLocations = mutable.HashMap[Int, mutable.Set[Int]]()
  private val locationUsers = mutable.HashMap[Int, mutable.Set[Int]]()

  def create(visit: Visit): Unit = {
    // should put visit if location or user not found?
    visits += visit.id -> visit
    locationDao.read(visit.location) match {
      case Some(location) =>
        userLocations.getOrElseUpdate(visit.user, mutable.Set()) += visit.location
        locationUsers.getOrElseUpdate(visit.location, mutable.Set()) += visit.user
        val uv = UserVisit(
          visit.id,
          visit.location,
          visit.mark,
          visit.visitedAt,
          location.place,
          location.country,
          location.distance,
        )
        val map = userVisitsIndex.getOrElseUpdate(visit.user, mutable.TreeMap())
        map.put(visit.visitedAt, map.get(visit.visitedAt).map(_ + uv).getOrElse(Set(uv)))
      case None =>
        // handle it with exception handler?
        throw new Exception(s"Location ${visit.location} not found")
    }
    userDao.read(visit.user).foreach { user =>
      val lv = LocationVisit(
        visit.user,
        visit.id,
        visit.mark,
        visit.visitedAt,
        // switch to datetimes everywhere?
        LocalDateTime.ofEpochSecond(user.birthDate, 0, ZoneOffset.UTC).until(generationDateTime, YEARS).toInt,
        user.gender
      )
      locationVisits.put(visit.visitedAt,
        locationVisits.get(visit.location).map(_ - lv).getOrElse(Set(lv)))
    }
  }

  def read(id: Int): Option[Visit] = visits.get(id)

  def json(id: Int): Array[Byte] = visits(id).toJson.compactPrint.getBytes

  //noinspection UnitInMap
  def update(id: Int, update: VisitUpdate): Option[Unit] =
    read(id).map { visit =>
      val updated = visit `with` update
      visits.put(id, updated)

      // can not always update it
      userLocations(visit.user) -= visit.location
      locationUsers(visit.location) -= visit.user
      val newUser = update.user.getOrElse(visit.user)
      val newLocation = update.location.getOrElse(visit.location)
      userLocations.getOrElseUpdate(newUser, mutable.Set()) += newLocation
      locationUsers.getOrElseUpdate(newLocation, mutable.Set()) += newUser

      val oldUserId = visit.user
      val oldTimestamp = visit.visitedAt
      val oldUserVisits = userVisitsIndex(oldUserId)(oldTimestamp)
      val oldUserVisit = oldUserVisits.find(_.visitId == visit.id)
      val newUserId = update.user.getOrElse(oldUserId)
      val newTimestamp = update.visitedAt.getOrElse(oldTimestamp)
      oldUserVisit.foreach { ouv =>
        val newUserVisit = ouv.`with`(update, locationDao)
        if (oldUserId != newUserId || oldTimestamp != newTimestamp) {
          userVisitsIndex(oldUserId).put(oldTimestamp, oldUserVisits - ouv)
        }
        val map = userVisitsIndex.getOrElseUpdate(newUserId, mutable.TreeMap())
        map.put(newTimestamp, map.get(newTimestamp).map(_ + newUserVisit).getOrElse(Set(newUserVisit)))
      }

      val oldLocationId = visit.location
      val oldLocationVisits = locationVisits(oldLocationId)
      val newLocationId = update.location.getOrElse(oldLocationId)
      oldLocationVisits.foreach { olv =>
        val newLocationVisit = olv.`with`(update, userDao, generationDateTime)
        if (oldLocationId != newLocationId) {
          locationVisits.put(oldLocationId, locationVisits(oldLocationId) - olv)
        }
        locationVisits.put(newLocationId,
          locationVisits.get(newLocationId).map(_ + newLocationVisit).getOrElse(Set(newLocationVisit)))
      }
    }

  def updateLocation(locationId: Int, update: LocationUpdate): Unit = {
    locationUsers.get(locationId).foreach {
      _.foreach { user =>
        val map = userVisitsIndex(user)
        map.foreach { case (key, visitsSet) =>
          for (visit <- visitsSet if visit.locationId == locationId) {
            map.update(key, visitsSet - visit + (visit `with` update))
          }
        }
      }
    }
  }

  def updateUser(userId: Int, update: UserUpdate): Unit = {
    userLocations.get(userId).foreach {
      _.foreach { location =>
        val set = locationVisits(location)
        for (visit <- set if visit.userId == userId) {
          locationVisits.update(location,
            locationVisits(location) - visit + visit.`with`(update, generationDateTime))
        }
      }
    }
  }

  def userVisits(user: Int,
                 fromDate: Option[Int],
                 toDate: Option[Int],
                 country: Option[String],
                 toDistance: Option[Int]): Array[Byte] = {
    val filtered = userVisitsIndex.get(user).fold(Iterable[Array[Byte]]())(
      _.rangeImpl(fromDate, toDate).mapValues(
        _.withFilter(userVisit =>
          toDistance.fold(true)(_ > userVisit.distance) &&
            country.fold(true)(_ == userVisit.country)
        ).map(_.json)
      ).flatMap(_._2)
    )
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
    val found = locationVisits.get(location).fold(Iterable[Int]())(
      _.withFilter(visit =>
        fromDate.fold(true)(_ < visit.visitedAt) &&
          toDate.fold(true)(_ > visit.visitedAt) &&
          fromAge.fold(true)(_ <= visit.age) &&
          toAge.fold(true)(_ > visit.age) &&
          gender.fold(true)(_ == visit.gender))
        .map(_.mark)
    )
    val (count, sum) = found.foldLeft(0.0 -> 0.0) {
      case ((c, s), mark) => c + 1 -> (s + mark)
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
}
