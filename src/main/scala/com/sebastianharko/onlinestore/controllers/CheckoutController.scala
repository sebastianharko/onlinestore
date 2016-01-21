package com.sebastianharko.onlinestore.controllers

import com.google.inject.Inject
import com.sebastianharko.onlinestore.routes.OnlineCart
import com.sebastianharko.onlinestore.util.ErrMsg

import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import scalaz.Scalaz._
import scalaz.\/

// we extends AnyVal to add some typesafety, learned about this
// from a BoldRadius YouTube video
case class ProductId(value: Int) extends AnyVal

case class Money(value: Double) extends AnyVal

case class Product(id: ProductId, name: String, description: String, price: Money) {
  require(name.length > 0, "name has to be valid")
  require(description.length > 0, "description has to be valid")
  require(price.value > 0, "price has to be greater than zero")
}

// represents a product taken in a quantity
case class ProductQty(product: Product, qty: Int)


// Some useful implicit here, mostly because sometimes I like to
// view a basket as a hash-map where the key is the product and the
// qty is the value ; sometimes I like to use tuples to represent
// products quantities e.g., p1 -> 5 instead ProductQty(p1, 5)
object UsefulImplicits {

  implicit def tuple2ProductQty(v: (Product, Int)): ProductQty = {
    ProductQty(v._1, v._2)
  }

  implicit def productQty2Tuple(v: ProductQty): (Product, Int) = {
    v.product -> v.qty
  }

  implicit def vectorProductQty2Map(v: Vector[ProductQty]): Map[Product, Int] = {
    v.map(f => f: (Product, Int)).toMap // uses conversion defined above
  }

  implicit def map2Vector(v: Map[Product, Int]): Vector[ProductQty] = {
    v.toVector.map(item => item: ProductQty) // uses conversion defined above
  }

}


// a basket of products (or a cart)
case class Basket(productsQty: Vector[ProductQty]) {

  import UsefulImplicits._

  // just a small check here to check if this is a valid basket
  // distinct products required
  require({
    productsQty.size == productsQty.map(_.product).map(_.id).map(_.value).distinct.size
  }, "distinct products (distinct by product id) required")

  require(!productsQty.exists(p => p.qty < 0), "quantities have to be positive")

  // get regular price (with no discounts applied at all)
  def getPrice = Money {
    productsQty.map(item => item.product.price.value * item.qty).sum
  }

  // multiply a basket
  def *(times: Int) = {
    val newProductsQty: Vector[ProductQty] = productsQty.map(i => ProductQty(i.product, i.qty * times))
    Basket(newProductsQty)
  }

  // add two baskets into one basket
  def +(other: Basket) = {
    val m1: Map[Product, Int] = this.productsQty
    val m2: Map[Product, Int] = other.productsQty
    Basket(m1 |+| m2)
  }

  // subtract a basket B from another basket A
  // can only be done if B is a subset of A
  def -(other: Basket): Option[Basket] = {
    val r = Try {
      val m1: Map[Product, Int] = this.productsQty
      val m2: Map[Product, Int] = other.productsQty.mapValues(i => -i)
      Basket(m1 |+| m2)
    }.toOption
    r
  }

}

// it's convenient to model a "discount bundle" as basket of products all sold together at a discounted price
case class DiscountBundle(basket: Basket, price: Money, description: Option[String] = None) {

  require(getSavingsForCustomer.value >= 0, "this bundle has to provide the customer a discount or at least the appearance of a discount:-)")

  def getSavingsForCustomer = {
    Money(basket.getPrice.value - price.value)
  }

  def *(times: Int): DiscountBundle = {
    DiscountBundle(basket * times, Money(price.value * times))
  }

  def +(other: DiscountBundle): DiscountBundle = {
    DiscountBundle(basket + other.basket, Money(price.value + other.price.value))
  }


}

case class AppliedDiscount(discount: DiscountBundle, timesApplied: Int)

// this represents note we send the customer that contains a list of all the discounts applied, the final price
// for the cart and the amount of money we managed to help the customer save
case class CheckoutNote(discounts: Vector[AppliedDiscount], finalPrice: Money, customerSavings: Money)


class CheckoutController @Inject()(val db: DbAccessLayer) {

