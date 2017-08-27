package com.github.evis.highloadcup2017.api

import java.nio.ByteBuffer

trait WithFiller {
  def fill(buffer: ByteBuffer): Unit

  protected def putInt(i: Int, buffer: ByteBuffer): Unit = {
    if (i < 0) buffer.put('-'.toByte)
    val abs = if (i > 0) i else -i

    def loop(n: Int): Unit = {
      val curr = n % 10
      val next = n / 10
      if (next != 0) {
        loop(next)
      }
      buffer.put(('0' + curr).toByte)
    }

    loop(abs)
  }

  protected def putString(s: String, buffer: ByteBuffer): Unit = {
    s.foreach { c =>
      if (c >= 'А' && c <= 'п') {
        buffer.put((-48).toByte)
        buffer.put((-112 + (c - 'А')).toByte)
      } else if (c >= 'р' && c <= 'я') {
        buffer.put((-47).toByte)
        buffer.put((-128 + (c - 'р')).toByte)
      } else if (c == 'Ё') {
        buffer.put((-48).toByte)
        buffer.put((-127).toByte)
      } else if (c == 'ё') {
        buffer.put((-47).toByte)
        buffer.put((-111).toByte)
      } else buffer.put(c.toByte)
    }
  }
}
