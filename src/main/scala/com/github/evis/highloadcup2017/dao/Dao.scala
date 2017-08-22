package com.github.evis.highloadcup2017.dao

trait Dao {
  def json(id: Int): Array[Byte]
}
