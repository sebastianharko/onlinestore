package com.sebastianharko.onlinestore.controllers

import com.sebastianharko.onlinestore.util.ErrMsg

import scalaz.\/

class DbAccessLayer {

  import UsefulImplicits._

  private val p1 = Product(ProductId(0), "Breaking Bad", "a DVD", Money(100))

  private val p2 = Product(ProductId(1), "Better call Saul", "a DVD", Money(50))

  private val p3 = Product(ProductId(2), "House of Cards", "a DVD", Money(40))

  private val bundle1 = DiscountBundle(Basket(Vector(p1 -> 2)),
    Money(110),
    description = Some("buy two copies of the 'Breaking Bad' DVD for just $110 (save $90)"))

  private val bundle2 = DiscountBundle(Basket(Vector(p1 -> 1, p2 -> 1)),
    Money(120),
    description = Some("buy 'Better call Saul' AND 'Breaking Bad' for just $120 (you save $30)")
  )

  private val bundle3 = DiscountBundle(Basket(Vector(p2 -> 1, p3 -> 1)), Money(70),
    description = Some("buy 'Better call Saul' AND 'House of Cards' for just $70 (you save $20)"))

  private val bundles = List(bundle1, bundle2, bundle3)

  private val products = List(p1, p2, p3)


  def getProducts: ErrMsg \/ List[Product] = {
    \/.right(products)
  }

  def getDiscountBundles: ErrMsg \/ List[DiscountBundle] = {
    \/.right(bundles)
  }

  def getProductById(id: ProductId): Product = {
    products.find(_.id == id).get
    // error handling not implemented yet here :-)
  }


}

