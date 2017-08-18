package com.github.evis.highloadcup2017.dao

import com.github.evis.highloadcup2017.model.{User, UserUpdate}

trait UserDao {
  def create(user: User): Unit

  def read(id: Int): Option[User]

  def update(id: Int, update: UserUpdate): Option[Unit]
}
