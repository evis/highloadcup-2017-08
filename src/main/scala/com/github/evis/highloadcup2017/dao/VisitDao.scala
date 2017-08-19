package com.github.evis.highloadcup2017.dao

import java.time.Instant
import java.util.Collections.emptyNavigableSet
import java.util.concurrent.ConcurrentHashMap

import com.github.evis.highloadcup2017.model.{UserVisit, UserVisits, UserVisitsRequest, Visit, VisitUpdate}
import com.google.common.collect.TreeMultimap

import scala.collection.JavaConverters._

class VisitDao(locationDao: LocationDao, generationInstant: Instant) {
  private val visits = new ConcurrentHashMap[Int, Visit]()
  // here int is user id
  private val userVisits = TreeMultimap.create[Int, UserVisit](
    (i1: Int, i2: Int) => i1 - i2,
    (v1: UserVisit, v2: UserVisit) => v1.visitedAt.compareTo(v2.visitedAt)
  )

  def create(visit: Visit): Unit = {
    // should put visit if location not found?
    visits.put(visit.id, visit)
    locationDao.read(visit.location) match {
      case Some(location) =>
        userVisits.put(visit.user, UserVisit(
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
  }

  def read(id: Int): Option[Visit] =
    Option(visits.get(id))

  //noinspection UnitInMap
  def update(id: Int, update: VisitUpdate): Option[Unit] =
    read(id).map(visit => visits.put(id, visit `with` update))

  def userVisits(request: UserVisitsRequest): Option[UserVisits] = {
    val allVisits = userVisits.get(request.user)
    val filteredByDate = (request.fromDate, request.toDate) match {
      case (Some(from), Some(to)) if from.isBefore(to) =>
        allVisits.subSet(from, false, to, false)
      case (Some(from), None) =>
        allVisits.tailSet(from, false)
      case (None, Some(to)) =>
        allVisits.headSet(to, false)
      case (None, None) =>
        allVisits
      case _ =>
        emptyNavigableSet()
    }
    filteredByDate.asScala.filter(userVisit =>
      request.toDistance.fold(true)(_ > userVisit.distance) &&
        request.country.fold(true)(_ == userVisit.country)
    ).toSeq match {
      case Seq() => None
      case filteredVisits => Some(UserVisits(filteredVisits))
    }
  }
}
