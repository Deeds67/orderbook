package com.example.starter.orders

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface OrderService {
  fun submitLimitOrder(limitOrder: LimitOrder): String?
  fun getOrderBookSummary(pricePair: String): OrderBookSummary?
}

class OrderServiceImpl(
  private val orderBooks: Map<String, OrderBook>,
  private val coroutineScope: CoroutineScope
): OrderService {
  private val channels = orderBooks.keys.associateWith {
    Channel<LimitOrder>(Channel.UNLIMITED)
  }

  init {
    // Launch a coroutine for each order book.
    // This ensures that orders from different order books can be processed in Parallel
    // while ensuring that each order book limit order is processed sequentially.
    orderBooks.forEach { (currencyPair, orderBook) ->
      val channel = channels[currencyPair] ?: throw Exception("Failed to get channel for $currencyPair")
      coroutineScope.launch {
        for (limitOrder in channel) {
          orderBook.submitLimitOrder(limitOrder)
        }
      }
    }
  }

  override fun submitLimitOrder(limitOrder: LimitOrder): String? {
    val channel = channels[limitOrder.pair] ?: return null
    val submitted = channel.trySend(limitOrder).isSuccess
    if (!submitted) {
      return null
    }
    return limitOrder.orderId
  }

  override fun getOrderBookSummary(pricePair: String): OrderBookSummary? {
    val orderBook = orderBooks[pricePair] ?: return null
    return orderBook.getOrderBookSummary()
  }

  fun shutdown() {
    channels.values.forEach { it.close() }
  }
}
