package com.github.evis.highloadcup2017.model

case class UserVisit(mark: Int,
                     visitedAt: Int,
                     place: String,
                     country: String,
                     distance: Int) {

  def json: Array[Byte] = UserVisit.genJson(mark, visitedAt, place)
}

object UserVisit {
  def genJson(mark: Int, visitedAt: Int, place: String): Array[Byte] = {
    val builder = new StringBuilder
    builder.append("{\"mark\":")
    builder.append(mark)
    builder.append(",\"visited_at\":")
    builder.append(visitedAt)
    builder.append(",\"place\":\"")
    builder.append(place)
    builder.append("\"}")
    builder.result().getBytes
  }
}
