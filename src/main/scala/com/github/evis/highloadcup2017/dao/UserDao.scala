package com.github.evis.highloadcup2017.dao

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

import com.github.evis.highloadcup2017.model.{User, UserUpdate}

class UserDao(generationInstant: Instant) {
  private val users = new ConcurrentHashMap[Int, User]()

  def create(user: User): Unit =
    users.put(user.id, user)

  def read(id: Int): Option[User] =
    Option(users.get(id))

  //noinspection UnitInMap
  def update(id: Int, update: UserUpdate): Option[Unit] =
    read(id).map(user => users.put(id, user `with` update))
}
