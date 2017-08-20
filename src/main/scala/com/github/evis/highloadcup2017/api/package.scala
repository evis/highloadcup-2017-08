package com.github.evis.highloadcup2017

import colossus.protocols.http.{ParameterParser, QueryParameters}

import scala.util.{Success, Try}

package object api {

  implicit class RichQueryParameters(params: QueryParameters) {
    def optParam[T](key: String)(implicit parser: ParameterParser[T]): Try[Option[T]] =
      params.getFirst(key).fold(Success(None): Try[Option[T]])(parser.parse(_).map(Some(_)))
  }

}
