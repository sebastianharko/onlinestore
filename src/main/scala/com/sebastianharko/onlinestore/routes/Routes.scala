package com.sebastianharko.onlinestore.routes

import com.google.inject.Inject
import com.sebastianharko.onlinestore.controllers._
import com.sebastianharko.onlinestore.dto.Message
import com.sebastianharko.onlinestore.util.DefaultJson4sJacksonSupport
import com.wordnik.swagger.annotations._
import spray.routing.HttpService._
import spray.routing.Route

import scalaz.{-\/, \/-}

case class ProductIdQty(productId: Int, qty: Int)

case class OnlineCart(items: List[ProductIdQty])

@Api(value = "/checkout", description = "checkout operation (gets the best deal possible for the customer)")
class CheckoutRoutes @Inject()(val checkoutController: CheckoutController) extends DefaultJson4sJacksonSupport {

  val postRoute: Route = post {
    path("checkout") {
      entity(as[OnlineCart]) {
        onlineCart => {
          complete {
            checkoutController.getBestDeal(onlineCart) match {
              case \/-(value) => value
              case -\/(error) => error.sprayStatusCode -> error.getMessage
            }
          }
        }
      }
    }
  }

  @ApiOperation(value = "get a list of discount bundles",
    notes = "may return an error in case the database is down", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "cart", required = true, paramType = "body", dataType = "com.sebastianharko.onlinestore.routes.OnlineCart")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[Message]),
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 401, message = "Unauthorized"),
    new ApiResponse(code = 500, message = "Internal Server Error")))
  def route = postRoute

}


@Api(value = "/discounts", description = "operation related to discount bundles")
class DiscountBundleRoutes @Inject()(val dbAccessLayer: DbAccessLayer) extends DefaultJson4sJacksonSupport {

  val getRoute: Route = get {
    path("discounts") {
      complete {
        dbAccessLayer.getDiscountBundles match {
          case \/-(value) => value
          case -\/(error) => error.sprayStatusCode -> error.getMessage
        }
      }
    }
  }

  @ApiOperation(value = "get a list of discount bundles",
    notes = "may return an error in case the database is down", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK"),
    new ApiResponse(code = 500, message = "Internal Server Error")))
  def route = getRoute

}

@Api(value = "/products", description = "operations related to products")
class ProductRoutes @Inject()(val dbAccessLayer: DbAccessLayer) extends DefaultJson4sJacksonSupport {

  val getRoute: Route = get {
    path("products") {
      complete {
        dbAccessLayer.getProducts match {
          case \/-(value) => value
          case -\/(error) => error.sprayStatusCode -> error.getMessage
        }
      }
    }
  }

  @ApiOperation(value = "get a list of products",
    notes = "may return an error in case the database is down", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[Message]),
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 401, message = "Unauthorized"),
    new ApiResponse(code = 500, message = "Internal Server Error")))
  def route = getRoute


}
