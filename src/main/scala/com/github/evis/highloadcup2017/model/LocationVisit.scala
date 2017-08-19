package com.github.evis.highloadcup2017.model

import java.time.Instant

case class LocationVisit(userId: Int,
                         visitId: Int,
                         mark: Int,
                         visitedAt: Instant,
                         age: Int,
                         gender: Gender)

case class LocationAvgRequest(location: Int,
                              fromDate: Option[Instant],
                              toDate: Option[Instant],
                              fromAge: Option[Int],
                              toAge: Option[Int],
                              gender: Option[Gender])

case class LocationAvgResponse(avg: Double)
