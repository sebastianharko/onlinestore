package com.sebastianharko.onlinestore

import com.sebastianharko.onlinestore.controllers._
import org.scalactic.Tolerance
import org.scalatest.FunSuite

class Test extends FunSuite {

  import Tolerance._
  import UsefulImplicits._

  val tolerance = 0.00005

  val checkoutOps = new CheckoutController(new DbAccessLayer)

  test("creating a Product with no description should yield exception") {
    intercept[IllegalArgumentException] {
      Product(ProductId(0), "Apple", "", Money(1))
    }
  }

  test("creating a Product with no name should yield exception") {
    intercept[IllegalArgumentException] {
      Product(ProductId(0), "", "a healthy fruit", Money(1))
    }
  }

  test("creating a Product with invalid price should yield exception") {
    intercept[IllegalArgumentException] {
      Product(ProductId(0), "Apple", "a healthy fruit", Money(0))
    }
  }

  test("creating a basket of products with invalid quantities should yield exception") {
    val apple = Product(ProductId(0), "Apple", "a healthy fruit", Money(1.0))
    val milk = Product(ProductId(1), "Milk", "a healthy drink", Money(2.0))

    intercept[IllegalArgumentException] {
      Basket(Vector(apple -> 5, milk -> -1))
    }
  }

  test("products in a basket of products have to be distinct by id") {
    val p1 = Product(ProductId(0), "Apple", "a healthy fruit", Money(1.0))
    val p2 = Product(ProductId(0), "Pear", "a healthy fruit", Money(1.0))

    intercept[IllegalArgumentException] {
      Basket(Vector(p1 -> 1, p2 -> 1))
    }
  }

  test("products in a basket need positive quantities") {
    val p1 = Product(ProductId(0), "Apple", "a healthy fruit", Money(1.0))
    val p2 = Product(ProductId(1), "Pear", "a healthy fruit", Money(1.0))

    intercept[IllegalArgumentException] {
      Basket(Vector(p1 -> 1, p2 -> -1))
    }
  }

  test("we can compute price of products in a basket (pre-discounts)") {
    import Tolerance._

    val p1 = Product(ProductId(0), "Apple", "a healthy fruit", Money(1.0))
    val p2 = Product(ProductId(1), "Pear", "a healthy fruit", Money(1.0))
    val p3 = Product(ProductId(2), "Milk", "a healthy drink", Money(5.0))
    val cart = Basket(Vector(p1 -> 5, p2 -> 5, p3 -> 3))
    assert(cart.getPrice.value === ((1 * 5.0 + 1 * 5.0 + 5 * 3.0) +- tolerance))
  }

  test("we can 'multiply' a basket") {
    val p1 = Product(ProductId(0), "Apple", "a healthy fruit", Money(1.0))
    val p2 = Product(ProductId(1), "Pear", "a healthy fruit", Money(1.0))
    val p3 = Product(ProductId(2), "Milk", "a healthy drink", Money(5.0))
    val cart = Basket(Vector(p1 -> 5, p2 -> 5, p3 -> 3))
    val m = cart * 3
    val asMap = m.productsQty: Map[Product, Int]
    assert(asMap(p1) === 15)
    assert(asMap(p2) === 15)
    assert(asMap(p3) === 9)
    assert(m.productsQty.size === 3)
  }

  test("we can 'add' two baskets together") {
    val p1 = Product(ProductId(0), "Apple", "a healthy fruit", Money(1.0))
    val p2 = Product(ProductId(1), "Pear", "a healthy fruit", Money(1.0))
    val p3 = Product(ProductId(2), "Milk", "a healthy drink", Money(5.0))
    val cart1 = Basket(Vector(p1 -> 5, p2 -> 5, p3 -> 3))

    val p4 = Product(ProductId(3), "Coca Cola", "not a healthy drink", Money(0.5))
    val cart2 = Basket(Vector(p1 -> 10, p4 -> 1))

    val combined = cart1 + cart2
    val asMap = combined.productsQty: Map[Product, Int]

    assert(asMap(p1) === 15)
    assert(asMap(p2) === 5)
    assert(asMap(p3) === 3)
    assert(asMap(p4) === 1)
    assert(combined.productsQty.size === 4)

    assert(combined.getPrice.value === (cart1.getPrice.value + cart2.getPrice.value) +- tolerance)

  }

