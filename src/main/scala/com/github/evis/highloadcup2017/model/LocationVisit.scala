package com.github.evis.highloadcup2017.model

import java.time.temporal.ChronoUnit.YEARS
import java.time.{Instant, ZoneId}

case class LocationVisit(userId: Int,
                         visitId: Int,
                         mark: Int,
                         visitedAt: Instant,
                         age: Int,
                         gender: Gender) {

  def `with`(update: UserUpdate, generationInstant: Instant): LocationVisit = copy(
    age = update.birthDate.map(
      _.atZone(ZoneId.systemDefault()).until(
        generationInstant.atZone(ZoneId.systemDefault()),
        YEARS).toInt
      ).getOrElse(age),
    gender = update.gender.getOrElse(gender)
  )

  def `with`(update: VisitUpdate): LocationVisit = copy(
    mark = update.mark.getOrElse(mark),
    visitedAt = update.visitedAt.getOrElse(visitedAt)
  )
}

case class LocationAvgRequest(location: Int,
                              fromDate: Option[Instant],
                              toDate: Option[Instant],
                              fromAge: Option[Int],
                              toAge: Option[Int],
                              gender: Option[Gender])

case class LocationAvgResponse(avg: Double)
