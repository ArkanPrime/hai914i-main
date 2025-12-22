import sys
import csv
import numpy as np

import matplotlib
matplotlib.use("Agg")   # pas de GUI => pas de GTK => pas de segfault
import matplotlib.pyplot as plt

def load_csv(path: str):
    qid = []
    star = []
    ans = []
    match_ms = []
    drain_ms = []
    total_ms = []

    with open(path, newline="", encoding="utf-8") as f:
        r = csv.DictReader(f)
        for row in r:
            qid.append(int(row["query_id"]))
            star.append(int(row["star_size"]))
            ans.append(int(row["answers"]))
            match_ms.append(float(row["match_ms"]))
            drain_ms.append(float(row["drain_ms"]))
            total_ms.append(float(row["total_ms"]))

    return (np.array(qid), np.array(star), np.array(ans),
            np.array(match_ms), np.array(drain_ms), np.array(total_ms))

def summary(ans, total_ms, match_ms, drain_ms):
    zero = int((ans == 0).sum())
    print(f"queries: {len(ans)}")
    print(f"answers: total={ans.sum()} zero={zero} ({100*zero/len(ans):.2f}%)")
    print(f"total_ms: sum={total_ms.sum():.3f} mean={total_ms.mean():.3f} p50={np.percentile(total_ms,50):.3f} p95={np.percentile(total_ms,95):.3f} max={total_ms.max():.3f}")
    print(f"match_ms: mean={match_ms.mean():.3f} p95={np.percentile(match_ms,95):.3f} max={match_ms.max():.3f}")
    print(f"drain_ms: mean={drain_ms.mean():.3f} p95={np.percentile(drain_ms,95):.3f} max={drain_ms.max():.3f}")

def top_slowest(qid, ans, total_ms, k=15):
    idx = np.argsort(total_ms)[-k:][::-1]
    print(f"\nTop {k} slowest queries (by total_ms):")
    for i in idx:
        print(f"  query_id={qid[i]} answers={ans[i]} total_ms={total_ms[i]:.3f}")

def save_hist(data, title, xlabel, filename, bins=60):
    plt.figure(figsize=(9, 5))
    plt.hist(data, bins=bins)
    plt.title(title)
    plt.xlabel(xlabel)
    plt.ylabel("Number of queries")
    plt.tight_layout()
    plt.savefig(filename, dpi=200)
    plt.close()

def save_scatter(x, y, title, xlabel, ylabel, filename):
    plt.figure(figsize=(9, 5))
    plt.scatter(x, y, s=8)
    plt.title(title)
    plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    plt.tight_layout()
    plt.savefig(filename, dpi=200)
    plt.close()

def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "benchmark_per_query_ALL_QUERIES.csv"
    qid, star, ans, match_ms, drain_ms, total_ms = load_csv(path)

    print(f"file: {path}")
    summary(ans, total_ms, match_ms, drain_ms)
    top_slowest(qid, ans, total_ms, k=15)

    # Graphes (PNG)
    save_hist(total_ms, "Distribution of TOTAL query time", "total_ms", "hist_total_ms.png")
    save_hist(match_ms, "Distribution of MATCH time", "match_ms", "hist_match_ms.png")
    save_hist(drain_ms, "Distribution of DRAIN time", "drain_ms", "hist_drain_ms.png")

    save_scatter(ans, total_ms, "Answers vs TOTAL time", "answers", "total_ms", "scatter_answers_total_ms.png")

    # Avec/sans réponses
    with_ans = int((ans > 0).sum())
    without_ans = int((ans == 0).sum())
    plt.figure(figsize=(6, 4))
    plt.bar(["With answers", "Without answers"], [with_ans, without_ans])
    plt.ylabel("Number of queries")
    plt.title("Queries with/without answers")
    plt.tight_layout()
    plt.savefig("bar_with_without_answers.png", dpi=200)
    plt.close()

    # Optionnel : temps moyen par star_size (si star_size dispo, sinon -1)
    valid = star >= 0
    if valid.any():
        uniq = np.unique(star[valid])
        means = [total_ms[(star == s) & valid].mean() for s in uniq]
        plt.figure(figsize=(9, 5))
        plt.bar([str(s) for s in uniq], means)
        plt.xlabel("star_size")
        plt.ylabel("mean total_ms")
        plt.title("Mean query time by star_size")
        plt.tight_layout()
        plt.savefig("bar_mean_time_by_star_size.png", dpi=200)
        plt.close()

    print("\nSaved plots:")
    print(" - hist_total_ms.png")
    print(" - hist_match_ms.png")
    print(" - hist_drain_ms.png")
    print(" - scatter_answers_total_ms.png")
    print(" - bar_with_without_answers.png")
    if valid.any():
        print(" - bar_mean_time_by_star_size.png")

if __name__ == "__main__":
    main()
