package com.github.evis.highloadcup2017.model

case class Visit(id: Int,
                 // should be case class fields instead of Ints?
                 location: Int,
                 user: Int,
                 visitedAt: Int,
                 mark: Int)
