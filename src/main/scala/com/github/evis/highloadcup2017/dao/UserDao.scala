package com.github.evis.highloadcup2017.dao

import com.github.evis.highloadcup2017.model.User

import scala.concurrent.{ExecutionContext, Future}

trait UserDao {
  def create(user: User)(implicit ec: ExecutionContext): Future[Unit]

  def read(id: Int)(implicit ec: ExecutionContext): Future[User]

  def update(id: Int, user: User)(implicit ec: ExecutionContext): Future[Unit]
}
