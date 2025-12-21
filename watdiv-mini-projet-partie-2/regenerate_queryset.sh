#!/bin/bash
set -euo pipefail

# dossiers
QUERY_DIR="testsuite/queries"
DATA_DIR="../data"

mkdir -p "$QUERY_DIR" "$DATA_DIR"

for qt in testsuite/templates/*.sparql-template; do
  qt2=${qt##*templates/}

  [[ "$qt2" == *"_10000"* ]] && continue

  out="$QUERY_DIR/${qt2%.sparql-template}.queryset"

  echo "Génération (normal) : $qt2"

  bin/Release/watdiv -q model/wsdbm-data-model.txt "$qt" 1500 1 |
  awk '
    BEGIN { RS=""; ORS="\n\n" }
    {
      gsub(/[ \t\n]+/, " ", $0)
      if (!($0 in seen)) {
        seen[$0]=1
        print $0
      } else {
        dup++
      }
    }
    END {
      print "Doublons supprimés :", dup > "/dev/stderr"
      print "Queries uniques :", length(seen) > "/dev/stderr"

    }
  ' > "$out"
done

find "$QUERY_DIR" -type f -name "*.queryset" ! -name "*_10000*" \
  -exec cat {} + > "$DATA_DIR/all_queries.queryset"

echo "Fichier créé : $DATA_DIR/all_queries.queryset"
awk 'BEGIN{RS=""; c=0} {c++} END{print "Total queries (global) :", c}' \
  "$DATA_DIR/all_queries.queryset"