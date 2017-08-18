package com.github.evis.highloadcup2017

import java.nio.file.Files

import better.files._
import com.github.evis.highloadcup2017.dao.UserDao
import com.github.evis.highloadcup2017.model.User
import spray.json._

class InitialDataLoader(userDao: UserDao) {
  def load(zipPath: String): Unit =
    File(zipPath).unzip().list.foreach { file =>
      // streaming?
      val string = new String(Files.readAllBytes(file.path))
      val (entityType, entitiesValue) = string.parseJson.asJsObject.fields.head
      val entities = entitiesValue match {
        case JsArray(elements) => elements
        case other => throw new Exception(s"Bad entities. Expected JsArray, got $other")
      }
      val saveFun = entityType match {
        case "users" => saveUsers _
        case "locations" => saveLocations _
        case "visits" => saveVisits _
        case other => throw new Exception(s"Unknown entity type: $other")
      }
      saveFun(entities.map(_.asJsObject))
    }

  private def saveUsers(jsons: Seq[JsObject]) =
    jsons.foreach(json => userDao.create(json.convertTo[User]))

  private def saveLocations(jsons: Seq[JsObject]) {}

  private def saveVisits(jsons: Seq[JsObject]) {}
}
