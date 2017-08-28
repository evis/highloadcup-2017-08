package com.github.evis.highloadcup2017.dao

import java.nio.ByteBuffer
import java.time._

import com.github.evis.highloadcup2017.api.JsonFormats
import com.github.evis.highloadcup2017.model._
import com.typesafe.scalalogging.StrictLogging

import scala.collection.mutable
import scala.math.BigDecimal.RoundingMode.HALF_UP

class VisitDao(userDao: UserDao,
               locationDao: LocationDao,
               generationDateTime: LocalDateTime) extends JsonFormats with StrictLogging with Dao[Visit] {
  private val visits = Array.fill[Visit](11000000)(null)

  // better to refactor with value types?
  // user_id -> visited_at -> visit_id
  private val userVisitsIndex =
    Array.fill[mutable.TreeMap[Int, Int]](1100000)(null)
  // location id -> visit_id
  private val locationVisits = Array.fill[mutable.Set[Int]](800000)(null)

  def create(visit: Visit): Unit = {
    // should put visit if location or user not found?
    visits.update(visit.id, visit)
    val maybeMap = userVisitsIndex(visit.user)
    val map = if (maybeMap != null) maybeMap else {
      val newMap = mutable.TreeMap[Int, Int]()
      userVisitsIndex.update(visit.user, newMap)
      newMap
    }
    map.update(visit.visitedAt, visit.id)
    val maybeSet = locationVisits(visit.location)
    val set = if (maybeSet != null) maybeSet else {
      val newSet = mutable.Set[Int]()
      locationVisits.update(visit.location, newSet)
      newSet
    }
    set.add(visit.id)
  }

  def read(id: Int): Visit = visits(id)

  //noinspection UnitInMap
  def update(id: Int, update: VisitUpdate): Unit = {
    val visit = visits(id)
    val updated = visit `with` update
    visits.update(id, updated)
    // does this method need optimizition?
    val oldUserId = visit.user
    val oldTimestamp = visit.visitedAt
    val newUserId = update.user.getOrElse(oldUserId)
    val newTimestamp = update.visitedAt.getOrElse(oldTimestamp)
      if (oldUserId != newUserId || oldTimestamp != newTimestamp) {
        userVisitsIndex(oldUserId) -= oldTimestamp
      }
      val maybeMap = userVisitsIndex(newUserId)
      val map = if (maybeMap != null) maybeMap else {
        val newMap = mutable.TreeMap[Int, Int]()
        userVisitsIndex.update(newUserId, newMap)
        newMap
      }
      map.update(newTimestamp, id)
    val oldLocationId = visit.location
    val oldLocationVisits = locationVisits(oldLocationId)
    val newLocationId = update.location.getOrElse(oldLocationId)
    if (oldLocationId != newLocationId) {
      oldLocationVisits.remove(id)
    }
    val maybeSet = locationVisits(newLocationId)
    val set = if (maybeSet != null) maybeSet else {
      val newSet = mutable.Set[Int]()
      locationVisits.update(newLocationId, newSet)
      newSet
    }
    set.add(id)
  }

  def userVisits(user: Int,
                 fromDate: Option[Int],
                 toDate: Option[Int],
                 country: Option[String],
                 toDistance: Option[Int]): Iterable[UserVisit] = {
    // optimize?
    Option(userVisitsIndex(user)).fold(Iterable[UserVisit]())(
      _.rangeImpl(fromDate, toDate).values.map { visitId =>
          val visit = visits(visitId)
          val location = locationDao.read(visit.location)
          UserVisit(
            visit.mark,
            visit.visitedAt,
            location.place,
            location.country,
            location.distance)
        }.filter(userVisit =>
          toDistance.fold(true)(_ > userVisit.distance) &&
            country.fold(true)(_ == userVisit.country)
        )
    )
  }

  def locationAvg(location: Int,
                  fromDate: Option[Int],
                  toDate: Option[Int],
                  fromAge: Option[Int],
                  toAge: Option[Int],
                  gender: Option[Char]): Array[Byte] = {
    val found = Option(locationVisits(location)).fold(Seq[Int]())(
      _.toSeq.map { visitId =>
        val visit = visits(visitId)
        // proper None handling?
        val user = userDao.read(visit.user)
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
