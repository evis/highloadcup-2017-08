package com.github.evis.highloadcup2017.dao

import java.time._
import java.time.temporal.ChronoUnit.YEARS
import java.util
import java.util.Collections.emptyNavigableMap
import java.util.Comparator

import com.github.evis.highloadcup2017.model._
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.math.BigDecimal.RoundingMode.HALF_UP

class VisitDao(userDao: UserDao, locationDao: LocationDao, generationDateTime: LocalDateTime) extends StrictLogging {
  private val visits = mutable.HashMap[Int, Visit]()

  private val keyOrdering = Ordering.by(Key.unapply)

  private val keyComparator = new Comparator[Key] {
    override def compare(k1: Key, k2: Key): Int = keyOrdering.compare(k1, k2)
  }

  private val userVisits = new util.TreeMap[Key, UserVisit](keyComparator)
  private val locationVisits = new util.TreeMap[Key, LocationVisit](keyComparator)
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
        userVisits.put(Key(visit.user, visit.visitedAt, visit.id), UserVisit(
          visit.id,
          visit.location,
          visit.mark,
          visit.visitedAt,
          location.place,
          location.country,
          location.distance
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

  //noinspection UnitInMap
  def update(id: Int, update: VisitUpdate): Option[Unit] =
    read(id).map { visit =>
      visits.put(id, visit `with` update)

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
      val oldUserVisit = userVisits.get(oldUserKey)
      val newUserId = update.user.getOrElse(oldUserId)
      val newTimestamp = update.visitedAt.getOrElse(oldTimestamp)
      val newUserKey = Key(newUserId, newTimestamp, visit.id)
      val newUserVisit = oldUserVisit.`with`(update, locationDao)
      if (oldUserKey == newUserKey) {
        userVisits.replace(newUserKey, newUserVisit)
      } else {
        userVisits.remove(oldUserKey)
        userVisits.put(newUserKey, newUserVisit)
      }

      val oldLocationId = visit.location
      val oldLocationKey = Key(oldLocationId, oldTimestamp, visit.id)
      val oldLocationVisit = locationVisits.get(oldLocationKey)
      val newLocationId = update.location.getOrElse(oldLocationId)
      val newLocationKey = Key(newLocationId, newTimestamp, visit.id)
      val newLocationVisit = oldLocationVisit.`with`(update, userDao, generationDateTime)
      if (oldLocationKey == newLocationKey) {
        locationVisits.replace(newLocationKey, newLocationVisit)
      } else {
        locationVisits.remove(oldLocationKey)
        locationVisits.put(newLocationKey, newLocationVisit)
      }
    }

  def updateLocation(locationId: Int, update: LocationUpdate): Unit = {
    locationUsers.get(locationId).foreach {
      _.foreach { user =>
        getAllUserVisits(user).asScala.view
          .filter(_._2.locationId == locationId)
          .map { case (key, visit) => (key, visit `with` update) }
          .toSeq
          .foreach { case (key, newVisit) => userVisits.replace(key, newVisit) }
      }
    }
  }

  def updateUser(userId: Int, update: UserUpdate): Unit = {
    userLocations.get(userId).foreach {
      _.foreach { location =>
        getAllLocationVisits(location).asScala.view
          .filter(_._2.userId == userId)
          .map { case (key, visit) => (key, visit.`with`(update, generationDateTime)) }
          .toSeq
          .foreach { case (key, newVisit) => locationVisits.replace(key, newVisit) }
      }
    }
  }

  def userVisits(request: UserVisitsRequest): Option[UserVisits] = {
    userDao.read(request.user).map { _ =>
      UserVisits(
        getUserVisits(request.user, request.fromDate, request.toDate).values().asScala
          .filter(userVisit =>
            request.toDistance.fold(true)(_ > userVisit.distance) &&
              request.country.fold(true)(_ == userVisit.country)
          ).toSeq
      )
    }
  }

  def locationAvg(request: LocationAvgRequest): Option[LocationAvgResponse] = {
    locationDao.read(request.location).map { _ =>
      val visits = getLocationVisits(request.location, request.fromDate, request.toDate).values().asScala
      val found = visits.filter(visit =>
        request.fromAge.fold(true)(_ <= visit.age) &&
          request.toAge.fold(true)(_ > visit.age) &&
          request.gender.fold(true)(_ == visit.gender))
      val count = found.size
      val avg =
        if (count == 0) 0
        else found.foldLeft(0.0)((acc, visit) => acc + visit.mark) / count
      LocationAvgResponse(BigDecimal(avg).setScale(5, HALF_UP).toDouble)
    }
  }

  private def getAllUserVisits(user: Int) =
    userVisits.subMap(
      Key(user, Int.MinValue, Int.MinValue), true,
      Key(user, Int.MaxValue, Int.MaxValue), true)

  private def getAllLocationVisits(location: Int) =
    locationVisits.subMap(
      Key(location, Int.MinValue, Int.MinValue), true,
      Key(location, Int.MaxValue, Int.MaxValue), true)

  private def getUserVisits(user: Int, fromDate: Option[Int], toDate: Option[Int]) =
    getByKeyAndDate(userVisits, user, fromDate, toDate)

  private def getLocationVisits(location: Int, fromDate: Option[Int], toDate: Option[Int]) =
    getByKeyAndDate(locationVisits, location, fromDate, toDate)

  private def getByKeyAndDate[V](map: util.TreeMap[Key, V], key: Int, fromDate: Option[Int], toDate: Option[Int]) =
    (fromDate, toDate) match {
      case (Some(from), Some(to)) if from >= to =>
        emptyNavigableMap[Key, V]()
      case _ =>
        map.subMap(
          Key(key, fromDate.getOrElse(Int.MinValue), Int.MinValue), true,
          Key(key, toDate.getOrElse(Int.MaxValue), Int.MinValue), false)
    }
}

case class Key(key: Int, timestamp: Int, visitId: Int)
