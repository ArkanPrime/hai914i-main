package qengine.program;

import qengine.model.RDFTriple;
import qengine.model.StarQuery;
import qengine.parser.RDFTriplesParser;
import qengine.parser.StarQuerySparQLParser;
import qengine.storage.RDFHexaStore;
import qengine.storage.RDFStorage;

import fr.boreal.model.logicalElements.api.Substitution;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Benchmark {

    private static final String WORKING_DIR = "data/";
    private static final String DATA_FILE = WORKING_DIR + "data.nt";
    private static final String QUERY_FILE = WORKING_DIR + "all_queries.queryset";
    private static final String QUERY_FILE_10000 = WORKING_DIR + "all_queries_10000.queryset";

    // stabilité benchmark
    private static final int WARMUP_QUERIES = 50;
    private static final int MEASURE_RUNS = 5;
    private static final long SHUFFLE_SEED = 42L;

    public static void main(String[] args) throws IOException {

        // 1) Charger les données
        long t0 = System.nanoTime();
        List<RDFTriple> triples = parseRDFData(DATA_FILE);
        long t1 = System.nanoTime();
        System.out.printf("Data loaded: %,d triples in %.3f s%n", triples.size(), (t1 - t0) / 1e9);

        // 2) Construire le store
        RDFStorage store = new RDFHexaStore();

        long t2 = System.nanoTime();
        for (RDFTriple t : triples) store.add(t);
        long t3 = System.nanoTime();
        System.out.printf("Store built in %.3f s%n", (t3 - t2) / 1e9);

        // 3) Benchmarks
        runBenchmark("ALL_QUERIES", QUERY_FILE, store);
        runBenchmark("ALL_QUERIES_10000", QUERY_FILE_10000, store);
    }

    private static void runBenchmark(String label, String queryFilePath, RDFStorage store) throws IOException {

        // --- parsing queries ---
        long tParse0 = System.nanoTime();
        List<StarQuery> queries = parseSparQLQueries(queryFilePath);
        long tParse1 = System.nanoTime();
        double parseSec = (tParse1 - tParse0) / 1e9;

        // shuffle reproductible (évite un biais d'ordre)
        Collections.shuffle(queries, new Random(SHUFFLE_SEED));

        // warmup JVM
        int warmup = Math.min(WARMUP_QUERIES, queries.size());
        for (int i = 0; i < warmup; i++) {
            // warmup sans mesure fine
            drain(store.match(queries.get(i)));
        }

        // --- runs de mesure (stabilité) : temps total uniquement ---
        double[] runTotalMs = new double[MEASURE_RUNS];
        for (int r = 0; r < MEASURE_RUNS; r++) {
            long start = System.nanoTime();
            for (StarQuery q : queries) {
                drain(store.match(q));
            }
            long end = System.nanoTime();
            runTotalMs[r] = (end - start) / 1e6;
        }

        double minMs = Arrays.stream(runTotalMs).min().orElse(0);
        double maxMs = Arrays.stream(runTotalMs).max().orElse(0);
        double medMs = median(runTotalMs);

        // --- run "profiling" : mesures par requête + CSV (bufferisé) ---
        ProfileSummary prof = profileAndExport(label, queries, store);

        // --- résumé console ---
        System.out.println();
        System.out.println("===== BENCHMARK " + label + " =====");
        System.out.printf("Query file: %s%n", queryFilePath);
        System.out.printf("Queries parsed: %,d%n", queries.size());
        System.out.printf("Parsing time: %.3f s (%.3f ms/query)%n",
                parseSec, queries.isEmpty() ? 0.0 : (parseSec * 1000) / queries.size());

        System.out.printf("Execution runs (ms) over %d runs: min=%.3f  median=%.3f  max=%.3f%n",
                MEASURE_RUNS, minMs, medMs, maxMs);

        System.out.printf("Profiling run: total=%.3f ms (avg %.3f ms/query)%n",
                prof.totalMs, queries.isEmpty() ? 0.0 : prof.totalMs / queries.size());

        System.out.printf("Total answers: %,d%n", prof.totalAnswers);
        System.out.printf("Zero-answer queries: %,d (%.2f%%)%n",
                prof.zeroAnswerQueries,
                queries.isEmpty() ? 0.0 : (100.0 * prof.zeroAnswerQueries) / queries.size());

        System.out.println("Per-query CSV: " + prof.csvPath.toAbsolutePath());
        System.out.println("===================================");
    }

    private static ProfileSummary profileAndExport(String label, List<StarQuery> queries, RDFStorage store) throws IOException {
        Path perQueryCsv = Path.of("benchmark_per_query_" + label + ".csv");

        StringBuilder sb = new StringBuilder(Math.max(1024, queries.size() * 64));
        sb.append("query_id,star_size,answers,match_ms,drain_ms,total_ms\n");

        long totalAnswers = 0;
        long zeroAnswerQueries = 0;

        long execStart = System.nanoTime();

        for (int i = 0; i < queries.size(); i++) {
            StarQuery q = queries.get(i);
            int starSize = safeStarSize(q);

            long t0 = System.nanoTime();
            Iterator<Substitution> it = store.match(q);
            long t1 = System.nanoTime();
            long answers = drain(it);
            long t2 = System.nanoTime();

            double matchMs = (t1 - t0) / 1e6;
            double drainMs = (t2 - t1) / 1e6;
            double totalMs = (t2 - t0) / 1e6;

            totalAnswers += answers;
            if (answers == 0) zeroAnswerQueries++;

            sb.append(i).append(',')
                    .append(starSize).append(',')
                    .append(answers).append(',')
                    .append(formatMs(matchMs)).append(',')
                    .append(formatMs(drainMs)).append(',')
                    .append(formatMs(totalMs)).append('\n');
        }

        long execEnd = System.nanoTime();
        double execTotalMs = (execEnd - execStart) / 1e6;

        Files.writeString(perQueryCsv, sb.toString(), StandardCharsets.UTF_8);

        return new ProfileSummary(perQueryCsv, execTotalMs, totalAnswers, zeroAnswerQueries);
    }

    private static String formatMs(double v) {
        return String.format(Locale.US, "%.3f", v);
    }

    private static double median(double[] a) {
        double[] b = Arrays.copyOf(a, a.length);
        Arrays.sort(b);
        int n = b.length;
        if (n == 0) return 0.0;
        if (n % 2 == 1) return b[n / 2];
        return (b[n / 2 - 1] + b[n / 2]) / 2.0;
    }

    private static long drain(Iterator<Substitution> it) {
        long c = 0;
        while (it.hasNext()) {
            it.next();
            c++;
        }
        return c;
    }

    private static List<RDFTriple> parseRDFData(String rdfFilePath) throws IOException {
        List<RDFTriple> rdfAtoms = new ArrayList<>();
        try (RDFTriplesParser parser =
                     new RDFTriplesParser(new FileReader(rdfFilePath), RDFFormat.NTRIPLES)) {
            while (parser.hasNext()) rdfAtoms.add(parser.next());
        }
        return rdfAtoms;
    }

    private static List<StarQuery> parseSparQLQueries(String queryFilePath) throws IOException {
        List<StarQuery> starQueries = new ArrayList<>();
        String content = Files.readString(Path.of(queryFilePath));

        List<String> blocks = new ArrayList<>(List.of(content.split("\\R\\s*\\R\\s*\\R")));
        List<String> finalBlocks = new ArrayList<>();

        for (String b : blocks) {
            String trimmed = b.trim();
            if (trimmed.isEmpty()) continue;

            String[] sub = trimmed.split("}\\s*(?=SELECT\\b)");
            for (String s : sub) {
                String piece = s.trim();
                if (piece.isEmpty()) continue;
                if (!piece.endsWith("}")) piece = piece + " }";
                finalBlocks.add(piece);
            }
        }

        int invalid = 0;
        for (String q : finalBlocks) {
            String queryText = q.trim();
            if (queryText.isEmpty()) continue;

            Path tmp = Files.createTempFile("query_", ".sparql");
            Files.writeString(tmp, queryText, StandardCharsets.UTF_8);

            try (StarQuerySparQLParser parser = new StarQuerySparQLParser(tmp.toString())) {
                if (parser.hasNext()) {
                    var query = parser.next();
                    if (query instanceof StarQuery sq) starQueries.add(sq);
                }
            } catch (RuntimeException e) {
                invalid++;
            } finally {
                Files.deleteIfExists(tmp);
            }
        }

        if (invalid > 0) {
            System.err.printf("WARNING: %d invalid queries skipped in %s%n", invalid, queryFilePath);
        }

        return starQueries;
    }

    /**
     * On essaie de récupérer la taille de l'étoile sans dépendre d'une API précise.
     * Si on ne trouve pas, on renvoie -1.
     */
    private static int safeStarSize(StarQuery q) {
        // essaie méthodes courantes via reflection (getTriples / getAtoms / size)
        try {
            Method m = q.getClass().getMethod("getTriples");
            Object res = m.invoke(q);
            if (res instanceof Collection<?> c) return c.size();
        } catch (Exception ignored) { }

        try {
            Method m = q.getClass().getMethod("getAtoms");
            Object res = m.invoke(q);
            if (res instanceof Collection<?> c) return c.size();
        } catch (Exception ignored) { }

        try {
            Method m = q.getClass().getMethod("size");
            Object res = m.invoke(q);
            if (res instanceof Integer i) return i;
            if (res instanceof Number n) return n.intValue();
        } catch (Exception ignored) { }

        return -1;
    }

    private record ProfileSummary(Path csvPath, double totalMs, long totalAnswers, long zeroAnswerQueries) { }
}
