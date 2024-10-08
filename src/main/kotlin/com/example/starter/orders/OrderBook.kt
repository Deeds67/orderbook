package com.example.starter.orders

import com.example.starter.trades.TradeRecorder
import java.math.BigDecimal
import java.util.*

enum class OrderSide {
  BUY, SELL
}

data class LimitOrder(
  val side: OrderSide,
  var quantity: BigDecimal,
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
  private val tradeRecorder: TradeRecorder
): OrderBook {

  internal val buyOrders = TreeMap<BigDecimal, ArrayList<LimitOrder>>(compareByDescending { it })
  internal val sellOrders = TreeMap<BigDecimal, ArrayList<LimitOrder>>()

  override fun submitLimitOrder(limitOrder: LimitOrder): Boolean {
    if (limitOrder.pair != currencyPair)
        return false

    processLimitOrder(limitOrder)
    return true
  }
  private fun processLimitOrder(limitOrder: LimitOrder) {
    when (limitOrder.side) {
      OrderSide.BUY -> processBuyOrder(limitOrder)
      OrderSide.SELL -> processSellOrder(limitOrder)
    }
  }

  private fun processBuyOrder(limitOrder: LimitOrder) {
    while (limitOrder.quantity > BigDecimal.ZERO && sellOrders.isNotEmpty()) {
      val lowestSellPrice = sellOrders.firstKey()
      if (lowestSellPrice > limitOrder.price) break

      val sellOrdersAtPrice = sellOrders[lowestSellPrice] ?: continue
      limitOrder.quantity = matchOrders(sellOrdersAtPrice, limitOrder.quantity, OrderSide.BUY)
      if (sellOrdersAtPrice.isEmpty()) {
        sellOrders.remove(lowestSellPrice)
      }
    }

    if (limitOrder.quantity > BigDecimal.ZERO) {
      addOrderToBook(buyOrders, limitOrder)
    }
  }

  private fun processSellOrder(limitOrder: LimitOrder) {

    while (limitOrder.quantity > BigDecimal.ZERO && buyOrders.isNotEmpty()) {
      val highestBuyPrice = buyOrders.firstKey()
      if (highestBuyPrice < limitOrder.price) break

      val buyOrdersAtPrice = buyOrders[highestBuyPrice] ?: continue
      limitOrder.quantity = matchOrders(buyOrdersAtPrice, limitOrder.quantity, OrderSide.SELL)
      if (buyOrdersAtPrice.isEmpty()) {
        buyOrders.remove(highestBuyPrice)
      }
    }

    if (limitOrder.quantity > BigDecimal.ZERO) {
      addOrderToBook(sellOrders, limitOrder)
    }
  }

  private fun addOrderToBook(orders: TreeMap<BigDecimal, ArrayList<LimitOrder>>, limitOrder: LimitOrder) {
    orders.getOrPut(limitOrder.price) { ArrayList() }.add(limitOrder)
  }

  private fun matchOrders(limitOrders: ArrayList<LimitOrder>, quantity: BigDecimal, orderSide: OrderSide): BigDecimal {
    var remainingQuantity = quantity
    val iterator = limitOrders.iterator()

    while (iterator.hasNext() && remainingQuantity > BigDecimal.ZERO) {
      val existingOrder = iterator.next()
      val matchedQuantity = minOf(existingOrder.quantity, remainingQuantity)
      val matchPrice = existingOrder.price

      tradeRecorder.recordTrade(
        price = matchPrice,
        quantity = matchedQuantity,
        pair = currencyPair,
        takerSide = orderSide,
        quoteVolume = matchPrice.multiply(matchedQuantity)
      )

      if (existingOrder.quantity <= remainingQuantity) {
        remainingQuantity = remainingQuantity.subtract(existingOrder.quantity)
        iterator.remove()
      } else {
        existingOrder.quantity = existingOrder.quantity.subtract(remainingQuantity)
        return BigDecimal.ZERO
      }
    }

    return remainingQuantity
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
}
