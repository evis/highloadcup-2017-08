package com.github.evis.highloadcup2017.dao

import com.github.evis.highloadcup2017.model.Location

import scala.concurrent.Future

trait LocationDao {
  def create(location: Location): Future[Unit]
}
