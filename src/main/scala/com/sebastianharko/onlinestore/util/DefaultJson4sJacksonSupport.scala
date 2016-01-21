package com.sebastianharko.onlinestore.util

import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sJacksonSupport

trait DefaultJson4sJacksonSupport extends Json4sJacksonSupport {
  override implicit def json4sJacksonFormats: Formats = DefaultFormats
}
