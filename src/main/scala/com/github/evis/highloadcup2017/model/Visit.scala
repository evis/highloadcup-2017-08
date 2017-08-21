package com.github.evis.highloadcup2017.model

case class Visit(id: Int,
                 // should be case class fields instead of Ints?
                 location: Int,
                 user: Int,
                 visitedAt: Int,
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
                       visitedAt: Option[Int],
                       mark: Option[Int])
