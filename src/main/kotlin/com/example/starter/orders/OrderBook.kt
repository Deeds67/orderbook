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
  fun getBuyOrders(): List<LimitOrder>
  fun getSellOrders(): List<LimitOrder>
}

class OrderBookImpl(
  private val currencyPair: String,
): OrderBook {

  internal val buyOrders = TreeMap<BigDecimal, ArrayList<LimitOrder>>(compareByDescending { it })
  internal val sellOrders = TreeMap<BigDecimal, ArrayList<LimitOrder>>()

  override fun submitLimitOrder(limitOrder: LimitOrder): Boolean {
    if (limitOrder.pair != currencyPair)
        return false

    if (limitOrder.side == OrderSide.BUY)
      buyOrders.merge(limitOrder.price, arrayListOf(limitOrder)) { prev, order ->
        prev.addAll(order)
        prev
      } else {
      sellOrders.merge(limitOrder.price, arrayListOf(limitOrder)) { prev, order ->
        prev.addAll(order)
        prev
      }
    }


    return true
  }

  override fun getBuyOrders(): List<LimitOrder> {
    return buyOrders.values.flatten().toList()
  }

  override fun getSellOrders(): List<LimitOrder> {
    return sellOrders.values.flatten().toList()
  }

}
