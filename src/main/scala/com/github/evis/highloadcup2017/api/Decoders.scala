package com.github.evis.highloadcup2017.api

import colossus.protocols.http.HttpBodyDecoder
import spray.json._

import scala.util.Try

trait Decoders extends JsonFormats {

  implicit def jsonDecoder[T: JsonReader]: HttpBodyDecoder[T] = new HttpBodyDecoder[T] {
    override def decode(body: Array[Byte]): Try[T] =
      Try(implicitly[JsonReader[T]].read(new String(body).parseJson))
  }
}
