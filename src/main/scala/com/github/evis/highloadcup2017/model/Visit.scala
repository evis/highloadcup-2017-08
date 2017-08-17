package com.github.evis.highloadcup2017.model

import java.time.Instant

case class Visit(id: Int,
                 // should be case class fields instead of Ints?
                 location: Int,
                 user: Int,
                 visitedAt: Instant,
                 mark: Int)
