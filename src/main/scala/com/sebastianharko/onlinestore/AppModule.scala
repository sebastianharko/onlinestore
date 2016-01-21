package com.sebastianharko.onlinestore

import com.google.inject.AbstractModule
import com.sebastianharko.onlinestore.controllers.DbAccessLayer
import com.sebastianharko.onlinestore.routes.{CheckoutRoutes, DiscountBundleRoutes, ProductRoutes}
import net.codingwell.scalaguice.ScalaModule

class AppModule extends AbstractModule with ScalaModule {

  def configure(): Unit = {

    bind[DbAccessLayer].asEagerSingleton()
    bind[CheckoutRoutes].asEagerSingleton()
    bind[ProductRoutes].asEagerSingleton()
    bind[DiscountBundleRoutes].asEagerSingleton()

  }

}
