package com.github.evis.highloadcup2017.dao

import com.github.evis.highloadcup2017.api.JsonFormats
import com.github.evis.highloadcup2017.model.{User, UserUpdate}

class UserDao extends JsonFormats with Dao[User] {
  private val users = Array.fill[User](1100000)(null)

  private var visitDao: VisitDao = _

  def create(user: User): Unit =
    users.update(user.id, user)

  def read(id: Int): User = users(id)

  //noinspection UnitInMap
  def update(id: Int, update: UserUpdate): Unit = {
    val user = users(id)
    val updated = user `with` update
    users.update(id, updated)
  }

  def setVisitDao(visitDao: VisitDao): Unit =
    if (this.visitDao == null) this.visitDao = visitDao
    else throw new RuntimeException("setVisitDao() should be invoked once")
}
