package com.github.evis.highloadcup2017.dao

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

import com.github.evis.highloadcup2017.model.{Location, LocationUpdate}

class LocationDao(generationInstant: Instant) {
  private val locations = new ConcurrentHashMap[Int, Location]()

  def create(location: Location): Unit =
    locations.put(location.id, location)

  def read(id: Int): Option[Location] =
    Option(locations.get(id))

  //noinspection UnitInMap
  def update(id: Int, update: LocationUpdate): Option[Unit] =
    read(id).map(location => locations.put(id, location `with` update))
}
