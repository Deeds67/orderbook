package com.example.starter

import com.example.starter.orders.OrderBookImpl
import com.example.starter.orders.OrderServiceImpl
import com.example.starter.trades.TradeRecorderImpl
import io.vertx.config.ConfigRetriever
import io.vertx.core.http.HttpServer
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
  }
}
