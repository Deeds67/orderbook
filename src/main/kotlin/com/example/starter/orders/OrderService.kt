package com.example.starter.orders

interface OrderService {
  fun submitLimitOrder(limitOrder: LimitOrder): String?
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

}
