package com.example.starter

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class TestMainVerticle {

  @BeforeEach
  fun deploy_verticle(vertx: Vertx, testContext: VertxTestContext) {
    vertx.deployVerticle(MainVerticle()).onComplete(testContext.succeeding<String> { _ -> testContext.completeNow() })
  }

  @Test
  fun `Submit a limit order and get the order book`(vertx: Vertx, testContext: VertxTestContext) {
    val client = WebClient.create(vertx)

    val limitOrderBody = JsonObject()
      .put("side", "BUY")
      .put("quantity", "1.5")
      .put("price", "50000")
      .put("pair", "BTCUSD")

    client.post(8080, "localhost", "/v1/orders/limit")
      .sendJsonObject(limitOrderBody) { ar ->
        if (ar.succeeded()) {
          client.get(8080, "localhost", "/BTCUSD/orderbook")
            .send { ar2 ->
              if (ar2.succeeded()) {
                val response = ar2.result()
                assertEquals(200, response.statusCode())

                val body = response.bodyAsJsonObject()
                assertTrue(body.containsKey("Bids"))
                assertTrue(body.containsKey("Asks"))
                assertTrue(body.containsKey("currencyPair"))

                val bids = body.getJsonArray("Bids")
                assertEquals(1, bids.size())

                val firstBid = bids.getJsonObject(0)
                assertEquals("50000", firstBid.getString("price"))
                assertEquals("1.5", firstBid.getString("quantity"))
                assertEquals(1, firstBid.getInteger("orderCount"))

                testContext.completeNow()
              } else {
                testContext.failNow(ar2.cause())
              }
            }
        } else {
          testContext.failNow(ar.cause())
        }
      }
  }

  @Test
  fun `Submit two limit orders that will fill and provide a trade history`(vertx: Vertx, testContext: VertxTestContext) {
    val client = WebClient.create(vertx)

    val buyOrderBody = JsonObject()
      .put("side", "BUY")
      .put("quantity", "1.0")
      .put("price", "50000")
      .put("pair", "BTCUSD")

    val sellOrderBody = JsonObject()
      .put("side", "SELL")
      .put("quantity", "1.0")
      .put("price", "50000")
      .put("pair", "BTCUSD")

    client.post(8080, "localhost", "/v1/orders/limit")
      .sendJsonObject(buyOrderBody) { ar ->
        if (ar.succeeded()) {
          client.post(8080, "localhost", "/v1/orders/limit")
            .sendJsonObject(sellOrderBody) { ar2 ->
              if (ar2.succeeded()) {
                client.get(8080, "localhost", "/BTCUSD/tradehistory")
                  .send { ar3 ->
                    if (ar3.succeeded()) {
                      val response = ar3.result()
                      assertEquals(200, response.statusCode())

                      val trades = response.bodyAsJsonArray()
                      assertEquals(1, trades.size())

                      val trade = trades.getJsonObject(0)
                      assertEquals("50000", trade.getString("price"))
                      assertEquals("1.0", trade.getString("quantity"))
                      assertEquals("BTCUSD", trade.getString("currencyPair"))
                      assertNotNull(trade.getString("tradedAt"))
                      assertEquals(trade.getString("takerSide"), "SELL")
                      assertEquals(1L, trade.getLong("sequenceId"))
                      assertNotNull(trade.getString("id"))
                      assertEquals("50000.0", trade.getString("quoteVolume"))

                      testContext.completeNow()
                    } else {
                      testContext.failNow(ar3.cause())
                    }
                  }
              } else {
                testContext.failNow(ar2.cause())
              }
            }
        } else {
          testContext.failNow(ar.cause())
        }
      }
  }

  @Test
  fun `Get non-existent order book returns 404`(vertx: Vertx, testContext: VertxTestContext) {
    val client = WebClient.create(vertx)

    client.get(8080, "localhost", "/NONEXISTENT/orderbook")
      .send { ar ->
        if (ar.succeeded()) {
          val response = ar.result()
          assertEquals(404, response.statusCode())
          val body = response.bodyAsJsonObject()
          assertEquals("Order book not found", body.getString("message"))
          testContext.completeNow()
        } else {
          testContext.failNow(ar.cause())
        }
      }
  }

  @Test
  fun `Submit limit order to non-existent order book returns 400`(vertx: Vertx, testContext: VertxTestContext) {
    val client = WebClient.create(vertx)

    val limitOrderBody = JsonObject()
      .put("side", "BUY")
      .put("quantity", "1.0")
      .put("price", "50000")
      .put("pair", "NONEXISTENT")

    client.post(8080, "localhost", "/v1/orders/limit")
      .sendJsonObject(limitOrderBody) { ar ->
        if (ar.succeeded()) {
          val response = ar.result()
          assertEquals(400, response.statusCode())
          val body = response.bodyAsJsonObject()
          assertEquals("Failed to submit limit order", body.getString("message"))
          testContext.completeNow()
        } else {
          testContext.failNow(ar.cause())
        }
      }
  }

  @Test
  fun `Get trade history for non-existent currency pair returns 404`(vertx: Vertx, testContext: VertxTestContext) {
    val client = WebClient.create(vertx)

    client.get(8080, "localhost", "/NONEXISTENT/tradehistory")
      .send { ar ->
        if (ar.succeeded()) {
          val response = ar.result()
          assertEquals(404, response.statusCode())
          val body = response.bodyAsJsonObject()
          assertEquals("Trade history not found", body.getString("message"))
          testContext.completeNow()
        } else {
          testContext.failNow(ar.cause())
        }
      }
  }
}
