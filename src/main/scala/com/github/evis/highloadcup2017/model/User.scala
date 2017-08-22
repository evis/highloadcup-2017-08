package com.github.evis.highloadcup2017.model

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
}

case class UserUpdate(email: Option[String],
                      firstName: Option[String],
                      lastName: Option[String],
                      gender: Option[Char],
                      birthDate: Option[Int]) extends EntityUpdate
