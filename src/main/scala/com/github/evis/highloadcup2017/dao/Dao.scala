package com.github.evis.highloadcup2017.dao

import com.github.evis.highloadcup2017.api.WithFiller

trait Dao[T <: WithFiller] {
  def read(id: Int): T
}