  def getBestDeal(onlineCart: OnlineCart): ErrMsg \/ CheckoutNote = {
    import UsefulImplicits._
    val prods: Vector[ProductQty] = onlineCart.items.map(item => db.getProductById(ProductId(item.productId)) -> item.qty)
      .map((item: (Product, Int)) => item: ProductQty)
      .toVector

    getBestDeal(Basket(prods))

  }

  def getBestDeal(customerBasket: Basket): ErrMsg \/ CheckoutNote = {
    db.getDiscountBundles flatMap {
      bundles =>
        getBestDeal(customerBasket, bundles.toVector)
    }
  }

  // the customer has provided a basket of goods (or a shopping cart)
  // we are also given the list of *all* "discount bundles" available in our database
  // we return a checkout note that contains a list of all the discounts applied, the final price
  // for the cart and the amount of money we managed to help the customer save
  def getBestDeal(customerBasket: Basket, allDiscountBundles: Vector[DiscountBundle]): ErrMsg \/ CheckoutNote = {
    // first step is to filter out the bundles that the customer does not even qualify for
    val applicableBundles: Vector[(DiscountBundle, Int)] = getApplicableDiscountBundles(customerBasket, allDiscountBundles)

    val solutionSpace = ArrayBuffer[Vector[(DiscountBundle, Int)]]()

    def buildSolutionSpace(i: Int, j: Int, solution: Array[(DiscountBundle, Int)]): Unit = {
      if (i == applicableBundles.length) {
        solutionSpace += solution.toVector
      } else {
        if (j <= applicableBundles(i)._2) {
          solution(i) = (applicableBundles(i)._1, j)
          buildSolutionSpace(i + 1, 0, solution)
          buildSolutionSpace(i, j + 1, solution)
        }
      }
    }

    buildSolutionSpace(0, 0, Array.ofDim[(DiscountBundle, Int)](applicableBundles.length))

    // needed by the maxBy method
    implicit object MoneyOrdering extends scala.Ordering[Money] {
      def compare(x: Money, y: Money): Int = x.value.compare(y.value)
    }

    // go through every possible solution and pick the best one (maximum savings for the customer)
    val result: CheckoutNote = solutionSpace
      .map(possibleSolution => applyDiscountBundles(possibleSolution, customerBasket))
      .filter(_.isDefined)
      .map(_.get)
      .maxBy(_.customerSavings)

    \/.right(result)
  }

  // get all discount bundles that can be potentially applied to a customer basket
  // for each discount bundle, we also return the maximum times that it can be applied
  // to the customer's basket
  def getApplicableDiscountBundles(customerBasket: Basket, allDiscountBundles: Vector[DiscountBundle]): Vector[(DiscountBundle, Int)] = {
    allDiscountBundles.map(bundle => (bundle, getDiscountApplicability(bundle, customerBasket))).filter(_._2 > 0)
  }

  // is a particular discount bundle applicable to this user's shopping cart ?
  // if so, how many times can the discount be applied to the user's shopping cart
  // (provided that no other type of discount gets applied) ?
  // returns 0 if the discount is not applicable
  def getDiscountApplicability(discountBundle: DiscountBundle, customerBasket: Basket): Int = {
    val result = discountBundle.basket.productsQty.map {
      p => customerBasket.productsQty.find(item => item.product.id == p.product.id).map(_.qty / p.qty).getOrElse(0)
    }.min
    result
  }

  // apply a set of discount bundles to a customer basket
  // if the set of discount bundles is invalid (can't be applied to this basket), we return None else we return some checkout note
  // to the customer that contains information such as how much money we saved them etc.
  def applyDiscountBundles(bundles: Vector[(DiscountBundle, Int)], customerBasket: Basket): Option[CheckoutNote] = {
    val emptyDiscountBundle = DiscountBundle(Basket(Vector.empty[ProductQty]), Money(0))
    val discountBundleSum = bundles.map(item => item._1 * item._2).foldLeft(emptyDiscountBundle)(_ + _)
    val r: Option[Basket] = customerBasket - discountBundleSum.basket
    r match {
      case None => None
      case Some(_) => Some(CheckoutNote(bundles.filterNot(_._2 == 0).map(i => AppliedDiscount(i._1, i._2)), Money(customerBasket.getPrice.value - discountBundleSum.getSavingsForCustomer.value), discountBundleSum.getSavingsForCustomer))
    }
  }
}
