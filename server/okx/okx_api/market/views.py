from django.shortcuts import render
import json
import time

from django.http import JsonResponse
from .redis_client import r
import hashlib
import time
from django.conf import settings
from django.http import JsonResponse



STALE_MS = 10_000  # 10秒


API_KEY = "your_key"
API_SECRET = "your_secret"

def auth(request):
    # return True
    key = request.GET.get("key")
    ts = request.GET.get("ts")
    sign = request.GET.get("sign")

    if not key or not ts or not sign:
        return JsonResponse({"error": "missing params"}, status=403)

    if key != API_KEY:
        return JsonResponse({"error": "invalid key"}, status=403)

    # ⏱ 防重放（允许 ±60 秒）
    now = int(time.time())
    if abs(now - int(ts)) > 60:
        return JsonResponse({"error": "timestamp expired"}, status=403)

    # 🔐 校验签名
    raw = f"{key}{ts}{API_SECRET}"
    server_sign = hashlib.md5(raw.encode()).hexdigest()

    if sign != server_sign:
        return JsonResponse({"error": "invalid sign"}, status=403)

    return True



def get_prices(request):
    authRes = auth(request)
    if isinstance(authRes,JsonResponse):
        return authRes

    inst_ids = request.GET.get("instIds")

    if not inst_ids:
        return JsonResponse({"error": "instIds required"}, status=400)

    inst_list = inst_ids.split(",")

    result = []
    now = int(time.time() * 1000)

    for inst_id in inst_list:
        raw = r.get(f"price:{inst_id}")
        if not raw:
            continue

        item = json.loads(raw)

        result.append({
            "instId": item["instId"],
            "price": item["price"],
            "ts": item["ts"],
            "stale": (now - item["ts"]) > STALE_MS
        })

    return JsonResponse({"data": result})


def get_price(request):

    authRes = auth(request)
    if authRes is not True:
        return authRes
    inst_id = request.GET.get("instId")

    if not inst_id:
        return JsonResponse({"error": "instId is required"}, status=400)

    raw = r.get(f"price:{inst_id}")

    if not raw:
        return JsonResponse({
            "instId": inst_id,
            "price": None,
            "ts": None,
            "stale": True,
            "error": "no data"
        }, status=404)

    item = json.loads(raw)

    now = int(time.time() * 1000)

    return JsonResponse({
        "instId": item["instId"],
        "price": item["price"],
        "ts": item["ts"],
        "stale": (now - item["ts"]) > STALE_MS
    })


def get_prices2(request):
    authRes = auth(request)
    if authRes is not True:
        return authRes
    keys = r.keys("price:*")

    result = []
    now = int(time.time() * 1000)

    for key in keys:
        raw = r.get(key)
        if not raw:
            continue

        item = json.loads(raw)

        result.append({
            "instId": item["instId"],
            "price": item["price"],
            "ts": item["ts"],
            "stale": (now - item["ts"]) > STALE_MS
        })

    return JsonResponse({"data": result})


def health(request):
    last = r.get("ws:last_update")

    if not last:
        return JsonResponse({
            "ws_connected": False,
            "msg": "no recent data"
        })

    now = int(time.time() * 1000)

    return JsonResponse({
        "ws_connected": (now - int(last)) < 10000,
        "last_update": int(last)
    })
# Create your views here.
