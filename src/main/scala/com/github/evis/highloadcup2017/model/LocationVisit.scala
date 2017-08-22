package com.github.evis.highloadcup2017.model

import java.time.temporal.ChronoUnit.YEARS
import java.time.{LocalDateTime, ZoneOffset}

import com.github.evis.highloadcup2017.dao.UserDao

case class LocationVisit(userId: Int,
                         visitId: Int,
                         mark: Int,
                         visitedAt: Int,
                         age: Int,
                         gender: Char) {

  def `with`(update: UserUpdate, generationDateTime: LocalDateTime): LocationVisit = copy(
    age = update.birthDate.map(
      LocalDateTime.ofEpochSecond(_, 0, ZoneOffset.UTC).until(generationDateTime, YEARS).toInt
    ).getOrElse(age),
    gender = update.gender.getOrElse(gender)
  )

  def `with`(update: VisitUpdate, userDao: UserDao, generationDateTime: LocalDateTime): LocationVisit = {
    val user = update.user.filter(_ != userId).flatMap(userDao.read)
    copy(
      userId = update.user.getOrElse(userId),
      mark = update.mark.getOrElse(mark),
      visitedAt = update.visitedAt.getOrElse(visitedAt),
      age = user.fold(age)( u =>
        LocalDateTime.ofEpochSecond(u.birthDate, 0, ZoneOffset.UTC).until(generationDateTime, YEARS).toInt
      ),
      gender = user.fold(gender)(_.gender)
    )
  }
}
