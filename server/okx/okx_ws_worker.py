# 从okx获取实时价格, 然后存入redis


import asyncio
import json
import time
import traceback

import redis.asyncio as redis
import websockets

OKX_WS_URL = "wss://ws.okx.com:8443/ws/v5/public"

# 👉 你要订阅的合约
SYMBOLS = [
    "BTC-USDT-SWAP",
    "ETH-USDT-SWAP",
]

REDIS_HOST = "127.0.0.1"
REDIS_PORT = 6379
REDIS_TTL = 30  # 秒

r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)


async def save_price(inst_id: str, price: float):
    ts = int(time.time() * 1000)

    data = {
        "instId": inst_id,
        "price": price,
        "ts": ts,
    }

    key = f"price:{inst_id}"

    await r.set(key, json.dumps(data), ex=REDIS_TTL)

    # 记录最后更新时间（用于健康检查）
    await r.set("ws:last_update", ts, ex=REDIS_TTL)

    print(f"[OK] {inst_id} = {price}")


async def subscribe(ws):
    sub = {
        "op": "subscribe",
        "args": [
            {"channel": "mark-price", "instId": inst_id}
            for inst_id in SYMBOLS
        ],
    }

    await ws.send(json.dumps(sub))
    print("[WS] subscribed")


async def handle_message(msg: str):
    try:
        data = json.loads(msg)

        if "data" not in data:
            return

        for item in data["data"]:
            inst_id = item.get("instId")
            mark_px = item.get("markPx")

            if not inst_id or not mark_px:
                continue

            await save_price(inst_id, float(mark_px))

    except Exception:
        print("[ERROR] parse error")
        traceback.print_exc()


async def run():
    reconnect_delay = 1

    while True:
        try:
            print("[WS] connecting...")

            async with websockets.connect(
                OKX_WS_URL,
                ping_interval=15,
                ping_timeout=10,
                close_timeout=5,
                max_size=2**20,
            ) as ws:

                print("[WS] connected")

                await subscribe(ws)

                reconnect_delay = 1

                while True:
                    msg = await ws.recv()
                    await handle_message(msg)

        except Exception as e:
            print(f"[WS] error: {e}")
            print(f"[WS] reconnect in {reconnect_delay}s")

            await asyncio.sleep(reconnect_delay)

            reconnect_delay = min(reconnect_delay * 2, 40)


if __name__ == "__main__":
    asyncio.run(run())
