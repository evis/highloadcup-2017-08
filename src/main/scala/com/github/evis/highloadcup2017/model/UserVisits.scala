package com.github.evis.highloadcup2017.model

import com.github.evis.highloadcup2017.dao.LocationDao

case class UserVisits(visits: Iterable[UserVisit])

case class UserVisit(visitId: Int,
                     locationId: Int,
                     mark: Int,
                     visitedAt: Int,
                     place: String,
                     country: String,
                     distance: Int) {

  def `with`(update: VisitUpdate, locationDao: LocationDao): UserVisit = {
    val location =
      update.location.filter(_ != locationId).flatMap(locationDao.read)
    copy(
      locationId = update.location.getOrElse(locationId),
      mark = update.mark.getOrElse(mark),
      visitedAt = update.visitedAt.getOrElse(visitedAt),
      place = location.fold(place)(_.place),
      country = location.fold(country)(_.country),
      distance = location.fold(distance)(_.distance)
    )
  }

  def `with`(update: LocationUpdate): UserVisit = copy(
    place = update.place.getOrElse(place),
    country = update.country.getOrElse(country),
    distance = update.distance.getOrElse(distance)
  )
}

case class UserVisitsRequest(user: Int,
                             fromDate: Option[Int],
                             toDate: Option[Int],
                             country: Option[String],
                             toDistance: Option[Int])
