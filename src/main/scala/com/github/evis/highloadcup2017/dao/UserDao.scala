package com.github.evis.highloadcup2017.dao

import com.github.evis.highloadcup2017.api.JsonFormats
import com.github.evis.highloadcup2017.model.{User, UserUpdate}
import spray.json._

import scala.collection.mutable

class UserDao extends JsonFormats with Dao {
  private val users = new mutable.HashMap[Int, User]()

  private var visitDao: VisitDao = _

  def create(user: User): Unit =
    users += user.id -> user

  def read(id: Int): Option[User] = users.get(id)

  def json(id: Int): Array[Byte] = users(id).toJson.compactPrint.getBytes

  //noinspection UnitInMap
  def update(id: Int, update: UserUpdate): Option[Unit] = {
    read(id).map { user =>
      val updated = user `with` update
      users.put(id, updated)
    }
  }

  def setVisitDao(visitDao: VisitDao): Unit =
    if (this.visitDao == null) this.visitDao = visitDao
    else throw new RuntimeException("setVisitDao() should be invoked once")
}
