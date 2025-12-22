package qengine.program;

import fr.boreal.model.logicalElements.api.Substitution;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;
import qengine.parser.RDFTriplesParser;
import qengine.parser.StarQuerySparQLParser;
import qengine.storage.RDFHexaStore;
import qengine.storage.RDFStorage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class Benchmark {

    // ===== DATA =====
    private static final String DATA_FILE = "data/data.nt";

    // ===== QUERY FILES =====
    private static final String[] QUERY_FILES = {
            "data/all_queries.queryset",
            "data/all_queries_10000.queryset"
    };

    // ===== OUTPUT NAMES =====
    private static final String CSV_PREFIX = "benchmark_compare_";
    private static final String ZERO_HEXA_PREFIX = "zero_queries_HEXA_";
    private static final String ZERO_INTEGRAAL_PREFIX = "zero_queries_INTEGRAAL_";

    private enum Engine { HEXA, INTEGRAAL }

    private static final class Row {
        final int queryId;
        final Engine engine;
        final int starSize;
        final long answers;
        final long execNs;

        Row(int q, Engine e, int s, long a, long t) {
            queryId = q;
            engine = e;
            starSize = s;
            answers = a;
            execNs = t;
        }
    }

    // ===== MAIN =====
    public static void main(String[] args) throws Exception {

        // ---- Load DATA ONCE ----
        long t0 = System.nanoTime();
        List<RDFTriple> triples = loadTriples(DATA_FILE);
        long parseDataNs = System.nanoTime() - t0;

        System.out.printf(
                "Triples: %,d loaded in %.3f s%n",
                triples.size(), parseDataNs / 1e9
        );

        // ---- Build HEXA ----
        RDFStorage hexa = new RDFHexaStore();
        long tHexa0 = System.nanoTime();
        for (RDFTriple t : triples) hexa.add(t);
        long buildHexaNs = System.nanoTime() - tHexa0;

        System.out.printf("HEXA built in %.3f s%n", buildHexaNs / 1e9);

        // ---- Build INTEGRAAL ----
        IntegraalRuntime ig = IntegraalRuntime.tryInit();
        long buildIgNs = -1;

        if (ig.available) {
            long tIg0 = System.nanoTime();
            for (RDFTriple t : triples) ig.addFact(t);
            buildIgNs = System.nanoTime() - tIg0;
            System.out.printf("INTEGRAAL built in %.3f s%n", buildIgNs / 1e9);
        } else {
            System.out.println("INTEGRAAL unavailable: " + ig.whyUnavailable);
        }

        // ---- RUN BENCHMARK FOR EACH QUERY FILE ----
        for (String queryFile : QUERY_FILES) {
            runBenchmarkForQueryFile(
                    queryFile,
                    hexa,
                    ig,
                    triples.size(),
                    parseDataNs,
                    buildHexaNs,
                    buildIgNs
            );
        }

        System.out.println("All benchmarks finished.");
    }

    // ===== BENCHMARK PER QUERY FILE =====
    private static void runBenchmarkForQueryFile(
            String queryFile,
            RDFStorage hexa,
            IntegraalRuntime ig,
            int nTriples,
            long parseDataNs,
            long buildHexaNs,
            long buildIgNs
    ) throws Exception {

        String tag = queryFile.contains("10000") ? "10000" : "all";

        System.out.println("\n=== Benchmark: " + queryFile + " ===");

        // ---- Load QUERIES ----
        long tq0 = System.nanoTime();
        List<StarQuery> queries = loadQueries(queryFile);
        long parseQueriesNs = System.nanoTime() - tq0;

        System.out.printf(
                "Queries: %,d loaded in %.3f s%n",
                queries.size(), parseQueriesNs / 1e9
        );

        List<Row> rows = new ArrayList<>();
        List<Integer> zeroHexa = new ArrayList<>();
        List<Integer> zeroIg = new ArrayList<>();

        for (int i = 0; i < queries.size(); i++) {
            StarQuery q = queries.get(i);
            int starSize = q.getRdfAtoms().size();

            // ---- HEXA ----
            long h0 = System.nanoTime();
            Iterator<Substitution> hit = hexa.match(q);
            long hAns;
            if (!hit.hasNext()) {
                hAns = 0;
            } else {
                hit.next();
                hAns = 1 + drain(hit);
            }
            long hNs = System.nanoTime() - h0;

            rows.add(new Row(i, Engine.HEXA, starSize, hAns, hNs));
            if (hAns == 0) zeroHexa.add(i);

            // ---- INTEGRAAL ----
            if (ig.available) {
                Object fo = q.asFOQuery();
                long ig0 = System.nanoTime();
                long igAns = ig.countAnswersFastZero(fo);
                long igNs = System.nanoTime() - ig0;

                rows.add(new Row(i, Engine.INTEGRAAL, starSize, igAns, igNs));
                if (igAns == 0) zeroIg.add(i);
            }

            if (i % 500 == 0 && i > 0) {
                System.out.printf("Progress (%s): %d / %d%n",
                        tag, i, queries.size());
            }
        }

        // ---- WRITE OUTPUTS ----
        writeCsv(
                CSV_PREFIX + tag + ".csv",
                rows,
                nTriples,
                queries.size(),
                parseDataNs,
                parseQueriesNs,
                buildHexaNs,
                buildIgNs,
                ig.available
        );

        writeList(ZERO_HEXA_PREFIX + tag + ".txt", zeroHexa);
        writeList(ZERO_INTEGRAAL_PREFIX + tag + ".txt", zeroIg);

        System.out.println("Done: " + tag);
    }

    // ===== CSV =====
    private static void writeCsv(
            String outFile,
            List<Row> rows,
            int nTriples,
            int nQueries,
            long parseDataNs,
            long parseQueriesNs,
            long buildHexaNs,
            long buildIgNs,
            boolean igEnabled
    ) throws IOException {

        try (BufferedWriter w =
                     Files.newBufferedWriter(Path.of(outFile), StandardCharsets.UTF_8)) {

            w.write("# triples=" + nTriples + "\n");
            w.write("# queries=" + nQueries + "\n");
            w.write(String.format(Locale.ROOT,
                    "# parse_data_ms=%.3f%n", parseDataNs / 1e6));
            w.write(String.format(Locale.ROOT,
                    "# parse_queries_ms=%.3f%n", parseQueriesNs / 1e6));
            w.write(String.format(Locale.ROOT,
                    "# build_hexa_ms=%.3f%n", buildHexaNs / 1e6));
            w.write(igEnabled
                    ? String.format(Locale.ROOT,
                    "# build_integraal_ms=%.3f%n", buildIgNs / 1e6)
                    : "# build_integraal_ms=NA\n");

            w.write("query_id,engine,star_size,answers,exec_ms\n");

            for (Row r : rows) {
                w.write(String.format(Locale.ROOT,
                        "%d,%s,%d,%d,%.6f%n",
                        r.queryId,
                        r.engine,
                        r.starSize,
                        r.answers,
                        r.execNs / 1e6));
            }
        }
    }

    // ===== Integraal Runtime =====
    private static final class IntegraalRuntime {
        boolean available;
        String whyUnavailable;
        Object atomSet;
        Method atomSetAdd;
        Object evaluator;
        List<Method> evalMethods;

        static IntegraalRuntime tryInit() {
            try {
                IntegraalRuntime r = new IntegraalRuntime();

                Class<?> sb =
                        Class.forName("fr.boreal.storage.builder.StorageBuilder");
                r.atomSet =
                        sb.getMethod("getDefaultInMemoryAtomSet").invoke(null);

                for (Method m : r.atomSet.getClass().getMethods()) {
                    if (m.getName().equals("add")) {
                        r.atomSetAdd = m;
                        break;
                    }
                }

                Class<?> ev =
                        Class.forName(
                                "fr.boreal.query_evaluation.generic.DefaultGenericQueryEvaluator");
                r.evaluator =
                        ev.getMethod("defaultInstance").invoke(null);

                r.evalMethods = new ArrayList<>();
                for (Method m : r.evaluator.getClass().getMethods()) {
                    if (m.getName().equals("evaluate")) {
                        r.evalMethods.add(m);
                    }
                }

                r.available = true;
                return r;

            } catch (Throwable t) {
                IntegraalRuntime r = new IntegraalRuntime();
                r.available = false;
                r.whyUnavailable = t.getMessage();
                return r;
            }
        }

        void addFact(Object atom) throws Exception {
            atomSetAdd.invoke(atomSet, atom);
        }

        long countAnswersFastZero(Object foQuery) throws Exception {
            for (Method m : evalMethods) {
                try {
                    Object res = m.invoke(evaluator, foQuery, atomSet);
                    if (res instanceof Stream<?> s) {
                        Iterator<?> it = s.iterator();
                        if (!it.hasNext()) return 0;
                        long c = 1;
                        it.next();
                        while (it.hasNext()) {
                            it.next();
                            c++;
                        }
                        s.close();
                        return c;
                    }
                } catch (IllegalArgumentException ignored) {}
            }
            return 0;
        }
    }

    // ===== Helpers =====
    private static List<RDFTriple> loadTriples(String path) throws IOException {
        List<RDFTriple> triples = new ArrayList<>();
        try (RDFTriplesParser p = new RDFTriplesParser(new File(path))) {
            while (p.hasNext()) {
                triples.add(p.next());
            }
        }
        return triples;
    }

    // WatDiv: each query starts with SELECT
    private static List<StarQuery> loadQueries(String path) throws IOException {
        List<StarQuery> qs = new ArrayList<>();
        String content = Files.readString(Path.of(path));

        for (String block : content.split("(?=SELECT)")) {
            block = block.trim();
            if (block.isEmpty()) continue;

            Path tmp = Files.createTempFile("q", ".sparql");
            Files.writeString(tmp, block, StandardCharsets.UTF_8);

            try (StarQuerySparQLParser p =
                         new StarQuerySparQLParser(tmp.toString())) {
                if (p.hasNext()) {
                    qs.add((StarQuery) p.next());
                }
            } catch (Exception ignored) {
            } finally {
                Files.deleteIfExists(tmp);
            }
        }
        return qs;
    }

    private static long drain(Iterator<?> it) {
        long c = 0;
        while (it.hasNext()) {
            it.next();
            c++;
        }
        return c;
    }

    private static void writeList(String file, List<Integer> ids)
            throws IOException {
        try (BufferedWriter w =
                     Files.newBufferedWriter(Path.of(file),
                             StandardCharsets.UTF_8)) {
            for (int i : ids) {
                w.write(i + "\n");
            }
        }
    }
}
