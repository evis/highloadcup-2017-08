package com.github.evis.highloadcup2017

import scala.language.implicitConversions

package object model {

  implicit def intToUserVisit(int: Option[Int]): Option[UserVisit] =
    int.map(UserVisit(-1, -1, -1, _, null, null, -1))

  implicit def intToLocationVisit(int: Option[Int]): Option[LocationVisit] =
    int.map(LocationVisit(-1, -1, -1, _, -1, 'g'))

  implicit val userVisitOrdering: Ordering[UserVisit] =
    Ordering.by(_.visitedAt)

  implicit val locationVisitOrdering: Ordering[LocationVisit] =
    Ordering.by(_.visitedAt)
}
