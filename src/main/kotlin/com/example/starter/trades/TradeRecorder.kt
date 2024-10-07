package com.example.starter.trades

import com.example.starter.orders.OrderSide
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicLong

data class Trade(
  val price: BigDecimal,
  val quantity: BigDecimal,
  val currencyPair: String,
  val tradedAt: Instant,
  val takerSide: OrderSide,
  val sequenceId: Long = 0,
  val id: String = UUID.randomUUID().toString(),
  val quoteVolume: BigDecimal
)
interface TradeRecorder {
  fun recordTrade(price: BigDecimal,
                  quantity: BigDecimal,
                  pair: String,
                  takerSide: OrderSide,
                  quoteVolume: BigDecimal): Long?

  fun getRecentTrades(limit: Int): List<Trade>
}

class TradeRecorderImpl(
  private val currencyPair: String,
  firstSequenceNumber: Long = 0,
  private val maxRecentTrades: Int = 100
) : TradeRecorder {
  private val recentTrades = ArrayDeque<Trade>(maxRecentTrades)
  private val sequenceGenerator = AtomicLong(firstSequenceNumber)

  override fun recordTrade(price: BigDecimal,
                           quantity: BigDecimal,
                           pair: String,
                           takerSide: OrderSide,
                           quoteVolume: BigDecimal): Long? {

    if (currencyPair != pair)
      return null

    val trade = Trade(
      price = price,
      quantity = quantity,
      currencyPair = pair,
      tradedAt = Instant.now(),
      takerSide = takerSide,
      quoteVolume = price.multiply(quantity),
      sequenceId = sequenceGenerator.incrementAndGet()
    )
    recentTrades.addFirst(trade)
    if (recentTrades.size > maxRecentTrades) {
      recentTrades.removeLast()
    }

    return trade.sequenceId
  }

  override fun getRecentTrades(limit: Int): List<Trade> {
    return recentTrades.take(limit)
  }
}
