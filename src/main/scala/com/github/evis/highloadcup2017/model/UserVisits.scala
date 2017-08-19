package com.github.evis.highloadcup2017.model

import java.time.Instant

case class UserVisits(visits: Iterable[UserVisit])

case class UserVisit(mark: Int,
                     visitedAt: Instant,
                     place: String,
                     country: String,
                     distance: Int)

case class UserVisitsRequest(user: Int,
                             fromDate: Option[Instant],
                             toDate: Option[Instant],
                             country: Option[String],
                             toDistance: Option[Int])
