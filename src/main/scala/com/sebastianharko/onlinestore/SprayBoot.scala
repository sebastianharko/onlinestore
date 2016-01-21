package com.sebastianharko.onlinestore

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.sebastianharko.onlinestore.actors.MainSprayActor
import spray.can.Http

object SprayBoot extends App {

  implicit val system = ActorSystem("actor-system")

  val api = system.actorOf(Props[MainSprayActor], "api-actor")

  IO(Http) ! Http.Bind(listener = api,
    interface = "0.0.0.0",
    port = 8080)

}