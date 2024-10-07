package com.example.starter.orders

import java.math.BigDecimal
import java.util.*

enum class OrderSide {
  BUY, SELL
}

data class LimitOrder(
  val side: OrderSide,
  val quantity: BigDecimal,
  val price: BigDecimal,
  val pair: String,
  val orderId: String = UUID.randomUUID().toString(),
)

interface OrderBook {
  fun submitLimitOrder(limitOrder: LimitOrder): Boolean
}

class OrderBookImpl(
  private val currencyPair: String,
): OrderBook {
  override fun submitLimitOrder(limitOrder: LimitOrder): Boolean {
    return true
  }

}