  test("we can sometimes 'subtract' a basket from another basket") {

    val p1 = Product(ProductId(0), "Apple", "a healthy fruit", Money(1.0))
    val p2 = Product(ProductId(1), "Pear", "a healthy fruit", Money(1.0))
    val p3 = Product(ProductId(2), "Milk", "a healthy drink", Money(5.0))
    val cart1 = Basket(Vector(p1 -> 5, p2 -> 5, p3 -> 3))
    val cart2 = Basket(Vector(p1 -> 4, p2 -> 4, p3 -> 2))

    val subtract: Option[Basket] = cart1 - cart2
    assert(subtract.isDefined)
    val asMap = subtract.get.productsQty: Map[Product, Int]
    assert(asMap(p1) === 1)
    assert(asMap(p2) === 1)
    assert(asMap(p3) === 1)


  }

  test("we can't always 'subtract' a basket from another basket") {
    val p1 = Product(ProductId(0), "Apple", "a healthy fruit", Money(1.0))
    val p2 = Product(ProductId(1), "Pear", "a healthy fruit", Money(1.0))
    val p3 = Product(ProductId(2), "Milk", "a healthy drink", Money(5.0))
    val p4 = Product(ProductId(3), "Coca Cola", "not a healthy drink", Money(0.5))

    val cart1 = Basket(Vector(p1 -> 5, p2 -> 5, p3 -> 3))
    val cart2 = Basket(Vector(p1 -> 4, p2 -> 4, p3 -> 2, p4 -> 2))

    assert((cart1 - cart2).isEmpty) // Coca Cola missing from cart1 (but present in cart2) so we can't subtract

  }

  test("we can create a discount bundle") {
    val p1 = Product(ProductId(0), "Apple", "a healthy fruit", Money(1.0))
    val p2 = Product(ProductId(1), "Pear", "a healthy fruit", Money(1.0))

    // a customer can buy 5 apples and 5 pears for just $4 => $6 in savings
    val bundle = DiscountBundle(Basket(Vector(p1 -> 5, p2 -> 5)), Money(4.0))
    assert(bundle.getSavingsForCustomer.value === 6.0 +- tolerance)

  }


  test("discount applicability #1") {
    val p1 = Product(ProductId(0), "Apple", "a healthy fruit", Money(1.0))
    val p2 = Product(ProductId(1), "Pear", "a healthy fruit", Money(1.0))
    val cart = Basket(Vector(p1 -> 5, p2 -> 4))
    val bundle = DiscountBundle(Basket(Vector(p1 -> 5, p2 -> 5)), Money(4.0))
    val result = checkoutOps.getDiscountApplicability(bundle, cart)
    assert(result === 0) // discount is not applicable since the customer is short one pear
  }

  test("discount applicability #2") {
    val p1 = Product(ProductId(0), "Apple", "a healthy fruit", Money(1.0))
    val p2 = Product(ProductId(1), "Pear", "a healthy fruit", Money(1.0))
    val p4 = Product(ProductId(3), "Coca Cola", "not a healthy drink", Money(0.5))
    val cart = Basket(Vector(p1 -> 5, p2 -> 4))
    val bundle = DiscountBundle(Basket(Vector(p1 -> 5, p2 -> 5, p4 -> 1)), Money(4.0))
    val result = checkoutOps.getDiscountApplicability(bundle, cart)
    assert(result === 0) // discount is not applicable since the customer is short one product (Coca Cola)
  }

  test("discount applicability #3") {
    val p1 = Product(ProductId(0), "Apple", "a healthy fruit", Money(1.0))
    val p2 = Product(ProductId(1), "Pear", "a healthy fruit", Money(1.0))
    val cart = Basket(Vector(p1 -> 5, p2 -> 5))
    val bundle = DiscountBundle(Basket(Vector(p1 -> 5, p2 -> 5)), Money(4.0))
    val result1 = checkoutOps.getDiscountApplicability(bundle, cart)
    assert(result1 === 1) // discount is applicable once
    // if we multiply the cart 5 times , discount should be applicable 5 times
    val result2 = checkoutOps.getDiscountApplicability(bundle, cart * 5)
    assert(result2 === 5)
  }

