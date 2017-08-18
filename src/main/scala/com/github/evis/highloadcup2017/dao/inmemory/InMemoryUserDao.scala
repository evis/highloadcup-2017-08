package com.github.evis.highloadcup2017.dao.inmemory

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

import com.github.evis.highloadcup2017.dao.{NotFoundException, UserDao}
import com.github.evis.highloadcup2017.model.{User, UserUpdate}

import scala.concurrent.{ExecutionContext, Future}

class InMemoryUserDao(generationInstant: Instant) extends UserDao {
  private val users = new ConcurrentHashMap[Int, User]()

  override def create(user: User)(implicit ec: ExecutionContext): Future[Unit] = {
    users.put(user.id, user)
    Future.successful()
  }

  override def read(id: Int)(implicit ec: ExecutionContext): Future[User] = {
    users.get(id) match {
      case null => notFound(id)
      case user => Future.successful(user)
    }
  }

  override def update(id: Int, update: UserUpdate)(implicit ec: ExecutionContext): Future[Unit] = {
    users.get(id) match {
      case null =>
        notFound(id)
      case user =>
        users.put(id, user `with` update)
        Future.successful()
    }
  }

  private def notFound(id: Int) =
    Future.failed(new NotFoundException(s"User $id not found"))
}
