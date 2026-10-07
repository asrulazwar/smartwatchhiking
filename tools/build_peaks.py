#!/usr/bin/env python3
"""
Mengambil daftar puncak (natural=peak) di Indonesia dari Overpass API dan menuliskannya
dalam format gunung.json yang dipakai aplikasi. TIDAK dijalankan di CI; jalankan manual
lalu periksa hasilnya sebelum menggabungkan ke wear/src/main/assets/gunung.json.

Contoh:
  python3 tools/build_peaks.py --min-ele 1500 --out tools/out/gunung_overpass.json
  python3 tools/build_peaks.py --merge wear/src/main/assets/gunung.json --out tools/out/gunung_gabungan.json

Catatan: data OSM tidak selalu punya tag `ele`; puncak tanpa elevasi dilewati.
"""
import argparse
import json
import re
import sys
import time
import urllib.parse
import urllib.request

OVERPASS_URL = "https://overpass-api.de/api/interpreter"

# Bounding box kasar Indonesia (lat_min, lon_min, lat_max, lon_max)
BBOX = (-11.5, 94.5, 6.5, 141.5)

QUERY = """
[out:json][timeout:180];
node["natural"="peak"]["ele"]({lat_min},{lon_min},{lat_max},{lon_max});
out body;
"""


def slug(s: str) -> str:
    s = s.lower()
    s = re.sub(r"^(gunung|gn\.?|mount|mt\.?|bukit)\s+", "", s)
    s = re.sub(r"[^a-z0-9]+", "-", s).strip("-")
    return s or "tanpa-nama"


def nama_bersih(s: str) -> str:
    return re.sub(r"^(Gunung|Gn\.?|Mount|Mt\.?)\s+", "", s.strip())


def ambil(min_ele: int):
    q = QUERY.format(lat_min=BBOX[0], lon_min=BBOX[1], lat_max=BBOX[2], lon_max=BBOX[3])
    data = urllib.parse.urlencode({"data": q}).encode()
    req = urllib.request.Request(OVERPASS_URL, data=data, headers={"User-Agent": "pendaki-wearos/1.0"})
    for percobaan in range(3):
        try:
            with urllib.request.urlopen(req, timeout=240) as r:
                return json.load(r)["elements"]
        except Exception as e:  # noqa: BLE001
            print(f"gagal ({e}), coba lagi...", file=sys.stderr)
            time.sleep(10 * (percobaan + 1))
    raise SystemExit("Overpass tidak merespons")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--min-ele", type=int, default=1000, help="elevasi minimum (m)")
    ap.add_argument("--merge", help="gunung.json yang sudah ada; entri lama dipertahankan (prioritas)")
    ap.add_argument("--out", default="tools/out/gunung_overpass.json")
    args = ap.parse_args()

    elements = ambil(args.min_ele)
    hasil = {}
    for el in elements:
        tags = el.get("tags", {})
        nama = tags.get("name:id") or tags.get("name")
        if not nama:
            continue
        try:
            ele = int(float(str(tags["ele"]).replace(",", ".").split()[0]))
        except (ValueError, KeyError):
            continue
        if ele < args.min_ele:
            continue
        g = {
            "id": slug(nama),
            "nama": nama_bersih(nama),
            "lat": round(el["lat"], 5),
            "lon": round(el["lon"], 5),
            "elevasi": ele,
            "provinsi": tags.get("is_in:state") or tags.get("addr:province") or "",
        }
        # jika id bentrok, pertahankan yang lebih tinggi
        if g["id"] not in hasil or hasil[g["id"]]["elevasi"] < ele:
            hasil[g["id"]] = g

    if args.merge:
        with open(args.merge, encoding="utf-8") as f:
            for g in json.load(f):
                hasil[g["id"]] = g  # data kurasi menang

    daftar = sorted(hasil.values(), key=lambda g: (-g["elevasi"], g["nama"]))
    import os
    os.makedirs(os.path.dirname(args.out) or ".", exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(daftar, f, ensure_ascii=False, indent=1)
    print(f"{len(daftar)} puncak ditulis ke {args.out}")


if __name__ == "__main__":
    main()
