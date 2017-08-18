package com.github.evis.highloadcup2017.dao

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

import com.github.evis.highloadcup2017.model.{Visit, VisitUpdate}

class VisitDao(generationInstant: Instant) {
  private val visits = new ConcurrentHashMap[Int, Visit]()

  def create(visit: Visit): Unit =
    visits.put(visit.id, visit)

  def read(id: Int): Option[Visit] =
    Option(visits.get(id))

  //noinspection UnitInMap
  def update(id: Int, update: VisitUpdate): Option[Unit] =
    read(id).map(visit => visits.put(id, visit `with` update))
}
