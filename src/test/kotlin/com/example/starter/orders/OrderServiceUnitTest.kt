package com.example.starter.orders

import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OrderProcessorUnitTest {
  private lateinit var orderBook: OrderBook
  private lateinit var orderService: OrderService

  @BeforeEach
  fun setUp() {
    orderBook = OrderBookImpl("BTCUSD")

    orderService = OrderServiceImpl(mapOf("BTCUSD" to orderBook))
  }

  @Test
  fun `submitting a valid limit order returns the order id`() {
    // Given
    val limitOrder = LimitOrder(OrderSide.BUY, BigDecimal("10"), BigDecimal("100"), "BTCUSD")

    // When
    val limitOrderId = orderService.submitLimitOrder(limitOrder)

    // Then
    assertEquals(limitOrder.orderId, limitOrderId)
  }

  @Test
  fun `submitting an invalid limit order returns null`() {
    // Given
    val invalidPricePair = "INVALID"
    val limitOrder = LimitOrder(OrderSide.BUY, BigDecimal("10"), BigDecimal("100"), invalidPricePair)

    // When
    val res = orderService.submitLimitOrder(limitOrder)

    // Then
    assertNull(res)
  }

  @Test
  fun `Submitting a limit order that failed in the order book should return null`() {
    // Given
    orderBook = mockk<OrderBook>()
    orderService = OrderServiceImpl(mapOf("USDBTC" to orderBook))
    val limitOrder = LimitOrder(OrderSide.BUY, BigDecimal("10"), BigDecimal("100"), "BTCUSD")
    coEvery { orderBook.submitLimitOrder(any()) } returns false

    // When
    val res = orderService.submitLimitOrder(limitOrder)

    // Then
    assertNull(res)
  }

  @Test
  fun `Getting an orderbook summary for an order book with orders should return the summary`() {
    // Given
    orderBook = mockk<OrderBook>()
    coEvery { orderBook.getOrderBookSummary() } returns OrderBookSummary("BTCUSD",
      listOf(PriceSummary(BigDecimal("100"), BigDecimal("1"), 1)),
      listOf(PriceSummary(BigDecimal("105"), BigDecimal("1"), 1))
    )
    orderService = OrderServiceImpl(mapOf("BTCUSD" to orderBook))

    // When
    val res = orderService.getOrderBookSummary("BTCUSD")

    // Then
    assertNotNull(res)
    assertTrue(res!!.asks.isNotEmpty())
    assertTrue(res.bids.isNotEmpty())
  }

  @Test
  fun `Getting an orderbook summary for an a non existing order book returns null`() {
    // When
    val res = orderService.getOrderBookSummary("INVALID")

    // Then
    assertNull(res)
  }


}
