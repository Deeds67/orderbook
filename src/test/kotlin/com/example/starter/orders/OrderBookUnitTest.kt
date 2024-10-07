package com.example.starter.orders

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OrderBookUnitTest {
  private lateinit var orderBook: OrderBookImpl

  @BeforeEach
  fun setUp() {
    orderBook = OrderBookImpl("BTCUSD")
  }

  @Test
  fun `Submitting a valid limit order to an empty order book`() {
    // Given
    val limitOrder = LimitOrder(OrderSide.BUY, BigDecimal("10"), BigDecimal("100"), "BTCUSD")

    // When
    val result = orderBook.submitLimitOrder(limitOrder)

    // Then
    assertTrue(result)
  }
}
