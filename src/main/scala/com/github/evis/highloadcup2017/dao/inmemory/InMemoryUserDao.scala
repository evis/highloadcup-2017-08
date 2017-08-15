package com.github.evis.highloadcup2017.dao.inmemory

import java.util.concurrent.ConcurrentHashMap

import com.github.evis.highloadcup2017.dao.{NotFoundException, UserDao}
import com.github.evis.highloadcup2017.model.User

import scala.concurrent.{ExecutionContext, Future}

class InMemoryUserDao extends UserDao {
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

  override def update(id: Int, user: User)(implicit ec: ExecutionContext): Future[Unit] = {
    users.get(id) match {
      case null =>
        notFound(id)
      case _ =>
        users.put(id, user)
        Future.successful()
    }
  }

  private def notFound(id: Int) =
    Future.failed(new NotFoundException(s"User $id not found"))
}
