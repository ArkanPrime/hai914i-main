#!/bin/bash

# dossiers
QUERY_DIR="testsuite/queries"
DATA_DIR="../data"

mkdir -p "$QUERY_DIR"
mkdir -p "$DATA_DIR"

# génération + déduplication
for qt in testsuite/templates/*.sparql-template; do
  qt2=${qt##*templates/}
  out="$QUERY_DIR/${qt2%.sparql-template}.queryset"

  echo "Génération : $qt2"

  bin/Release/watdiv -q model/wsdbm-data-model.txt "$qt" 1000 1 |
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
    }
  ' > "$out"
done

# concaténation sans _10000
find "$QUERY_DIR" -type f -name "*.queryset" ! -name "*_10000*" \
  -exec cat {} + > "$DATA_DIR/all_queries.queryset"

# concaténation avec _10000 uniquement
find "$QUERY_DIR" -type f -name "*.queryset" -name "*_10000*" \
  -exec cat {} + > "$DATA_DIR/all_queries_10000.queryset"

echo "Fichiers créés :"
echo " - data/all_queries.queryset"
echo " - data/all_queries_10000.queryset"
