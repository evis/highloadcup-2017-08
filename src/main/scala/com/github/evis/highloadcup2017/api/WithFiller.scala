package com.github.evis.highloadcup2017.api

import java.nio.ByteBuffer

trait WithFiller {
  def fill(buffer: ByteBuffer): Unit
}
