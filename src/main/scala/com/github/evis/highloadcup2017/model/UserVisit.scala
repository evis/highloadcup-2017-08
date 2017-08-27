package com.github.evis.highloadcup2017.model

import java.nio.ByteBuffer

import com.github.evis.highloadcup2017.api.WithFiller

case class UserVisit(mark: Int,
                     visitedAt: Int,
                     place: String,
                     country: String,
                     distance: Int) extends WithFiller {

  override def fill(buffer: ByteBuffer): Unit = {
    import UserVisit._
    buffer.put(Mark)
    putInt(mark, buffer)
    buffer.put(VisitedAt)
    putInt(visitedAt, buffer)
    buffer.put(Place)
    putString(place, buffer)
    buffer.put(End)
  }
}

object UserVisit {
  private val Mark = "{\"mark\":".getBytes
  private val VisitedAt = ",\"visited_at\":".getBytes
  private val Place = ",\"place\":\"".getBytes
  private val End = "\"}".getBytes
}
