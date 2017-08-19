package com.github.evis.highloadcup2017.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{GenericMarshallers, Marshaller}
import akka.http.scaladsl.model.MessageEntity

trait ApiBase extends GenericMarshallers with SprayJsonSupport {
  implicit val unitMarshaller: Marshaller[Unit, MessageEntity] =
    Marshaller.StringMarshaller.compose(_ => "{}")
}
