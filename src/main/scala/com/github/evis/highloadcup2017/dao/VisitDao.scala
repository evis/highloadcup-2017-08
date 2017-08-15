package com.github.evis.highloadcup2017.dao

import com.github.evis.highloadcup2017.model.Visit

import scala.concurrent.Future

trait VisitDao {
  def create(visit: Visit): Future[Unit]
}
