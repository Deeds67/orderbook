package com.example.starter

import com.example.starter.orders.*
import com.example.starter.trades.TradeRecorder
import com.example.starter.trades.TradeRecorderImpl
import io.vertx.config.ConfigRetriever
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.math.BigDecimal
import java.util.*

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

    router.post("/v1/orders/limit").handler { ctx -> submitLimitOrder(ctx, orderService) }
    router.get("/:currencyPair/orderbook").handler { ctx -> getOrderBookSummary(ctx, orderService) }
    router.get("/:currencyPair/tradehistory").handler { ctx -> getRecentTrades(ctx, tradeRecorders) }

    val port = config.getInteger("http.port")
    server.requestHandler(router).listen(port)
    println("HTTP server started on port $port")
  }

  private fun getOrderBookSummary(ctx: RoutingContext, orderService: OrderService) {
    val currencyPair = ctx.pathParam("currencyPair") ?: throw Exception("CurrencyPair not specified")
    val summary = orderService.getOrderBookSummary(currencyPair)

    if (summary == null) {
      ctx.response().setStatusCode(404).end(JsonObject().put("message", "Order book not found").encode())
      return
    }

    val response = JsonObject()
    response.put("currencyPair", currencyPair)
    response.put("Asks", JsonArray(summary.asks.map { entry ->
      JsonObject()
        .put("price", entry.price.toString())
        .put("quantity", entry.quantity.toString())
        .put("orderCount", entry.orderCount)
    }))
    response.put("Bids", JsonArray(summary.bids.map { entry ->
      JsonObject()
        .put("price", entry.price.toString())
        .put("quantity", entry.quantity.toString())
        .put("orderCount", entry.orderCount)
    }))

    ctx.response()
      .putHeader("content-type", "application/json")
      .end(response.encode())
  }

  private fun submitLimitOrder(ctx: RoutingContext, orderService: OrderService) {
    val body = ctx.body().asJsonObject()
    val side = OrderSide.valueOf(body.getString("side").uppercase(Locale.getDefault()))
    val quantity = BigDecimal(body.getString("quantity"))
    val price = BigDecimal(body.getString("price"))
    val pair = body.getString("pair")

    val limitOrder = LimitOrder(side, quantity, price, pair)

    val processedOrderId = orderService.submitLimitOrder(limitOrder)

    val response = JsonObject()
      .put("id", processedOrderId)

    ctx.response()
      .putHeader("content-type", "application/json")
      .end(response.encode())
  }

  private fun getRecentTrades(ctx: RoutingContext, tradeRecorders: Map<String, TradeRecorder>) {
    val currencyPair = ctx.pathParam("currencyPair")
    if (currencyPair == null) {
      ctx.response().setStatusCode(404).end(JsonObject().put("message", "CurrencyPair not found").encode())
      return
    }

    val tradeRecorder = tradeRecorders[currencyPair]
    if (tradeRecorder == null) {
      ctx.response().setStatusCode(404).end(JsonObject().put("message", "Trade history not found").encode())
      return
    }

    val limit = ctx.request().getParam("limit")?.toIntOrNull() ?: 100

    val trades = tradeRecorder.getRecentTrades(limit)

    val response = JsonArray(trades.map { trade ->
      JsonObject()
        .put("price", trade.price.toString())
        .put("quantity", trade.quantity.toString())
        .put("currencyPair", trade.currencyPair)
        .put("tradedAt", trade.tradedAt.toString())
        .put("takerSide", trade.takerSide.toString())
        .put("sequenceId", trade.sequenceId)
        .put("id", trade.id)
        .put("quoteVolume", trade.quoteVolume.toString())
    })

    ctx.response()
      .putHeader("content-type", "application/json")
      .end(response.encode())
  }
}
