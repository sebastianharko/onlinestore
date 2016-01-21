package com.sebastianharko.onlinestore.util

import com.sebastianharko.onlinestore.dto.Message
import com.sebastianharko.onlinestore.util.ErrCode.ErrCode
import spray.http.StatusCodes

object ErrCode extends Enumeration {

  type ErrCode = Value

  val BadRequest = Value
  // 400
  // The server cannot or will not process the request due to something that is perceived
  // to be a client error (e.g., malformed request syntax,
  // invalid request message framing, or deceptive request routing)

  val Conflict = Value
  // 409
  // Indicates that the request could not be processed because of conflict in the request,
  // such as an edit conflict in the case of multiple updates.


  val NotFound = Value
  // 404
  // The requested resource could not be found but may be available again in the future.
  // Subsequent requests by the client are permissible.


  val UnknownError = Value
  // 500
  // A generic error message, given when an unexpected condition was encountered
  // and no more specific message is suitable.

}

// naming these ErrCode and ErrMsg since names such
// as ErrorCode, ErrorMessage are present in other packages
// and the IDE (IntelliJ) sometimes gets confused, and adds import
// statements for those packages - super annoying

case class ErrMsg(errCode: ErrCode, message: Option[String] = None) {

  def sprayStatusCode = {
    errCode match {
      case ErrCode.BadRequest => StatusCodes.BadRequest
      case ErrCode.Conflict => StatusCodes.Conflict
      case ErrCode.NotFound => StatusCodes.NotFound
      case ErrCode.UnknownError => StatusCodes.InternalServerError
    }
  }

  // we "box" the message attribute ( of type Option[String] )
  // into an object of type com.threetier.util.Message
  def getMessage: Message = {
    message match {
      case Some(s) => Message(s)
      case None => errCode match {
        case ErrCode.BadRequest => Message("bad request")
        case ErrCode.Conflict => Message("conflict")
        case ErrCode.NotFound => Message("not found")
        case ErrCode.UnknownError => Message("internal server error")
      }
    }
  }

}

