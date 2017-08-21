package com.github.evis.highloadcup2017.dao

import com.github.evis.highloadcup2017.api.JsonFormats
import com.github.evis.highloadcup2017.model.{User, UserUpdate}
import spray.json._

import scala.collection.mutable

class UserDao extends JsonFormats {
  private val users = new mutable.HashMap[Int, User]()

  // is it ok to hardcode size this way?
  private val jsons = Array.fill[Array[Byte]](120000)(null)

  private var visitDao: VisitDao = _

  def create(user: User): Unit = {
    users += user.id -> user
    jsons.update(user.id, user.toJson.compactPrint.getBytes)
  }

  def read(id: Int): Option[User] = users.get(id)

  def json(id: Int): Array[Byte] = jsons(id)

  //noinspection UnitInMap
  def update(id: Int, update: UserUpdate): Option[Unit] = {
    visitDao.updateUser(id, update)
    read(id).map { user =>
      val updated = user `with` update
      users.put(id, updated)
      jsons.update(id, updated.toJson.compactPrint.getBytes)
    }
  }

  def setVisitDao(visitDao: VisitDao): Unit =
    if (this.visitDao == null) this.visitDao = visitDao
    else throw new RuntimeException("setVisitDao() should be invoked once")

  def cleanAfterPost(): Unit =
    users.clear()
}
