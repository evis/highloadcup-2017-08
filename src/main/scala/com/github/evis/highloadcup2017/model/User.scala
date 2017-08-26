package com.github.evis.highloadcup2017.model

import java.time.temporal.ChronoUnit.YEARS
import java.time.{LocalDateTime, ZoneOffset}

case class User(id: Int,
                email: String,
                firstName: String,
                lastName: String,
                gender: Char,
                birthDate: Int) extends Entity {

  def `with`(update: UserUpdate): User = copy(
    email = update.email.getOrElse(email),
    firstName = update.firstName.getOrElse(firstName),
    lastName = update.lastName.getOrElse(lastName),
    gender = update.gender.getOrElse(gender),
    birthDate = update.birthDate.getOrElse(birthDate)
  )

  def age(generationDateTime: LocalDateTime): Int =
    LocalDateTime.ofEpochSecond(birthDate, 0, ZoneOffset.UTC).until(generationDateTime, YEARS).toInt
}

case class UserUpdate(email: Option[String],
                      firstName: Option[String],
                      lastName: Option[String],
                      gender: Option[Char],
                      birthDate: Option[Int]) extends EntityUpdate
