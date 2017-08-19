package com.github.evis.highloadcup2017.dao

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

import com.github.evis.highloadcup2017.model.{User, UserUpdate}

class UserDao(generationInstant: Instant) {
  private val users = new ConcurrentHashMap[Int, User]()

  private var visitDao: VisitDao = _

  def create(user: User): Unit =
    users.put(user.id, user)

  def read(id: Int): Option[User] =
    Option(users.get(id))

  //noinspection UnitInMap
  def update(id: Int, update: UserUpdate): Option[Unit] = {
    visitDao.updateUser(id, update)
    read(id).map(user => users.put(id, user `with` update))
  }

  def setVisitDao(visitDao: VisitDao): Unit =
    if (this.visitDao == null) this.visitDao = visitDao
    else throw new RuntimeException("setVisitDao() should be invoked once")
}
