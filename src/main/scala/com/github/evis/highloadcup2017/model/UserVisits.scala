package com.github.evis.highloadcup2017.model

import com.github.evis.highloadcup2017.dao.LocationDao

case class UserVisits(visits: Iterable[UserVisit])

case class UserVisit(visitId: Int,
                     locationId: Int,
                     mark: Int,
                     visitedAt: Int,
                     place: String,
                     country: String,
                     distance: Int,
                     json: Array[Byte]) {

  import UserVisit._

  def `with`(update: VisitUpdate, locationDao: LocationDao): UserVisit = {
    val location =
      update.location.filter(_ != locationId).flatMap(locationDao.read)
    val newMark = update.mark.getOrElse(mark)
    val newVisitedAt = update.visitedAt.getOrElse(visitedAt)
    val newPlace = location.fold(place)(_.place)
    copy(
      locationId = update.location.getOrElse(locationId),
      mark = newMark,
      visitedAt = newVisitedAt,
      place = newPlace,
      country = location.fold(country)(_.country),
      distance = location.fold(distance)(_.distance),
      json = genJson(newMark, newVisitedAt, newPlace)
    )
  }

  def `with`(update: LocationUpdate): UserVisit = copy(
    place = update.place.getOrElse(place),
    country = update.country.getOrElse(country),
    distance = update.distance.getOrElse(distance),
    json = update.place.map(newPlace =>
      genJson(mark, visitedAt, newPlace)
    ).getOrElse(json)
  )
}

object UserVisit {
  def genJson(mark: Int, visitedAt: Int, place: String): Array[Byte] = {
    val builder = new StringBuilder
    builder.append("{\"mark\':")
    builder.append(mark)
    builder.append(",\"visitedAt\":")
    builder.append(visitedAt)
    builder.append(",\"place\":\"")
    builder.append(place)
    builder.append("\"}")
    builder.append(Array[Byte]())
    builder.result().getBytes
  }
}
