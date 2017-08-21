package com.github.evis.highloadcup2017.dao

import com.github.evis.highloadcup2017.model.{Location, LocationUpdate}

import scala.collection.mutable

class LocationDao {
  private val locations = new mutable.HashMap[Int, Location]()

  private var visitDao: VisitDao = _

  def create(location: Location): Unit =
    locations += location.id -> location

  def read(id: Int): Option[Location] = locations.get(id)

  //noinspection UnitInMap
  def update(id: Int, update: LocationUpdate): Option[Unit] = {
    visitDao.updateLocation(id, update)
    read(id).map(location => locations.put(id, location `with` update))
  }

  def setVisitDao(visitDao: VisitDao): Unit =
    if (this.visitDao == null) this.visitDao = visitDao
    else throw new RuntimeException("setVisitDao() should be invoked once")
}
