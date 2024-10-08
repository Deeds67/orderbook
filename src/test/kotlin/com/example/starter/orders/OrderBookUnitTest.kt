package com.example.starter.orders

import com.example.starter.trades.TradeRecorder
import com.example.starter.trades.TradeRecorderImpl
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OrderBookUnitTest {
  private lateinit var orderBook: OrderBookImpl
  private lateinit var tradeRecorder: TradeRecorder

  @BeforeEach
  fun setUp() {
    tradeRecorder = mockk<TradeRecorder>()
    coEvery { tradeRecorder.recordTrade(any(), any(), any(), any(), any()) } returns 0L
    orderBook = OrderBookImpl("BTCUSD", tradeRecorder)
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
    assertEquals(listOf(limitOrder), orderBook.buyOrders[BigDecimal("100")])
    assertEquals(0, orderBook.sellOrders.size)
  }

  @Test
  fun `Submitting a valid SELL limit order to an empty order book should add the order`() {
    // Given
    val limitOrder = LimitOrder(OrderSide.SELL, BigDecimal("10"), BigDecimal("100"), "BTCUSD")

    // When
    val result = orderBook.submitLimitOrder(limitOrder)

    // Then
    assertTrue(result)
    assertEquals(listOf(limitOrder), orderBook.sellOrders[BigDecimal("100")])
    assertEquals(0, orderBook.buyOrders.size)
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

  @Test
  fun `Ensure Price improvement when a BUY limit order is added when the SELL price is lower`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("5"), BigDecimal("98"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("5"), BigDecimal("99"), "BTCUSD"))
    val res3 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("7"), BigDecimal("100"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertTrue(res3)

    assertTrue(orderBook.sellOrders.containsKey(BigDecimal("99")))
    assertEquals(BigDecimal("3"), orderBook.sellOrders[BigDecimal("99")]?.first()?.quantity)
    assertTrue(orderBook.buyOrders.isEmpty())

    // Verify that the buy order was filled at better prices
    assertEquals(1, orderBook.sellOrders.size)
    assertEquals(1, orderBook.sellOrders[BigDecimal("99")]?.size)
  }

  @Test
  fun `Ensure Price improvement when a SELL limit order is added when the BUY price is higher`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("98"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("99"), "BTCUSD"))
    val res3 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("7"), BigDecimal("95"), "BTCUSD"))

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertTrue(res3)

    assertTrue(orderBook.buyOrders.containsKey(BigDecimal("98")))
    assertEquals(BigDecimal("3"), orderBook.buyOrders[BigDecimal("98")]?.first()?.quantity)
    assertTrue(orderBook.sellOrders.isEmpty())

    // Verify that the sell order was filled at better prices
    assertEquals(1, orderBook.buyOrders.size)
    assertEquals(1, orderBook.buyOrders[BigDecimal("98")]?.size)
  }

  @Test
  fun `Getting an order book summary for an empty order book returns an empty summary`() {
    // When
    val emptySummary = orderBook.getOrderBookSummary()

    // Then
    assertEquals(OrderBookSummary(orderBook.currencyPair, listOf(), listOf()), emptySummary)
  }

  @Test
  fun `Getting an order book summary for an order book with only one ask and sell`() {
    // When
    val res1 = orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("99"), "BTCUSD"))
    val res2 = orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("7"), BigDecimal("100"), "BTCUSD"))
    val summary = orderBook.getOrderBookSummary()

    // Then
    assertTrue(res1)
    assertTrue(res2)
    assertEquals(OrderBookSummary(orderBook.currencyPair,
      listOf(PriceSummary(BigDecimal("99"), BigDecimal("5"), 1)),
      listOf(PriceSummary(BigDecimal("100"), BigDecimal("7"), 1))), summary)
  }

  @Test
  fun `Getting an order book summary for an order book with multiple one asks and sells at the same price`() {
    // When
    orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("95"), "BTCUSD"))
    orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("99"), "BTCUSD"))
    orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("5"), BigDecimal("99"), "BTCUSD"))

    orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("7"), BigDecimal("100"), "BTCUSD"))
    orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("7"), BigDecimal("100"), "BTCUSD"))
    orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("7"), BigDecimal("105"), "BTCUSD"))

    val summary = orderBook.getOrderBookSummary()

    // Then
    assertEquals(OrderBookSummary(orderBook.currencyPair,
      listOf(PriceSummary(BigDecimal("99"), BigDecimal("10"), 2),
        PriceSummary(BigDecimal("95"), BigDecimal("5"), 1)),
      listOf(PriceSummary(BigDecimal("100"), BigDecimal("14"), 2),
        PriceSummary(BigDecimal("105"), BigDecimal("7"), 1)),
      ), summary)
  }

  @Test
  fun `Submitting a limit order that doesn't get filled does not record a trade`() {
    // Given
    val limitOrder = LimitOrder(OrderSide.BUY, BigDecimal("10"), BigDecimal("100"), "BTCUSD")

    // When
    val result = orderBook.submitLimitOrder(limitOrder)

    // Then
    coVerify(exactly = 0) { tradeRecorder.recordTrade(any(), any(), any(), any(), any()) }
    assertTrue(result)
  }

  @Test
  fun `Submitting an order that trigger multiple fills records one trade per fill`() {
    // Given
    tradeRecorder = TradeRecorderImpl("BTCUSD")
    orderBook = OrderBookImpl("BTCUSD", tradeRecorder)

    // When
    orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("3"), BigDecimal("100"), "BTCUSD"))
    orderBook.submitLimitOrder(LimitOrder(OrderSide.BUY, BigDecimal("2"), BigDecimal("100"), "BTCUSD"))
    orderBook.submitLimitOrder(LimitOrder(OrderSide.SELL, BigDecimal("5"), BigDecimal("100"), "BTCUSD"))

    val trades = tradeRecorder.getRecentTrades(10)

    // Then
    assertEquals(2, trades.size)

    val trade1 = trades[1]
    val trade2 = trades[0]

    assertEquals(1L, trade1.sequenceId)
    assertEquals("BTCUSD", trade1.currencyPair)
    assertEquals(BigDecimal("3"), trade1.quantity)
    assertEquals(BigDecimal("100"), trade1.price)
    assertEquals(BigDecimal("300"), trade1.quoteVolume)
    assertEquals(OrderSide.SELL, trade1.takerSide)

    assertEquals(2L, trade2.sequenceId)
    assertEquals("BTCUSD", trade2.currencyPair)
    assertEquals(BigDecimal("2"), trade2.quantity)
    assertEquals(BigDecimal("100"), trade2.price)
    assertEquals(BigDecimal("200"), trade2.quoteVolume)
    assertEquals(OrderSide.SELL, trade2.takerSide)
  }
}
