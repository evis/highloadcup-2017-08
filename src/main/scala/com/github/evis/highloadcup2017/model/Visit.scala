package com.github.evis.highloadcup2017.model

import java.time.Instant

case class Visit(id: Int,
                 // should be case class fields instead of Ints?
                 location: Int,
                 user: Int,
                 visitedAt: Instant,
                 mark: Int) {

  def `with`(update: VisitUpdate): Visit = copy(
    location = update.location.getOrElse(location),
    user = update.user.getOrElse(user),
    visitedAt = update.visitedAt.getOrElse(visitedAt),
    mark = update.mark.getOrElse(mark)
  )
}

case class VisitUpdate(// should be case class fields instead of Ints?
                       location: Option[Int],
                       user: Option[Int],
                       visitedAt: Option[Instant],
                       mark: Option[Int])
