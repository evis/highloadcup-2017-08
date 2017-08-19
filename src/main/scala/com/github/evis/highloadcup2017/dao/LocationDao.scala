package com.github.evis.highloadcup2017.dao

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

import com.github.evis.highloadcup2017.model.{Location, LocationUpdate}

class LocationDao(generationInstant: Instant) {
  private val locations = new ConcurrentHashMap[Int, Location]()

  private var visitDao: VisitDao = _

  def create(location: Location): Unit =
    locations.put(location.id, location)

  def read(id: Int): Option[Location] =
    Option(locations.get(id))

  //noinspection UnitInMap
  def update(id: Int, update: LocationUpdate): Option[Unit] = {
    visitDao.updateLocation(id, update)
    read(id).map(location => locations.put(id, location `with` update))
  }

  def setVisitDao(visitDao: VisitDao): Unit =
    if (this.visitDao != null) this.visitDao = visitDao
}
