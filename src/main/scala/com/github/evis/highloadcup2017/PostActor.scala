package com.github.evis.highloadcup2017

import akka.actor.{Actor, ActorSystem, PoisonPill}
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import com.github.evis.highloadcup2017.dao.{LocationDao, UserDao, VisitDao}
import com.github.evis.highloadcup2017.model.{Location, LocationUpdate, User, UserUpdate, Visit, VisitUpdate}
import com.typesafe.config.Config

class PostActor(userDao: UserDao, locationDao: LocationDao, visitDao: VisitDao) extends Actor {
  override def receive: Receive = {
    case user: User =>
      userDao.create(user)
    case location: Location =>
      locationDao.create(location)
    case visit: Visit =>
      visitDao.create(visit)
    case (id: Int, update) => update match {
      case uu: UserUpdate =>
        userDao.update(id, uu)
      case lu: LocationUpdate =>
        locationDao.update(id, lu)
      case vu: VisitUpdate =>
        visitDao.update(id, vu)
      case PoisonPill =>
        userDao.cleanAfterPost()
        locationDao.cleanAfterPost()
        visitDao.cleanAfterPost()
    }
  }
}

class PostActorPriorityMailbox(settings: ActorSystem.Settings, config: Config)
  extends UnboundedStablePriorityMailbox(
    PriorityGenerator {
      case _: User => 0
      case _: Visit => 0
      case _: Location => 0
      case PoisonPill => 2
      case _ => 1
    }
  )
