package com.sebastianharko.onlinestore.actors

import com.gettyimages.spray.swagger.SwaggerHttpService
import com.sebastianharko.onlinestore.routes.{CheckoutRoutes, DiscountBundleRoutes, ProductRoutes}
import com.wordnik.swagger.model.ApiInfo

import scala.reflect.runtime.universe._

trait SwaggerActorForApiV1 extends SwaggerHttpService {

  override def apiTypes = Seq(typeOf[ProductRoutes], typeOf[DiscountBundleRoutes], typeOf[CheckoutRoutes])

  override def apiVersion: String = "1.0"

  override def baseUrl: String = "/" // let swagger-ui determine the host and port

  override def docsPath = "api-docs"

  override def apiInfo = Some(new ApiInfo(
    "Simple API",
    ":-).",
    "Some Terms of Service",
    "Sebastian Harko sebastian.harko.bc@gmail.com",
    "Some License",
    "http://choosealicense.com/"))

}
