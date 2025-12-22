#!/usr/bin/env python3
import sys
import csv
from collections import defaultdict

def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "benchmark_compare_all.csv"

    total = defaultdict(int)
    with_ans = defaultdict(int)

    with open(path, newline="", encoding="utf-8") as f:
        r = csv.DictReader(line for line in f if not line.startswith("#"))
        for row in r:
            eng = row["engine"].strip()
            ans = int(row["answers"])
            total[eng] += 1
            if ans > 0:
                with_ans[eng] += 1

    for eng in sorted(total.keys()):
        print(f"{eng}: {with_ans[eng]} / {total[eng]} queries with answers")

if __name__ == "__main__":
    main()
