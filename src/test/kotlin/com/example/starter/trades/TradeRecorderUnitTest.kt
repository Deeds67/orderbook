package com.example.starter.trades

import com.example.starter.orders.OrderSide
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TradeRecorderUnitTest {

    @Test
    fun `recordTrade should record a trade and return a sequence number`() {
        // Given
        val currencyPair = "BTCUSD"
        val tradeRecorder = TradeRecorderImpl(currencyPair)
        val price = BigDecimal("50000.00")
        val quantity = BigDecimal("1.5")
        val takerSide = OrderSide.BUY
        val quoteVolume = price.multiply(quantity)

        // When
        val sequenceNumber = tradeRecorder.recordTrade(price, quantity, currencyPair, takerSide, quoteVolume)

        // Then
        assertNotNull(sequenceNumber)
        assertEquals(1L, sequenceNumber)

        val recentTrades = tradeRecorder.getRecentTrades(1)
        assertEquals(1, recentTrades.size)

        val recordedTrade = recentTrades[0]
        assertEquals(price, recordedTrade.price)
        assertEquals(quantity, recordedTrade.quantity)
        assertEquals(currencyPair, recordedTrade.currencyPair)
        assertEquals(takerSide, recordedTrade.takerSide)
        assertEquals(quoteVolume, recordedTrade.quoteVolume)
        assertEquals(sequenceNumber, recordedTrade.sequenceId)
    }

    @Test
    fun `recordTrade should increment sequence number for multiple trades`() {
        // Given
        val currencyPair = "BTCUSD"
        val tradeRecorder = TradeRecorderImpl(currencyPair)
        val price = BigDecimal("50000.00")
        val quantity = BigDecimal("1.0")
        val takerSide = OrderSide.BUY
        val quoteVolume = price.multiply(quantity)

        // When
        val sequenceNumber1 = tradeRecorder.recordTrade(price, quantity, currencyPair, takerSide, quoteVolume)
        val sequenceNumber2 = tradeRecorder.recordTrade(price, quantity, currencyPair, takerSide, quoteVolume)
        val sequenceNumber3 = tradeRecorder.recordTrade(price, quantity, currencyPair, takerSide, quoteVolume)

        // Then
        assertNotNull(sequenceNumber1)
        assertNotNull(sequenceNumber2)
        assertNotNull(sequenceNumber3)
        assertEquals(1L, sequenceNumber1)
        assertEquals(2L, sequenceNumber2)
        assertEquals(3L, sequenceNumber3)

        val recentTrades = tradeRecorder.getRecentTrades(3)
        assertEquals(3, recentTrades.size)
        assertEquals(3L, recentTrades[0].sequenceId)
        assertEquals(2L, recentTrades[1].sequenceId)
        assertEquals(1L, recentTrades[2].sequenceId)
    }

    @Test
    fun `TradeRecorder should only keep track of the specified max trades`() {
        // Given
        val currencyPair = "BTCUSD"
        val maxRecentTrades = 5
        val tradeRecorder = TradeRecorderImpl(currencyPair, maxRecentTrades = maxRecentTrades)
        val price = BigDecimal("50000.00")
        val quantity = BigDecimal("1.0")
        val takerSide = OrderSide.BUY
        val quoteVolume = price.multiply(quantity)

        // When
        repeat(10) { i ->
            tradeRecorder.recordTrade(price, quantity, currencyPair, takerSide, quoteVolume)
        }

        // Then
        val recentTrades = tradeRecorder.getRecentTrades(10)
        assertEquals(maxRecentTrades, recentTrades.size)
        
        // Verify that only the most recent trades are kept
        for (i in 0 until maxRecentTrades) {
            assertEquals(10L - i, recentTrades[i].sequenceId)
        }
    }
}
