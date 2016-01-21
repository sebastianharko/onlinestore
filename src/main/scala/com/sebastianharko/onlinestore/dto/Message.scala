package com.sebastianharko.onlinestore.dto

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "a message")
case class Message(@(ApiModelProperty@field)(required = true) message: String)