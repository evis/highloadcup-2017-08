package com.github.evis.highloadcup2017.dao.inmemory

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

import com.github.evis.highloadcup2017.dao.UserDao
import com.github.evis.highloadcup2017.model.{User, UserUpdate}

class InMemoryUserDao(generationInstant: Instant) extends UserDao {
  private val users = new ConcurrentHashMap[Int, User]()

  override def create(user: User): Unit =
    users.put(user.id, user)

  override def read(id: Int): Option[User] =
    Option(users.get(id))

  //noinspection UnitInMap
  override def update(id: Int, update: UserUpdate): Option[Unit] =
    Option(users.get(id)).map(_ `with` update)
}
