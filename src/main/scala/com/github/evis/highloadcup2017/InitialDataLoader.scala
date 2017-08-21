package com.github.evis.highloadcup2017

import java.nio.file.Files

import better.files._
import com.github.evis.highloadcup2017.api.JsonFormats
import com.github.evis.highloadcup2017.dao.{LocationDao, UserDao, VisitDao}
import com.github.evis.highloadcup2017.model.{Location, User, Visit}
import spray.json._

class InitialDataLoader(userDao: UserDao, locationDao: LocationDao, visitDao: VisitDao)
  extends JsonFormats {

  def load(zipPath: String): (Int, Int, Int) = {
    File(zipPath).unzip().list.toSeq.sortBy(_.name).foreach { file =>
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
    (maxUserId, maxLocationId, maxVisitId)
  }

  private def saveUsers(jsons: Seq[JsObject]) =
    jsons.foreach { json =>
      val id = getId(json)
      if (id > maxUserId) maxUserId = id
      userDao.create(json.convertTo[User])
    }

  private def saveLocations(jsons: Seq[JsObject]) =
    jsons.foreach { json =>
      val id = getId(json)
      if (id > maxLocationId) maxLocationId = id
      locationDao.create(json.convertTo[Location])
    }

  private def saveVisits(jsons: Seq[JsObject]) =
    jsons.foreach { json =>
      val id = getId(json)
      if (id > maxVisitId) maxVisitId = id
      visitDao.create(json.convertTo[Visit])
    }

  private def getId(json: JsObject) = json.getFields("id").head.convertTo[Int]

  private var maxUserId = 0
  private var maxLocationId = 0
  private var maxVisitId = 0
}
