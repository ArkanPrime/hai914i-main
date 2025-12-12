for qt in testsuite/templates/*.sparql-template;
do
  qt2=${qt##*templates/}
  out="testsuite/queries/${qt2%.sparql-template}.queryset"

  bin/Release/watdiv -q model/wsdbm-data-model.txt "$qt" 100 1 |
  awk '
    BEGIN { RS=""; ORS="\n\n" }
    {
      gsub(/[ \t\n]+/, " ", $0)   # normalisation
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
