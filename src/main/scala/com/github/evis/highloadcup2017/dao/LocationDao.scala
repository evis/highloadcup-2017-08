package com.github.evis.highloadcup2017.dao

import com.github.evis.highloadcup2017.api.JsonFormats
import com.github.evis.highloadcup2017.model.{Location, LocationUpdate}
import spray.json._

import scala.collection.mutable

class LocationDao extends JsonFormats with Dao {
  private val locations = new mutable.HashMap[Int, Location]()

  // is it ok to hardcode size this way?
  private val jsons = Array.fill[Array[Byte]](100000)(null)

  private var visitDao: VisitDao = _

  def create(location: Location): Unit = {
    locations += location.id -> location
    jsons.update(location.id, location.toJson.compactPrint.getBytes)
  }

  def read(id: Int): Option[Location] = locations.get(id)

  override def json(id: Int): Array[Byte] = jsons(id)

  //noinspection UnitInMap
  def update(id: Int, update: LocationUpdate): Option[Unit] = {
    visitDao.updateLocation(id, update)
    read(id).map { location =>
      val updated = location `with` update
      locations.put(id, updated)
      jsons.update(id, updated.toJson.compactPrint.getBytes)
    }
  }

  def setVisitDao(visitDao: VisitDao): Unit =
    if (this.visitDao == null) this.visitDao = visitDao
    else throw new RuntimeException("setVisitDao() should be invoked once")

  def cleanAfterPost(): Unit = {
    locations.clear()
  }
}
