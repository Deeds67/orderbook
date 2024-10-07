package com.example.starter

import com.example.starter.orders.OrderBookImpl
import com.example.starter.orders.OrderServiceImpl
import com.example.starter.trades.TradeRecorderImpl
import io.vertx.config.ConfigRetriever
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MainVerticle : CoroutineVerticle() {

  override suspend fun start() {
    val config = ConfigRetriever.create(vertx).config.coAwait()
    val currencyPairs = config.getJsonArray("currencyPairs").map { it as String }
    val scope = CoroutineScope(vertx.dispatcher() + SupervisorJob())
    val tradeRecorders = currencyPairs.associateWith { TradeRecorderImpl(it) }
    val orderBooks = currencyPairs.associateWith { pair ->
      OrderBookImpl(pair, tradeRecorders[pair]!!)
    }
    val orderService = OrderServiceImpl(orderBooks, scope)

    val server: HttpServer = vertx.createHttpServer()

    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())

    val port = config.getInteger("http.port")
    server.requestHandler(router).listen(port)
    println("HTTP server started on port $port")
  }
}
