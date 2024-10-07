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

data class OrderBookSummary(
  val currencyPair: String,
  val bids: List<PriceSummary>,
  val asks: List<PriceSummary>
)

data class PriceSummary(
  val price: BigDecimal,
  val quantity: BigDecimal,
  val orderCount: Int
)


interface OrderBook {
  fun submitLimitOrder(limitOrder: LimitOrder): Boolean
  fun getOrderBookSummary(): OrderBookSummary
}

class OrderBookImpl(
  val currencyPair: String,
): OrderBook {

  internal val buyOrders = TreeMap<BigDecimal, ArrayList<LimitOrder>>(compareByDescending { it })
  internal val sellOrders = TreeMap<BigDecimal, ArrayList<LimitOrder>>()

  override fun submitLimitOrder(limitOrder: LimitOrder): Boolean {
    if (limitOrder.pair != currencyPair)
        return false

    processLimitOrder(limitOrder)
    return true
  }

  override fun getOrderBookSummary(): OrderBookSummary {
    val bids = buyOrders.entries.map { (price, orders) ->
      PriceSummary(price, orders.sumOf { it.quantity }, orders.size)
    }

    val asks = sellOrders.entries.map { (price, orders) ->
      PriceSummary(price, orders.sumOf { it.quantity }, orders.size)
    }

    return OrderBookSummary(currencyPair, bids, asks)
  }

  private fun processLimitOrder(limitOrder: LimitOrder) {
    when (limitOrder.side) {
      OrderSide.BUY -> processBuyOrder(limitOrder)
      OrderSide.SELL -> processSellOrder(limitOrder)
    }
  }

  private fun processBuyOrder(limitOrder: LimitOrder) {
    var remainingQuantity = limitOrder.quantity

    while (remainingQuantity > BigDecimal.ZERO && sellOrders.isNotEmpty()) {
      val lowestSellPrice = sellOrders.firstKey()
      if (lowestSellPrice > limitOrder.price) break

      val sellOrdersAtPrice = sellOrders[lowestSellPrice] ?: continue
      remainingQuantity = matchOrders(sellOrdersAtPrice, remainingQuantity, OrderSide.BUY)
      if (sellOrdersAtPrice.isEmpty()) {
        sellOrders.remove(lowestSellPrice)
      }
    }

    if (remainingQuantity > BigDecimal.ZERO) {
      buyOrders.merge(limitOrder.price, arrayListOf(limitOrder.copy(quantity = remainingQuantity))) { prev, order ->
        prev.addAll(order)
        prev
      }
    }
  }

  private fun processSellOrder(limitOrder: LimitOrder) {
    var remainingQuantity = limitOrder.quantity

    while (remainingQuantity > BigDecimal.ZERO && buyOrders.isNotEmpty()) {
      val highestBuyPrice = buyOrders.firstKey()
      if (highestBuyPrice < limitOrder.price) break

      val buyOrdersAtPrice = buyOrders[highestBuyPrice] ?: continue
      remainingQuantity = matchOrders(buyOrdersAtPrice, remainingQuantity, OrderSide.SELL)
      if (buyOrdersAtPrice.isEmpty()) {
        buyOrders.remove(highestBuyPrice)
      }
    }

    if (remainingQuantity > BigDecimal.ZERO) {
      sellOrders.merge(limitOrder.price, arrayListOf(limitOrder.copy(quantity = remainingQuantity))) { prev, order ->
        prev.addAll(order)
        prev
      }
    }
  }

  private fun matchOrders(limitOrders: ArrayList<LimitOrder>, quantity: BigDecimal, orderSide: OrderSide): BigDecimal {
    var remainingQuantity = quantity
    val iterator = limitOrders.iterator()

    while (iterator.hasNext() && remainingQuantity > BigDecimal.ZERO) {
      val existingOrder = iterator.next()

      if (existingOrder.quantity <= remainingQuantity) {
        remainingQuantity = remainingQuantity.subtract(existingOrder.quantity)
        iterator.remove()
      } else {
        val updatedQuantity = existingOrder.quantity.subtract(remainingQuantity)
        limitOrders[limitOrders.indexOf(existingOrder)] = existingOrder.copy(quantity = updatedQuantity)
        return BigDecimal.ZERO
      }
    }

    return remainingQuantity
  }
}
