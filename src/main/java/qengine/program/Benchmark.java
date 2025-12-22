package qengine.program;

import fr.boreal.model.formula.api.FOFormula;
import fr.boreal.model.formula.api.FOFormulaConjunction;
import fr.boreal.model.kb.api.FactBase;
import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.queryEvaluation.api.FOQueryEvaluator;
import fr.boreal.query_evaluation.generic.GenericFOQueryEvaluator;
import fr.boreal.storage.natives.SimpleInMemoryGraphStore;
import org.eclipse.rdf4j.rio.RDFFormat;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;
import qengine.parser.RDFTriplesParser;
import qengine.parser.StarQuerySparQLParser;
import qengine.storage.RDFHexaStore;
import qengine.storage.RDFStorage;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Benchmark {

    // ===== DATA =====
    private static final String DATA_FILE = "data/data.nt";

    // ===== QUERY FILES =====
    private static final String[] QUERY_FILES = {
            "data/all_queries.queryset",
            "data/all_queries_10000.queryset"
    };

    // ===== OUTPUT =====
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

    public static void main(String[] args) throws Exception {

        // ---- Parse data once (common) ----
        long tParseData0 = System.nanoTime();
        List<RDFTriple> triples = parseRdfData(DATA_FILE);
        long parseDataNs = System.nanoTime() - tParseData0;
        System.out.printf("Triples parsed: %,d in %.3f s%n", triples.size(), parseDataNs / 1e9);

        // ---- Build HEXA store ----
        RDFStorage hexa = new RDFHexaStore();
        long tHexaBuild0 = System.nanoTime();
        for (RDFTriple t : triples) {
            hexa.add(t);
        }
        long buildHexaNs = System.nanoTime() - tHexaBuild0;
        System.out.printf("HEXA load/build: %.3f s%n", buildHexaNs / 1e9);

        // ---- Build INTEGRAAL store (FactBase) ----
        FactBase factBase = new SimpleInMemoryGraphStore();
        long tIgBuild0 = System.nanoTime();
        for (RDFTriple t : triples) {
            factBase.add(t);
        }
        long buildIgNs = System.nanoTime() - tIgBuild0;
        System.out.printf("INTEGRAAL load/build: %.3f s%n", buildIgNs / 1e9);

        // ---- Evaluator instance once ----
        FOQueryEvaluator<FOFormula> evaluator = GenericFOQueryEvaluator.defaultInstance();

        // ---- Run benchmarks for each query file ----
        for (String qf : QUERY_FILES) {
            runBenchmarkForQueryFile(qf, triples.size(), parseDataNs, buildHexaNs, buildIgNs, hexa, factBase, evaluator);
        }

        System.out.println("All benchmarks finished.");
    }

    private static void runBenchmarkForQueryFile(
            String queryFile,
            int nTriples,
            long parseDataNs,
            long buildHexaNs,
            long buildIgNs,
            RDFStorage hexa,
            FactBase factBase,
            FOQueryEvaluator<FOFormula> evaluator
    ) throws Exception {

        String tag = tagFromQueryFile(queryFile);
        System.out.println("\n=== Benchmark: " + queryFile + " (tag=" + tag + ") ===");

        long tParseQ0 = System.nanoTime();
        List<StarQuery> queries = loadQueriesRobust(queryFile);
        long parseQueriesNs = System.nanoTime() - tParseQ0;
        System.out.printf("Queries parsed: %,d in %.3f s%n", queries.size(), parseQueriesNs / 1e9);

        List<Row> rows = new ArrayList<>(queries.size() * 2);
        List<Integer> zeroHexa = new ArrayList<>();
        List<Integer> zeroIg = new ArrayList<>();

        for (int i = 0; i < queries.size(); i++) {
            StarQuery q = queries.get(i);
            int starSize = q.getRdfAtoms().size();

            // ---- HEXA ----
            long h0 = System.nanoTime();
            Iterator<Substitution> hit = hexa.match(q);
            long hAns = countFastZero(hit);
            long hNs = System.nanoTime() - h0;
            rows.add(new Row(i, Engine.HEXA, starSize, hAns, hNs));
            if (hAns == 0) zeroHexa.add(i);

            // ---- INTEGRAAL ----
            long ig0 = System.nanoTime();
            // typed query from your example
            var foQuery = q.asFOQuery(); // FOQuery<FOFormulaConjunction>
            Iterator<Substitution> it = evaluator.evaluate(foQuery, factBase);
            long igAns = countFastZero(it);
            long igNs = System.nanoTime() - ig0;

            rows.add(new Row(i, Engine.INTEGRAAL, starSize, igAns, igNs));
            if (igAns == 0) zeroIg.add(i);

            if (i % 500 == 0 && i > 0) {
                System.out.printf("Progress (%s): %d / %d%n", tag, i, queries.size());
            }
        }

        String csv = CSV_PREFIX + tag + ".csv";
        writeCsv(csv, rows,
                DATA_FILE, queryFile,
                nTriples, queries.size(),
                parseDataNs, parseQueriesNs,
                buildHexaNs, buildIgNs);

        writeList(ZERO_HEXA_PREFIX + tag + ".txt", zeroHexa);
        writeList(ZERO_INTEGRAAL_PREFIX + tag + ".txt", zeroIg);

        System.out.println("Done: " + tag + " -> " + csv);
    }

    // ===== Counting (fast zero-check) =====
    private static long countFastZero(Iterator<?> it) {
        if (!it.hasNext()) return 0;
        long c = 1;
        it.next();
        while (it.hasNext()) {
            it.next();
            c++;
        }
        return c;
    }

    // ===== Data parsing (same as your Example) =====
    private static List<RDFTriple> parseRdfData(String rdfFilePath) throws IOException {
        List<RDFTriple> rdfAtoms = new ArrayList<>();
        try (FileReader rdfFile = new FileReader(rdfFilePath);
             RDFTriplesParser rdfAtomParser = new RDFTriplesParser(rdfFile, RDFFormat.NTRIPLES)) {
            while (rdfAtomParser.hasNext()) {
                rdfAtoms.add(rdfAtomParser.next());
            }
        }
        return rdfAtoms;
    }

    // ===== Query parsing (robust for WatDiv queryset) =====
    // We split concatenated queries on "SELECT" and parse each as a temp file.
    private static List<StarQuery> loadQueriesRobust(String queryFilePath) throws IOException {
        List<StarQuery> starQueries = new ArrayList<>();
        String content = Files.readString(Path.of(queryFilePath));

        for (String block : content.split("(?=SELECT)")) {
            block = block.trim();
            if (block.isEmpty()) continue;

            Path tmp = Files.createTempFile("q", ".sparql");
            Files.writeString(tmp, block, StandardCharsets.UTF_8);

            try (StarQuerySparQLParser queryParser = new StarQuerySparQLParser(tmp.toString())) {
                if (queryParser.hasNext()) {
                    var query = queryParser.next();
                    if (query instanceof StarQuery sq) {
                        starQueries.add(sq);
                    }
                }
            } catch (Exception ignored) {
                // skip invalid blocks
            } finally {
                Files.deleteIfExists(tmp);
            }
        }
        return starQueries;
    }

    // ===== CSV =====
    private static void writeCsv(
            String outFile,
            List<Row> rows,
            String dataPath,
            String queryPath,
            int nTriples,
            int nQueries,
            long parseDataNs,
            long parseQueriesNs,
            long buildHexaNs,
            long buildIgNs
    ) throws IOException {

        try (BufferedWriter w = Files.newBufferedWriter(Path.of(outFile), StandardCharsets.UTF_8)) {
            w.write("# data_path=" + dataPath + "\n");
            w.write("# query_path=" + queryPath + "\n");
            w.write("# triples=" + nTriples + "\n");
            w.write("# queries=" + nQueries + "\n");
            w.write(String.format(Locale.ROOT, "# parse_data_ms=%.3f%n", parseDataNs / 1e6));
            w.write(String.format(Locale.ROOT, "# parse_queries_ms=%.3f%n", parseQueriesNs / 1e6));
            w.write(String.format(Locale.ROOT, "# build_hexa_ms=%.3f%n", buildHexaNs / 1e6));
            w.write(String.format(Locale.ROOT, "# build_integraal_ms=%.3f%n", buildIgNs / 1e6));

            w.write("query_id,engine,star_size,answers,exec_ms\n");
            for (Row r : rows) {
                w.write(String.format(Locale.ROOT,
                        "%d,%s,%d,%d,%.6f%n",
                        r.queryId, r.engine, r.starSize, r.answers, r.execNs / 1e6));
            }
        }
    }

    private static void writeList(String file, List<Integer> ids) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(Path.of(file), StandardCharsets.UTF_8)) {
            for (int id : ids) {
                w.write(id + "\n");
            }
        }
    }

    private static String tagFromQueryFile(String qf) {
        String name = Path.of(qf).getFileName().toString();
        if (name.contains("10000")) return "10000";
        if (name.contains("all_queries")) return "all";
        return name.replaceAll("[^a-zA-Z0-9]+", "_");
    }
}
