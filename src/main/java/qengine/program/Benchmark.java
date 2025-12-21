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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Benchmark {

    private static final String WORKING_DIR = "data/";
    private static final String DATA_FILE = WORKING_DIR + "data.nt";
    private static final String QUERY_FILE = WORKING_DIR + "all_queries.queryset";
    private static final String QUERY_FILE_10000 = WORKING_DIR + "all_queries_10000.queryset";

    public static void main(String[] args) throws IOException {

        // 1) Charger les données
        long t0 = System.nanoTime();
        List<RDFTriple> triples = parseRDFData(DATA_FILE);
        long t1 = System.nanoTime();

        System.out.printf("Data loaded: %,d triples in %.3f s%n",
                triples.size(), (t1 - t0) / 1e9);

        // 2) Construire le store
        RDFStorage store = new RDFHexaStore();

        long t2 = System.nanoTime();
        for (RDFTriple t : triples) {
            store.add(t);
        }
        long t3 = System.nanoTime();

        System.out.printf("Store built in %.3f s%n", (t3 - t2) / 1e9);

        // 3) Benchmarks
        runBenchmark("ALL_QUERIES", QUERY_FILE, store);
        runBenchmark("ALL_QUERIES_10000", QUERY_FILE_10000, store);
    }

    private static void runBenchmark(String label, String queryFilePath, RDFStorage store) throws IOException {
        List<StarQuery> queries = parseSparQLQueries(queryFilePath);

        // warmup JVM
        int warmup = Math.min(50, queries.size());
        for (int i = 0; i < warmup; i++) {
            drain(store.match(queries.get(i)));
        }

        long start = System.nanoTime();
        long totalAnswers = 0;

        for (StarQuery q : queries) {
            totalAnswers += drain(store.match(q));
        }

        long end = System.nanoTime();
        double sec = (end - start) / 1e9;

        System.out.println();
        System.out.println("===== BENCHMARK " + label + " =====");
        System.out.printf("Queries: %,d%n", queries.size());
        System.out.printf("Total answers: %,d%n", totalAnswers);
        System.out.printf("Total time: %.3f s%n", sec);
        System.out.printf("Avg per query: %.3f ms%n", (sec * 1000) / queries.size());
        System.out.println("===================================");
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
            while (parser.hasNext()) {
                rdfAtoms.add(parser.next());
            }
        }
        return rdfAtoms;
    }

    private static List<StarQuery> parseSparQLQueries(String queryFilePath) throws IOException {
        List<StarQuery> starQueries = new ArrayList<>();

        String content = Files.readString(Path.of(queryFilePath));

        // 1) split principal: au moins deux lignes vides
        List<String> blocks = new ArrayList<>(List.of(content.split("\\R\\s*\\R\\s*\\R")));

        // 2) split de secours: si un bloc contient plusieurs SELECT collés, on découpe sur "} SELECT"
        List<String> finalBlocks = new ArrayList<>();
        for (String b : blocks) {
            String trimmed = b.trim();
            if (trimmed.isEmpty()) continue;

            // lookahead : on garde le "SELECT" du morceau suivant
            String[] sub = trimmed.split("}\\s*(?=SELECT\\b)");
            for (int i = 0; i < sub.length; i++) {
                String piece = sub[i].trim();
                if (piece.isEmpty()) continue;
                // on remet le '}' perdu par le split (sauf si déjà présent)
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
                    if (query instanceof StarQuery sq) {
                        starQueries.add(sq);
                    }
                }
            } catch (RuntimeException e) {
                // On skip les requêtes mal formées au lieu de tuer le benchmark
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
}
