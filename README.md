# OrderBook

This project implements a simple order book service. It provides an HTTP API for managing limit orders, retrieving order book summaries, and accessing recent trade history.

## API Endpoints

The service exposes the following HTTP endpoints:

1. `POST /v1/orders/limit` - Submit a limit order
2. `GET /{currencyPair}/orderbook` - Get the order book summary for a specific currency pair
3. `GET /{currencyPair}/tradehistory` - Retrieve recent trades for a specific currency pair

## Design Decisions

- **Order Book Structure**: Each order book uses two `TreeMap`s (one for bids, one for asks) to maintain sorted price levels. The `TreeMap` uses a Red-Black tree internally, ensuring efficient sorting on the price key.

- **Order Storage**: At each price level, orders are stored in an `ArrayList<LimitOrder>`, preserving the order of arrival. `ArrayList` was chosen for its performance benefits due to better cache locality.

- **Concurrency**: Coroutine channels are used to ensure that each order book processes orders sequentially while allowing different order books to operate in parallel.

- **Trade Recording**: Recent trades are stored in a fixed-size `ArrayDeque` for efficient retrieval of the latest trades.

## Performance Considerations

- The choice of `ArrayList` for storing orders at each price level was based on simulated performance tests. Real-world conditions may be different, so performance testing is recommended.
- According to Jane Street, order addition and cancellation occur much more frequently than order fills, so optimization would likely be done on these use-cases.

## Building and Running

To run the tests:
```
./gradlew clean test
```

To run the application:
```
./gradlew clean run
```

## Usage Examples

### Submit a Limit Order

```bash
curl -X POST http://localhost:8080/v1/orders/limit \
  -H "Content-Type: application/json" \
  -d '{"side":"SELL","quantity":"0.4","price":"29800","pair":"BTCUSD"}'
```

### Get Order Book Summary

```bash
curl http://localhost:8080/BTCUSD/orderbook
```

### Get Recent Trades

```bash
curl http://localhost:8080/BTCUSD/tradehistory
```

## Configuration

Supported currency pairs are defined in the `conf/config.json` file. Modify this file to add or remove supported trading pairs.