  test("all applicable discount bundles") {
    val p1 = Product(ProductId(0), "A", "a book", Money(100))
    val p2 = Product(ProductId(1), "B", "a book", Money(50))
    val p3 = Product(ProductId(2), "C", "a book", Money(40))

    val cart = Basket(Vector(p1 -> 2, p2 -> 1, p3 -> 1))

    val bundle1 = DiscountBundle(Basket(Vector(p1 -> 2)), Money(110)) // buy a second copy of book A for just $10
    val bundle2 = DiscountBundle(Basket(Vector(p1 -> 1, p2 -> 1)), Money(120)) // when you buy book B with book A you get book B for just $20
    val bundle3 = DiscountBundle(Basket(Vector(p2 -> 1, p3 -> 1)), Money(70)) // when you buy book C with book B you get book C for just $20

    val applicable = checkoutOps.getApplicableDiscountBundles(cart, Vector(bundle1, bundle2, bundle3))
    // all bundles should be applicable (but as we shall see later only one can be actually applied)
    assert(applicable.size === 3)
  }

  test("apply some discount bundles to a cart") {

    val p1 = Product(ProductId(0), "A", "a book", Money(100))
    val p2 = Product(ProductId(1), "B", "a book", Money(50))
    val p3 = Product(ProductId(2), "C", "a book", Money(40))

    val cart = Basket(Vector(p1 -> 2, p2 -> 1, p3 -> 1))
    // regular price for this cart is 2 * 100 + 50 + 40 = 290$

    val bundle1 = DiscountBundle(Basket(Vector(p1 -> 2)), Money(110)) // buy a second copy of book A for just $10
    val bundle2 = DiscountBundle(Basket(Vector(p1 -> 1, p2 -> 1)), Money(120)) // when you buy book B with book A you get book B for just $20
    val bundle3 = DiscountBundle(Basket(Vector(p2 -> 1, p3 -> 1)), Money(70)) // when you buy book C with book B you get book C for just $20

    val result1 = checkoutOps.applyDiscountBundles(Vector(bundle1 -> 1, bundle3 -> 1), cart)
    assert(result1.isDefined)
    // with these two discount bundles, we get the two A books for $110 and book B with C  for $70, so final price is $180 with savings
    // equal to 290 - 180 = 110
    assert(result1.get.customerSavings.value === 110.0 +- tolerance)
    assert(result1.get.finalPrice.value === 180.0 +- tolerance)
    assert(result1.get.discounts.size === 2)

    val result2 = checkoutOps.applyDiscountBundles(Vector(bundle1 -> 1, bundle2 -> 1), cart)
    // "overlapping" discount bundles are not allowed
    assert(result2.isEmpty)

    val result3 = checkoutOps.applyDiscountBundles(Vector(bundle2 -> 1, bundle3 -> 1), cart)
    // again, "overlapping" discount bundles are not allowed
    assert(result3.isEmpty)
  }

  test("get the best deal for a customer") {

    val p1 = Product(ProductId(0), "A", "a book", Money(100))
    val p2 = Product(ProductId(1), "B", "a book", Money(50))
    val p3 = Product(ProductId(2), "C", "a book", Money(40))

    val cart = Basket(Vector(p1 -> 2, p2 -> 1, p3 -> 1))

    val bundle1 = DiscountBundle(Basket(Vector(p1 -> 2)), Money(110)) // buy a second copy of book A for just $10
    val bundle2 = DiscountBundle(Basket(Vector(p1 -> 1, p2 -> 1)), Money(120)) // when you buy book B with book A you get book B for just $20
    val bundle3 = DiscountBundle(Basket(Vector(p2 -> 1, p3 -> 1)), Money(70)) // when you buy book C with book B you get book C for just $20

    val result1 = checkoutOps.getBestDeal(cart, Vector(bundle1, bundle2, bundle3)).toOption.get
    // bundle1  should win
    assert(result1.customerSavings.value === 110.0 +- tolerance)
    assert(result1.finalPrice.value === 180.0 +- tolerance)
    assert(result1.discounts.size === 2) // two discounts applied

  }

  test("no deal for the customer") {
    val p1 = Product(ProductId(0), "A", "a book", Money(100))
    val p2 = Product(ProductId(1), "B", "a book", Money(50))
    val p3 = Product(ProductId(2), "C", "a book", Money(40))

    val cart = Basket(Vector(p1 -> 2, p2 -> 1, p3 -> 1))
    val result = checkoutOps.getBestDeal(cart, Vector()).toOption.get
    assert(result.customerSavings.value === 0.0 +- tolerance)

  }

}
