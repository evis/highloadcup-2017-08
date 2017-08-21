package com.github.evis.highloadcup2017.dao

import java.time._
import java.time.temporal.ChronoUnit.YEARS

import com.github.evis.highloadcup2017.model._

import scala.collection.mutable
import scala.math.BigDecimal.RoundingMode.HALF_UP

class VisitDao(userDao: UserDao, locationDao: LocationDao, generationDateTime: LocalDateTime) {
  private val visits = mutable.HashMap[Int, Visit]()
  // here int is user id
  private val userVisits = mutable.HashMap[Int, mutable.SortedSet[UserVisit]]()
  // and here int is location id
  private val locationVisits = new mutable.HashMap[Int, mutable.SortedSet[LocationVisit]]()
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
        userVisits.getOrElseUpdate(visit.user, mutable.SortedSet()) += UserVisit(
          visit.id,
          visit.location,
          visit.mark,
          visit.visitedAt,
          location.place,
          location.country,
          location.distance
        )
      case None =>
        // handle it with exception handler?
        throw new Exception(s"Location ${visit.location} not found")
    }
    userDao.read(visit.user).foreach { user =>
      locationVisits.getOrElseUpdate(visit.location, mutable.SortedSet()) += LocationVisit(
        visit.user,
        visit.id,
        visit.mark,
        visit.visitedAt,
        // switch to datetimes everywhere?
        LocalDateTime.ofEpochSecond(user.birthDate, 0, ZoneOffset.UTC).until(generationDateTime, YEARS).toInt,
        user.gender
      )
    }
  }

  def read(id: Int): Option[Visit] = visits.get(id)

  //noinspection UnitInMap
  def update(id: Int, update: VisitUpdate): Option[Unit] =
    read(id).map { visit =>
      // can not always update it
      userLocations(visit.user) -= visit.location
      locationUsers(visit.location) -= visit.user
      val newUser = update.user.getOrElse(visit.user)
      val newLocation = update.location.getOrElse(visit.location)
      userLocations.getOrElseUpdate(newUser, mutable.Set()) += newLocation
      locationUsers.getOrElseUpdate(newLocation, mutable.Set()) += newUser

      val oldUserId = visit.user
      val oldUserVisits = userVisits(oldUserId)
      val oldUserVisit = oldUserVisits.find(_.visitId == id).getOrElse(
        throw new RuntimeException(s"Unable to find user visit ${visit.id} for user ${visit.user}"))
      oldUserVisits.remove(oldUserVisit)
      (update.user match {
        case Some(newUserId) if newUserId != oldUserId =>
          userVisits.getOrElseUpdate(newUserId, mutable.SortedSet())
        case _ =>
          oldUserVisits
      }) += oldUserVisit.`with`(update, locationDao)
      val oldLocationId = visit.location
      val oldLocationVisits = locationVisits(oldLocationId)
      val oldLocationVisit = oldLocationVisits.find(_.visitId == id).getOrElse(
        throw new RuntimeException(s"Unable to find user visit ${visit.id} for user ${visit.user}"))
      oldLocationVisits.remove(oldLocationVisit)
      (update.location match {
        case Some(newLocationId) if newLocationId != oldLocationId =>
          locationVisits.getOrElseUpdate(newLocationId, mutable.SortedSet())
        case _ =>
          oldLocationVisits
      }) += oldLocationVisit.`with`(update, userDao, generationDateTime)
      visits += id -> (visit `with` update)
    }

  def updateLocation(locationId: Int, update: LocationUpdate): Unit = {
    locationUsers.get(locationId).foreach {
      _.foreach { user =>
        userVisits.get(user).foreach { visits =>
          val toUpdate = visits.filter(_.locationId == locationId)
          visits --= toUpdate
          val seq = toUpdate.toSeq
          visits ++= seq.map(_ `with` update)
        }
      }
    }
  }

  def updateUser(userId: Int, update: UserUpdate): Unit = {
    userLocations.get(userId).foreach {
      _.foreach { location =>
        locationVisits.get(location).foreach { visits =>
          val toUpdate = visits.filter(_.userId == userId)
          visits --= toUpdate
          val seq = toUpdate.toSeq
          visits ++= seq.map(_.`with`(update, generationDateTime))
        }
      }
    }
  }

  def userVisits(request: UserVisitsRequest): Option[UserVisits] = {
    userDao.read(request.user).map { _ =>
      val optVisits = userVisits.get(request.user)
      UserVisits(
        optVisits.fold(Seq[UserVisit]())(
          _.rangeImpl(request.fromDate,
            intToUserVisit(request.toDate))
            .filter(userVisit =>
              request.toDistance.fold(true)(_ > userVisit.distance) &&
                request.country.fold(true)(_ == userVisit.country)
            ).toSeq))
    }
  }

  def locationAvg(request: LocationAvgRequest): Option[LocationAvgResponse] = {
    locationDao.read(request.location).map { _ =>
      val optVisits = locationVisits.get(request.location)
      val found = optVisits.fold(mutable.SortedSet[LocationVisit]())(
        _.rangeImpl(request.fromDate,
          intToLocationVisit(request.toDate))
          .filter(visit =>
            request.fromAge.fold(true)(_ < visit.age) &&
              request.toAge.fold(true)(_ > visit.age) &&
              request.gender.fold(true)(_ == visit.gender)))
      val count = found.size
      val avg =
        if (count == 0) 0
        else found.foldLeft(0.0)((acc, visit) => acc + visit.mark) / count
      LocationAvgResponse(BigDecimal(avg).setScale(5, HALF_UP).toDouble)
    }
  }
}
