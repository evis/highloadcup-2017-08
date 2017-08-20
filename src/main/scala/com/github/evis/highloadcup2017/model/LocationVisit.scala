package com.github.evis.highloadcup2017.model

import java.time.temporal.ChronoUnit.YEARS
import java.time.{Instant, ZoneId}

import com.github.evis.highloadcup2017.dao.UserDao

case class LocationVisit(userId: Int,
                         visitId: Int,
                         mark: Int,
                         visitedAt: Instant,
                         age: Int,
                         gender: Char) {

  def `with`(update: UserUpdate, generationInstant: Instant): LocationVisit = copy(
    age = update.birthDate.map(
      _.atZone(ZoneId.systemDefault()).until(
        generationInstant.atZone(ZoneId.systemDefault()),
        YEARS).toInt
      ).getOrElse(age),
    gender = update.gender.getOrElse(gender)
  )

  def `with`(update: VisitUpdate, userDao: UserDao, generationInstant: Instant): LocationVisit = {
    val user = update.user.filter(_ != userId).flatMap(userDao.read)
    copy(
      userId = update.user.getOrElse(userId),
      mark = update.mark.getOrElse(mark),
      visitedAt = update.visitedAt.getOrElse(visitedAt),
      age = user.fold(age)(_.birthDate
        .atZone(ZoneId.systemDefault()).until(
        generationInstant.atZone(ZoneId.systemDefault()),
        YEARS).toInt
      ),
      gender = user.fold(gender)(_.gender)
    )
  }
}

case class LocationAvgRequest(location: Int,
                              fromDate: Option[Instant],
                              toDate: Option[Instant],
                              fromAge: Option[Int],
                              toAge: Option[Int],
                              gender: Option[Char])

case class LocationAvgResponse(avg: Double)
