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

  @Test
  fun `Submitting a BUY order to an order book that contains other buy orders at different prices`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("10"), BigDecimal("100"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("99"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertEquals(2, orderBook.buyOrders.size)
    assertEquals(BigDecimal("10"), orderBook.buyOrders[BigDecimal("100")]?.first()?.quantity)
    assertEquals(BigDecimal("5"), orderBook.buyOrders[BigDecimal("99")]?.first()?.quantity)
  }

  @Test
  fun `Submitting a BUY order to an order book that contains other sell orders at different prices`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("10"), BigDecimal("101"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("99"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertEquals(1, orderBook.sellOrders.size)
    assertEquals(1, orderBook.buyOrders.size)
    assertEquals(BigDecimal("10"), orderBook.sellOrders[BigDecimal("101")]?.first()?.quantity)
    assertEquals(BigDecimal("5"), orderBook.buyOrders[BigDecimal("99")]?.first()?.quantity)
  }

  @Test
  fun `Submitting a BUY order to an order book that contains other buy orders at the same price`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("10"), BigDecimal("100"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("100"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertEquals(1, orderBook.buyOrders.size)
    assertEquals(2, orderBook.buyOrders[BigDecimal("100")]?.size)
    assertEquals(BigDecimal("10"), orderBook.buyOrders[BigDecimal("100")]?.get(0)?.quantity)
    assertEquals(BigDecimal("5"), orderBook.buyOrders[BigDecimal("100")]?.get(1)?.quantity)
    assertEquals(arrayListOf<LimitOrder>(), orderBook.sellOrders.values.flatten())
  }

  @Test
  fun `Submitting a SELL order to an order book that contains other sell orders at the same price`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("10"), BigDecimal("100"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("5"), BigDecimal("100"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertEquals(1, orderBook.sellOrders.size)
    assertEquals(2, orderBook.sellOrders[BigDecimal("100")]?.size)
    assertEquals(BigDecimal("10"), orderBook.sellOrders[BigDecimal("100")]?.get(0)?.quantity)
    assertEquals(BigDecimal("5"), orderBook.sellOrders[BigDecimal("100")]?.get(1)?.quantity)
    assertEquals(arrayListOf<LimitOrder>(), orderBook.buyOrders.values.flatten())
  }

  @Test
  fun `Submitting a BUY order that will fully match on a single SELL order`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("10"), BigDecimal("100"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("100"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertEquals(1, orderBook.sellOrders.size)
    assertEquals(BigDecimal("5"), orderBook.sellOrders[BigDecimal("100")]?.first()?.quantity)
  }

  @Test
  fun `Submitting a BUY order that will fully match on multiple SELL orders`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("3"), BigDecimal("100"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("2"), BigDecimal("100"), "BTCUSD"))
    val res3 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("100"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertTrue(res3)
    assertEquals(0, orderBook.buyOrders.size)
    assertEquals(0, orderBook.sellOrders.size)
  }

  @Test
  fun `Submitting a BUY order that will partially match a single SELL order`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("3"), BigDecimal("100"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("100"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertEquals(0, orderBook.sellOrders.size)
    assertEquals(BigDecimal("2"), orderBook.buyOrders[BigDecimal("100")]?.first()?.quantity)
  }

  @Test
  fun `Submitting a BUY order that will partially match multiple SELL orders`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("3"), BigDecimal("100"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("1"), BigDecimal("100"), "BTCUSD"))
    val res3 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("100"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertTrue(res3)
    assertEquals(0, orderBook.sellOrders.size)
    assertEquals(BigDecimal("1"), orderBook.buyOrders[BigDecimal("100")]?.first()?.quantity)
  }

  @Test
  fun `Submitting a SELL order that will fully match on a single BUY order`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("10"), BigDecimal("100"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("5"), BigDecimal("100"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertEquals(1, orderBook.buyOrders.size)
    assertEquals(BigDecimal("5"), orderBook.buyOrders[BigDecimal("100")]?.first()?.quantity)
  }

  @Test
  fun `Submitting a SELL order that will fully match on multiple BUY orders`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("3"), BigDecimal("100"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("2"), BigDecimal("100"), "BTCUSD"))
    val res3 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("5"), BigDecimal("100"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertTrue(res3)
    assertEquals(0, orderBook.buyOrders.size)
    assertEquals(0, orderBook.sellOrders.size)
  }

  @Test
  fun `Submitting a SELL order that will partially match a single BUY order`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("3"), BigDecimal("100"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("5"), BigDecimal("100"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertEquals(0, orderBook.buyOrders.size)
    assertEquals(BigDecimal("2"), orderBook.sellOrders[BigDecimal("100")]?.first()?.quantity)
  }

  @Test
  fun `Submitting a SELL order that will partially match multiple BUY orders`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("3"), BigDecimal("100"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("1"), BigDecimal("100"), "BTCUSD"))
    val res3 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("5"), BigDecimal("100"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertTrue(res3)
    assertEquals(0, orderBook.buyOrders.size)
    assertEquals(BigDecimal("1"), orderBook.sellOrders[BigDecimal("100")]?.first()?.quantity)
  }

  @Test
  fun `Ensure price-time priority`() {
    // Given
    val buy1 = LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("100"), "BTCUSD")
    val buy2 = LimitOrder(OrderSide.BUY, BigDecimal("3"), BigDecimal("100"), "BTCUSD")
    val sell1 = LimitOrder(OrderSide.SELL, BigDecimal("7"), BigDecimal("100"), "BTCUSD")

    // When
    val res1 = orderBook.submitLimitOrder(buy1)
    val res2 = orderBook.submitLimitOrder(buy2)
    val res3 = orderBook.submitLimitOrder(sell1)

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertTrue(res3)
    // Buy1 should be filled first, so we expect buy2 to still exist in the order book
    assertEquals(orderBook.buyOrders[BigDecimal("100")]?.first()?.orderId, buy2.orderId)
    assertEquals(1, orderBook.buyOrders.size)
    assertEquals(1, orderBook.buyOrders[BigDecimal("100")]?.size)
    assertEquals(BigDecimal("1"), orderBook.buyOrders[BigDecimal("100")]?.first()?.quantity)
    assertTrue(orderBook.sellOrders.isEmpty())
  }
}
