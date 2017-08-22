package com.github.evis.highloadcup2017.model

case class Location(id: Int,
                    place: String,
                    country: String,
                    city: String,
                    distance: Int) extends Entity {

  def `with`(update: LocationUpdate): Location = copy(
    place = update.place.getOrElse(place),
    country = update.country.getOrElse(country),
    city = update.city.getOrElse(city),
    distance = update.distance.getOrElse(distance)
  )
}

case class LocationUpdate(place: Option[String],
                          country: Option[String],
                          city: Option[String],
                          distance: Option[Int]) extends EntityUpdate
