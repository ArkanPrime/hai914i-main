#!/bin/bash
set -euo pipefail

QUERY_DIR="testsuite/queries"
DATA_DIR="../data"

mkdir -p "$QUERY_DIR" "$DATA_DIR"

for qt in testsuite/templates/*.sparql-template; do
  qt2=${qt##*templates/}

  echo "Génération (_10000) : $qt2"

  bin/Release/watdiv -q model/wsdbm-data-model.txt "$qt" 1000 1 |
  awk '
    BEGIN { RS=""; ORS="\n\n" }
    {
      gsub(/[ \t\n]+/, " ", $0)
      if (!($0 in seen)) {
        seen[$0]=1
        print $0
      }
    }
  ' > "$QUERY_DIR/${qt2%.sparql-template}_10000.queryset"
done

# concaténation _10000 uniquement
find "$QUERY_DIR" -type f -name "*_10000.queryset" \
  -exec cat {} + > "$DATA_DIR/all_queries_10000.queryset"

echo "Fichier créé : $DATA_DIR/all_queries_10000.queryset"
