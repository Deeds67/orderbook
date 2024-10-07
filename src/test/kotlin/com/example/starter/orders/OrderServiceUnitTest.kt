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


}
