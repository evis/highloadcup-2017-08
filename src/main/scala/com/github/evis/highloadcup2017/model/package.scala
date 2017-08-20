package com.github.evis.highloadcup2017

import java.time.Instant

import scala.language.implicitConversions

package object model {

  implicit def instantToUserVisit(instant: Option[Instant]): Option[UserVisit] =
    instant.map(UserVisit(-1, -1, -1, _, null, null, -1))

  implicit def instantToLocationVisit(instant: Option[Instant]): Option[LocationVisit] =
    instant.map(LocationVisit(-1, -1, -1, _, -1, 'g'))

  implicit val userVisitOrdering: Ordering[UserVisit] =
    Ordering.by(_.visitedAt)

  implicit val locationVisitOrdering: Ordering[LocationVisit] =
    Ordering.by(_.visitedAt)
}
