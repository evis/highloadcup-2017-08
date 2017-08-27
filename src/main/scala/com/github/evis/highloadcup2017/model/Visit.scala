package com.github.evis.highloadcup2017.model

import java.nio.ByteBuffer

import com.github.evis.highloadcup2017.api.WithFiller

case class Visit(id: Int,
                 // should be case class fields instead of Ints?
                 location: Int,
                 user: Int,
                 visitedAt: Int,
                 mark: Int) extends Entity with WithFiller {

  def `with`(update: VisitUpdate): Visit = copy(
    location = update.location.getOrElse(location),
    user = update.user.getOrElse(user),
    visitedAt = update.visitedAt.getOrElse(visitedAt),
    mark = update.mark.getOrElse(mark)
  )

  override def fill(buffer: ByteBuffer): Unit = {
    import Visit._
    buffer.position(0)
    buffer.put(Id)
    putInt(id, buffer)
    buffer.put(Visit.Location)
    putInt(location, buffer)
    buffer.put(Visit.User)
    putInt(user, buffer)
    buffer.put(VisitedAt)
    putInt(visitedAt, buffer)
    buffer.put(Mark)
    putInt(mark, buffer)
    buffer.put(End)
  }
}

object Visit {
  private val Id = """{"id":""".getBytes
  private val Location = ""","location":""".getBytes
  private val User = ""","user":""".getBytes
  private val VisitedAt = ""","visited_at":""".getBytes
  private val Mark = ""","mark":""".getBytes
  private val End = "}".getBytes
}

case class VisitUpdate(// should be case class fields instead of Ints?
                       location: Option[Int],
                       user: Option[Int],
                       visitedAt: Option[Int],
                       mark: Option[Int]) extends EntityUpdate
