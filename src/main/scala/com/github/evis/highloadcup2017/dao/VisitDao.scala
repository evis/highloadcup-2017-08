package com.github.evis.highloadcup2017.dao

import java.nio.ByteBuffer
import java.time._

import com.github.evis.highloadcup2017.api.JsonFormats
import com.github.evis.highloadcup2017.model._
import com.typesafe.scalalogging.StrictLogging
import spray.json._

import scala.collection.mutable
import scala.math.BigDecimal.RoundingMode.HALF_UP

class VisitDao(userDao: UserDao,
               locationDao: LocationDao,
               generationDateTime: LocalDateTime) extends JsonFormats with StrictLogging with Dao {
  private val visits = mutable.HashMap[Int, Visit]()

  // better to refactor with value types?
  // user_id -> visited_at -> visit_id
  private val userVisitsIndex = mutable.Map.apply[Int, mutable.TreeMap[Int, mutable.Set[Int]]]()
  // location id -> visit_id
  private val locationVisits = mutable.Map.apply[Int, mutable.Set[Int]]()

  def create(visit: Visit): Unit = {
    // should put visit if location or user not found?
    visits += visit.id -> visit
    userVisitsIndex.getOrElseUpdate(visit.user, mutable.TreeMap())
      .getOrElseUpdate(visit.visitedAt, mutable.Set())
      .add(visit.id)
    locationVisits.getOrElseUpdate(visit.location, mutable.Set()).add(visit.id)
  }

  def read(id: Int): Option[Visit] = visits.get(id)

  def json(id: Int): Array[Byte] = visits(id).toJson.compactPrint.getBytes

  //noinspection UnitInMap
  def update(id: Int, update: VisitUpdate): Option[Unit] =
    read(id).map { visit =>
      val updated = visit `with` update
      visits.put(id, updated)
      // does this method need optimizition?
      val oldUserId = visit.user
      val oldTimestamp = visit.visitedAt
      val oldUserVisits = userVisitsIndex(oldUserId)(oldTimestamp)
      val oldUserVisit = oldUserVisits.find(_ == id)
      val newUserId = update.user.getOrElse(oldUserId)
      val newTimestamp = update.visitedAt.getOrElse(oldTimestamp)
      oldUserVisit.foreach { _ =>
        if (oldUserId != newUserId || oldTimestamp != newTimestamp) {
          oldUserVisits.remove(id)
        }
        userVisitsIndex.getOrElseUpdate(newUserId, mutable.TreeMap())
          .getOrElseUpdate(newTimestamp, mutable.Set()).add(id)
      }
      val oldLocationId = visit.location
      val oldLocationVisits = locationVisits(oldLocationId)
      val newLocationId = update.location.getOrElse(oldLocationId)
      if (oldLocationId != newLocationId) {
        oldLocationVisits.remove(id)
      }
      locationVisits.getOrElseUpdate(newLocationId, mutable.Set()).add(id)
    }

  def userVisits(user: Int,
                 fromDate: Option[Int],
                 toDate: Option[Int],
                 country: Option[String],
                 toDistance: Option[Int]): Array[Byte] = {
    // optimize?
    val filtered = userVisitsIndex.get(user).fold(Iterable[Array[Byte]]())(
      _.rangeImpl(fromDate, toDate).values.flatMap(
        _.map { visitId =>
          val visit = visits(visitId)
          val location = locationDao.read(visit.location).getOrElse(
            // proper None handling?
            deserializationError("")
          )
          UserVisit(
            visit.mark,
            visit.visitedAt,
            location.place,
            location.country,
            location.distance)
        }.withFilter(userVisit =>
          toDistance.fold(true)(_ > userVisit.distance) &&
            country.fold(true)(_ == userVisit.country)
        ).map(_.json)
      )
    )
    // don't allocate each time?
    val buffer = ByteBuffer.allocate(
      filtered.map(_.length).sum // for jsons
        + (if (filtered.nonEmpty) filtered.size - 1 else 0) // for commas
        + jsonVisitsPrefix.length
        + jsonVisitsPostfix.length
    )
    buffer.put(jsonVisitsPrefix)
    if (filtered.nonEmpty) {
      buffer.put(filtered.head)
      filtered.tail.foreach { visit =>
        buffer.put(comma)
        buffer.put(visit)
      }
    }
    buffer.put(jsonVisitsPostfix)
    buffer.array()
  }

  private val jsonVisitsPrefix = """{"visits":[""".getBytes
  private val jsonVisitsPostfix = "]}".getBytes
  private val comma = ','.toByte

  def locationAvg(location: Int,
                  fromDate: Option[Int],
                  toDate: Option[Int],
                  fromAge: Option[Int],
                  toAge: Option[Int],
                  gender: Option[Char]): Array[Byte] = {
    val found = locationVisits.get(location).fold(Seq[Int]())(
      _.toSeq.map { visitId =>
        val visit = visits(visitId)
        // proper None handling?
        val user = userDao.read(visit.user).getOrElse(deserializationError(""))
        LocationVisit(
          visit.mark,
          visit.visitedAt,
          user.age(generationDateTime),
          user.gender)
      }.withFilter(visit =>
        fromDate.fold(true)(_ < visit.visitedAt) &&
          toDate.fold(true)(_ > visit.visitedAt) &&
          fromAge.fold(true)(_ <= visit.age) &&
          toAge.fold(true)(_ > visit.age) &&
          gender.fold(true)(_ == visit.gender))
        .map(_.mark)
    )
    val (count, sum) = found.foldLeft(0.0 -> 0.0) {
      case ((c, s), mark) => c + 1 -> (s + mark)
    }
    val scale = 5
    val result =
      if (count == 0) "0.0".getBytes
      else BigDecimal(sum / count).setScale(scale, HALF_UP).toDouble.toString.getBytes
    // don't allocate each time?
    val buffer = ByteBuffer.wrap(Array.fill[Byte](
      jsonAvgPrefix.length
        + result.length // for double
        + 1 // for jsonAvgSuffix
    )(0))
    buffer.put(jsonAvgPrefix)
    buffer.put(result)
    buffer.put(jsonAvgSuffix)
    buffer.array()
  }

  private val jsonAvgPrefix = """{"avg":""".getBytes
  private val jsonAvgSuffix = '}'.toByte
}
