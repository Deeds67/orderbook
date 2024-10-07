## OrderBook

This service is an example order book.

It is an http service and exposes the following endpoints:

1. Get order book
2. Submit limit order
3. Recent Trades

## Design decisions

* An Order book keeps track of orders using two `TreeMaps` (one for bids, one for asks). `TreeMap` uses a Red-Black tree internally, which maintains sorting on the key.
We're using the `price` as key, with an `ArrayList<LimitOrder>` as value, which maintains the order in which orders arrive.
* The use of `ArrayList` was chosen, as it seems to outperform other data structures in simulated performance tests.
This is most likely due to the fact that arrays allocate a contiguous block of memory, and have better cache locality.
From research, it looks like `adding` and `cancelling` orders happen much more regularly than orders being `filled`, so it is advised
to do thorough performance testing that will mimic real-world conditions instead of relying solely on Big O time complexities.
* Coroutine channels are used to ensure that order books process their orders sequentially, while allowing other order books to process orders in parallel.


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
