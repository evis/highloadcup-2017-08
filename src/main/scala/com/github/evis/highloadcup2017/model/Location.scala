package com.github.evis.highloadcup2017.model

import java.nio.ByteBuffer

import com.github.evis.highloadcup2017.api.WithFiller

case class Location(id: Int,
                    place: String,
                    country: String,
                    city: String,
                    distance: Int) extends Entity with WithFiller {

  def `with`(update: LocationUpdate): Location = copy(
    place = update.place.getOrElse(place),
    country = update.country.getOrElse(country),
    city = update.city.getOrElse(city),
    distance = update.distance.getOrElse(distance)
  )

  override def fill(buffer: ByteBuffer): Unit = {
    import Location._
    buffer.position(0)
    buffer.put(Id)
    buffer.put(id.toString.getBytes)
    buffer.put(Place)
    buffer.put(place.getBytes)
    buffer.put(Country)
    buffer.put(country.getBytes)
    buffer.put(City)
    buffer.put(city.getBytes)
    buffer.put(Distance)
    buffer.put(distance.toString.getBytes)
    buffer.put(End)
  }
}

object Location {
  private val Id = """{"id":""".getBytes
  private val Place = ""","place":"""".getBytes
  private val Country = """","country":"""".getBytes
  private val City = """","city":"""".getBytes
  private val Distance = """","distance":""".getBytes
  private val End = "}".getBytes
}

case class LocationUpdate(place: Option[String],
                          country: Option[String],
                          city: Option[String],
                          distance: Option[Int]) extends EntityUpdate
