package com.github.evis.highloadcup2017.dao

import java.time.Instant

import com.github.evis.highloadcup2017.model._

import scala.collection.mutable

class VisitDao(locationDao: LocationDao, generationInstant: Instant) {
  private val visits = mutable.HashMap[Int, Visit]()
  // here int is user id
  private val userVisits = mutable.HashMap[Int, mutable.SortedSet[UserVisit]]()

  def create(visit: Visit): Unit = {
    // should put visit if location not found?
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

  def userVisits(request: UserVisitsRequest): UserVisits = {
    UserVisits(
      userVisits.get(request.user).fold(Seq[UserVisit]())(
        _.rangeImpl(request.fromDate, request.toDate) // to is inclusive! need to be exclusive =(
          .filter(userVisit =>
          request.toDistance.fold(true)(_ > userVisit.distance) &&
            request.country.fold(true)(_ == userVisit.country)
        ).toSeq)
    )
  }
}
