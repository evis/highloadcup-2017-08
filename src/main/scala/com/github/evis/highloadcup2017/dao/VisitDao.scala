package com.github.evis.highloadcup2017.dao

import java.time._
import java.time.temporal.ChronoUnit.YEARS

import com.github.evis.highloadcup2017.model._

import scala.collection.mutable
import scala.math.BigDecimal.RoundingMode.HALF_UP

class VisitDao(userDao: UserDao, locationDao: LocationDao, generationInstant: Instant) {
  private val visits = mutable.HashMap[Int, Visit]()
  // here int is user id
  private val userVisits = mutable.HashMap[Int, mutable.SortedSet[UserVisit]]()
  // and here int is location id
  private val locationVisits = new mutable.HashMap[Int, mutable.SortedSet[LocationVisit]]()

  def create(visit: Visit): Unit = {
    // should put visit if location or user not found?
    visits += visit.id -> visit
    locationDao.read(visit.location) match {
      case Some(location) =>
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
        user.birthDate.atZone(ZoneId.systemDefault()).until(
          generationInstant.atZone(ZoneId.systemDefault()),
          YEARS
        ).toInt,
        user.gender
      )
    }
  }

  def read(id: Int): Option[Visit] = visits.get(id)

  //noinspection UnitInMap
  def update(id: Int, update: VisitUpdate): Option[Unit] =
    read(id).map { visit =>
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
      }) += (oldUserVisit `with` update)
      visits += id -> (visit `with` update)
    }

  def updateLocation(locationId: Int, update: LocationUpdate): Unit = {
    // need one more index to do it faster?
    userVisits.values.foreach { visits =>
      val toUpdate = visits.filter(_.locationId == locationId)
      visits --= toUpdate
      val seq = toUpdate.toSeq
      visits ++= seq.map(_ `with` update)
    }
  }

  def userVisits(request: UserVisitsRequest): Option[UserVisits] = {
    userDao.read(request.user).map { _ =>
      val optVisits = userVisits.get(request.user)
      UserVisits(
        // to is inclusive! need to be exclusive =(
        optVisits.fold(Seq[UserVisit]())(_.rangeImpl(request.fromDate, request.toDate)
          .filter(userVisit =>
            request.toDistance.fold(true)(_ > userVisit.distance) &&
              request.country.fold(true)(_ == userVisit.country)
          ).toSeq))
    }
  }

  def locationAvg(request: LocationAvgRequest): Option[LocationAvgResponse] = {
    locationDao.read(request.location).map { _ =>
      val optVisits = locationVisits.get(request.location)
      // again bad inclusive
      val found = optVisits.fold(mutable.SortedSet[LocationVisit]())(_.rangeImpl(request.fromDate, request.toDate)
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
