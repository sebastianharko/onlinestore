package com.sebastianharko.onlinestore.actors

import akka.actor.ActorRefFactory
import com.google.inject.Guice
import com.sebastianharko.onlinestore.AppModule
import com.sebastianharko.onlinestore.routes.{CheckoutRoutes, DiscountBundleRoutes, ProductRoutes}
import com.sebastianharko.onlinestore.util.DefaultJson4sJacksonSupport
import net.codingwell.scalaguice.InjectorExtensions._
import spray.routing.HttpServiceActor

class MainSprayActor extends HttpServiceActor with DefaultJson4sJacksonSupport {

  val injector = Guice.createInjector(new AppModule())

  val productRoutes = injector.instance[ProductRoutes]
  val discountBundleRoutes = injector.instance[DiscountBundleRoutes]
  val checkoutRoutes = injector.instance[CheckoutRoutes]

  val swaggerRoutes = new SwaggerActorForApiV1 {
    override implicit def actorRefFactory: ActorRefFactory = context
  }

  override def receive: Receive = {
    runRoute {
      productRoutes.route ~
        discountBundleRoutes.route ~
        checkoutRoutes.route ~
        swaggerRoutes.routes ~
        pathEndOrSingleSlash {
          getFromResource("swagger-ui/dist/index.html")
        } ~ getFromResourceDirectory("swagger-ui/dist")
    }
  }

}