package com.github.evis.highloadcup2017.model

import java.nio.ByteBuffer
import java.time.temporal.ChronoUnit.YEARS
import java.time.{LocalDateTime, ZoneOffset}

import com.github.evis.highloadcup2017.api.WithFiller

case class User(id: Int,
                email: String,
                firstName: String,
                lastName: String,
                gender: Char,
                birthDate: Int) extends Entity with WithFiller {

  def `with`(update: UserUpdate): User = copy(
    email = update.email.getOrElse(email),
    firstName = update.firstName.getOrElse(firstName),
    lastName = update.lastName.getOrElse(lastName),
    gender = update.gender.getOrElse(gender),
    birthDate = update.birthDate.getOrElse(birthDate)
  )

  def age(generationDateTime: LocalDateTime): Int =
    LocalDateTime.ofEpochSecond(birthDate, 0, ZoneOffset.UTC).until(generationDateTime, YEARS).toInt

  override def fill(buffer: ByteBuffer): Unit = {
    // make fields Array[Byte]?
    import User._
    buffer.position(0)
    buffer.put(Id)
    buffer.put(id.toString.getBytes)
    buffer.put(Email)
    buffer.put(email.getBytes)
    buffer.put(FirstName)
    buffer.put(firstName.getBytes)
    buffer.put(LastName)
    buffer.put(lastName.getBytes)
    buffer.put(Gender)
    buffer.put(gender.toByte)
    buffer.put(BirthDate)
    buffer.put(birthDate.toString.getBytes)
    buffer.put(End)
  }
}

object User {
  private val Id = """{"id":""".getBytes
  private val Email = ""","email":"""".getBytes
  private val FirstName = """","first_name":"""".getBytes
  private val LastName = """","last_name":"""".getBytes
  private val Gender = """","gender":"""".getBytes
  private val BirthDate = """","birth_date":""".getBytes
  private val End = "}".getBytes
}

case class UserUpdate(email: Option[String],
                      firstName: Option[String],
                      lastName: Option[String],
                      gender: Option[Char],
                      birthDate: Option[Int]) extends EntityUpdate
