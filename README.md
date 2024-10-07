## OrderBook

This service is an example order book.

It is an http service and exposes the following endpoints:

1. Get order book
2. Submit limit order
3. Recent Trades

## Building

To run the tests:
```
./gradlew clean test
```

To run the application:
```
./gradlew clean run
```

## Trying it out:

Creating a limit order:

```bash
curl -X POST http://localhost:8080/v1/orders/limit \
  -H "Content-Type: application/json" \
  -d '{"side":"SELL","quantity":"0.4","price":"29800","pair":"BTCUSD"}'
```

Getting the order book summary:

```bash
curl http://localhost:8080/BTCUSD/orderbook
```

Getting recent trades:

```bash
curl http://localhost:8080/BTCUSD/tradehistory
```

## Config

The supported currency pairs are defined in the [config.json file](conf/config.json)
