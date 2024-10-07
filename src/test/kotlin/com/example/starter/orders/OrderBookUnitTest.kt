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
  fun `Submitting a valid limit order to the correct order book`() {
    // Given
    val limitOrder = LimitOrder(OrderSide.BUY, BigDecimal("10"), BigDecimal("100"), "BTCUSD")

    // When
    val result = orderBook.submitLimitOrder(limitOrder)

    // Then
    assertTrue(result)
  }

  @Test
  fun `Submitting a limit order to wrong order book should reject it`() {
    // Given
    val invalidCurrencyPair = "ETHUSD"
    val limitOrder = LimitOrder(OrderSide.BUY, BigDecimal("10"), BigDecimal("100"), invalidCurrencyPair)

    // When
    val result = orderBook.submitLimitOrder(limitOrder)

    // Then
    assertFalse(result)
  }

  @Test
  fun `Submitting a valid BUY limit order to an empty order book should add the order`() {
    // Given
    val limitOrder = LimitOrder(OrderSide.BUY, BigDecimal("10"), BigDecimal("100"), "BTCUSD")

    // When
    val result = orderBook.submitLimitOrder(limitOrder)

    // Then
    assertTrue(result)
    assertEquals(listOf(limitOrder), orderBook.getBuyOrders())
    assertEquals(arrayListOf<LimitOrder>(), orderBook.getSellOrders())
  }

  @Test
  fun `Submitting a valid SELL limit order to an empty order book should add the order`() {
    // Given
    val limitOrder = LimitOrder(OrderSide.SELL, BigDecimal("10"), BigDecimal("100"), "BTCUSD")

    // When
    val result = orderBook.submitLimitOrder(limitOrder)

    // Then
    assertTrue(result)
    assertEquals(arrayListOf<LimitOrder>(), orderBook.getBuyOrders())
    assertEquals(listOf(limitOrder), orderBook.getSellOrders())
  }
}
