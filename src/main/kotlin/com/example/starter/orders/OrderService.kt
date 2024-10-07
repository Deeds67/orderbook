package com.example.starter.orders

import java.math.BigDecimal


interface OrderService {
  fun submitLimitOrder(limitOrder: LimitOrder): String?
  fun getOrderBookSummary(pricePair: String): OrderBookSummary?
}

class OrderServiceImpl(
  private val orderBooks: Map<String, OrderBook>,
): OrderService {
  override fun submitLimitOrder(limitOrder: LimitOrder): String? {
    val orderBook = orderBooks[limitOrder.pair] ?: return null
    val submitted = orderBook.submitLimitOrder(limitOrder)
    if (!submitted)
      return null

    return limitOrder.orderId
  }

  override fun getOrderBookSummary(pricePair: String): OrderBookSummary? {
    val orderBook = orderBooks[pricePair] ?: return null
    return orderBook.getOrderBookSummary()
  }

}
