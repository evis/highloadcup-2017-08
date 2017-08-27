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
    buffer.put(id.toString.getBytes)
    buffer.put(Visit.Location)
    buffer.put(location.toString.getBytes)
    buffer.put(Visit.User)
    buffer.put(user.toString.getBytes)
    buffer.put(VisitedAt)
    buffer.put(visitedAt.toString.getBytes)
    buffer.put(Mark)
    buffer.put(mark.toString.getBytes)
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
