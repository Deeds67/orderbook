package com.example.starter.orders

import com.example.starter.trades.TradeRecorder
import com.example.starter.trades.TradeRecorderImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import java.util.UUID

class OrderProcessorUnitTest {
  private lateinit var orderBook: OrderBook
  private lateinit var tradeRecorder: TradeRecorder
  private lateinit var orderService: OrderServiceImpl // Change to OrderServiceImpl for access to shutdown
  private lateinit var coroutineScope: CoroutineScope

  @BeforeEach
  fun setUp() {
    tradeRecorder = mockk<TradeRecorder>()
    coroutineScope = CoroutineScope(Dispatchers.Default)
    orderBook = OrderBookImpl("BTCUSD", tradeRecorder)

    orderService = OrderServiceImpl(mapOf("BTCUSD" to orderBook), coroutineScope)
  }

  @AfterEach
  fun tearDown() {
    orderService.shutdown()
    runBlocking {
      coroutineScope.cancel()
      coroutineScope.coroutineContext.job.join()
    }
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
    orderService = OrderServiceImpl(mapOf("USDBTC" to orderBook), coroutineScope)
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
    orderService = OrderServiceImpl(mapOf("BTCUSD" to orderBook), coroutineScope)

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

  @Test
  fun `processOrder should not run in parallel for the same orderbook`() = runBlocking {
    // Given
    val orderCount = 10
    orderBook = spyk(OrderBookImpl("BTCUSD", tradeRecorder))
    orderService = OrderServiceImpl(mapOf("BTCUSD" to orderBook), coroutineScope)

    val processingCount = AtomicInteger(0)

    val limitOrder = LimitOrder(OrderSide.SELL, BigDecimal("10"), BigDecimal("100"), "BTCUSD")

    coEvery { orderBook.submitLimitOrder(any()) } coAnswers {
      processingCount.incrementAndGet()
      delay(100)

      // Ensures that only 1 order is processed at a time
      assertEquals(1, processingCount.get(), "Only one order should be processed at a time")
      processingCount.decrementAndGet()
      true
    }

    // When
    val jobs = (1..orderCount).toList().map {
      async { orderService.submitLimitOrder(limitOrder.copy(orderId = UUID.randomUUID().toString())) }
    }
    jobs.awaitAll()

    // Allow time for processing
    delay(2000)

    // Then
    coVerify(exactly = orderCount) { orderBook.submitLimitOrder(any()) }
    assertEquals(0, processingCount.get(), "All processing should be complete")
  }

  @Test
  fun `processOrder should run in parallel for orders from different orderbooks`() = runBlocking {
    // Given
    val orderCount = 10
    val btcOrderBook = spyk(OrderBookImpl("BTCUSD", TradeRecorderImpl("BTCUSD")))
    val ethOrderBook = spyk(OrderBookImpl("ETHUSD", TradeRecorderImpl("ETHUSD")))

    orderService = OrderServiceImpl(mapOf("BTCUSD" to btcOrderBook, "ETHUSD" to ethOrderBook), coroutineScope)

    val processingCount = AtomicInteger(0)
    var maxProcessingCount = 0

    val limitOrder = LimitOrder(OrderSide.SELL, BigDecimal("10"), BigDecimal("100"), "BTCUSD")

    coEvery { btcOrderBook.submitLimitOrder(any()) } coAnswers {
      processingCount.incrementAndGet()
      delay(100)

      // Ensures that max 2 orders are processed at a time (one per order book)
      assertTrue( processingCount.get() <= 2, "Max of 2 orders should be processed in parallel")
      maxProcessingCount = processingCount.get().coerceAtLeast(maxProcessingCount)
      processingCount.decrementAndGet()
      true
    }

    coEvery { ethOrderBook.submitLimitOrder(any()) } coAnswers {
      processingCount.incrementAndGet()
      delay(100)

      // Ensures that max 2 orders are processed at a time (one per order book)
      assertTrue( processingCount.get() <= 2, "Max of 2 orders should be processed in parallel")
      maxProcessingCount = processingCount.get().coerceAtLeast(maxProcessingCount)

      processingCount.decrementAndGet()
      true
    }

    // When
    val jobs = (1..orderCount).toList().map {
      async { orderService.submitLimitOrder(limitOrder.copy(orderId = UUID.randomUUID().toString())) }
      async { orderService.submitLimitOrder(limitOrder.copy(orderId = UUID.randomUUID().toString(), pair = "ETHUSD")) }
    }
    jobs.awaitAll()

    // Allow time for processing
    delay(2000)

    // Then
    coVerify(exactly = orderCount) { btcOrderBook.submitLimitOrder(any()) }
    coVerify(exactly = orderCount) { ethOrderBook.submitLimitOrder(any()) }
    assertEquals(0, processingCount.get(), "All processing should be complete")
    assertEquals(2, maxProcessingCount, "We expected 2 orders to be processed in parallel")
  }
}
