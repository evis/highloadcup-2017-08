package com.github.evis.highloadcup2017.api

import java.time.Instant

import colossus.protocols.http.ParameterParser

import scala.util.{Failure, Try}

trait Parsers {

  implicit val instantParser: ParameterParser[Instant] = new ParameterParser[Instant] {
    override def parse(value: String): Try[Instant] =
      Try(value.toLong).map(Instant.ofEpochSecond)
  }

  implicit val genderParser: ParameterParser[Char] = new ParameterParser[Char] {
    override def parse(value: String): Try[Char] = value.length match {
      case 1 => Try(value.head).filter(c => c == 'm' || c == 'f')
      case 0 => Failure(new Exception("Unable to get char from empty string"))
      case _ => Failure(new Exception("Expected one char"))
    }
  }
}
