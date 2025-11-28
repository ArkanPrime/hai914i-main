package qengine.program;

import fr.boreal.model.formula.api.FOFormula;
import fr.boreal.model.formula.api.FOFormulaConjunction;
import fr.boreal.model.kb.api.FactBase;
import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.api.Variable;
import fr.boreal.model.query.api.FOQuery;
import fr.boreal.model.queryEvaluation.api.FOQueryEvaluator;
import fr.boreal.query_evaluation.generic.GenericFOQueryEvaluator;
import fr.boreal.storage.natives.SimpleInMemoryGraphStore;
import org.eclipse.rdf4j.rio.RDFFormat;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;
import qengine.parser.RDFTriplesParser;
import qengine.parser.StarQuerySparQLParser;
import qengine.storage.RDFHexaStore;
import qengine.storage.RDFGiantTable;
import qengine.storage.RDFStorage;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Vérification de correction et complétude :
 * compare les résultats de votre store RDF (HexaStore / GiantTable)
 * avec ceux d'InteGraal (SimpleInMemoryGraphStore + GenericFOQueryEvaluator).
 */
public final class Verification {

    private static final String WORKING_DIR = "data/";
    private static final String DATA_FILE = WORKING_DIR + "sample_data.nt";
    private static final String QUERY_FILE = WORKING_DIR + "sample_query.queryset";

    public static void main(String[] args) throws IOException {
        // 1) Charger les données RDF
        List<RDFTriple> triples = parseRDFData(DATA_FILE);

        // 2) Charger les requêtes en étoile
        List<StarQuery> queries = parseSparQLQueries(QUERY_FILE);

        // 3) Créer l’oracle InteGraal (store de référence)
        FactBase oracle = new SimpleInMemoryGraphStore();
        for (RDFTriple t : triples) {
            oracle.add(t);
        }

        // 4) Créer votre store à tester
        //    Choisissez ici lequel vous voulez tester :
        RDFStorage store = new RDFHexaStore();
        // RDFStorage store = new RDFGiantTable();

        for (RDFTriple t : triples) {
            store.add(t);
        }

        // 5) Préparer l'évaluateur de requêtes InteGraal
        FOQueryEvaluator<FOFormula> evaluator = GenericFOQueryEvaluator.defaultInstance();

        int ok = 0;
        int ko = 0;

        for (StarQuery q : queries) {
            boolean same = compareAnswers(q, oracle, store, evaluator);
            if (same) {
                ok++;
            } else {
                ko++;
            }
        }

        System.out.println();
        System.out.println("===== RÉSUMÉ CORRECTION / COMPLÉTUDE =====");
        System.out.println("Requêtes correctes     : " + ok);
        System.out.println("Requêtes différentes   : " + ko);
        System.out.println("==========================================");
    }

    /**
     * Parse un fichier RDF en liste de RDFTriple.
     */
    private static List<RDFTriple> parseRDFData(String rdfFilePath) throws IOException {
        FileReader rdfFile = new FileReader(rdfFilePath);
        List<RDFTriple> rdfAtoms = new ArrayList<>();

        try (RDFTriplesParser rdfAtomParser = new RDFTriplesParser(rdfFile, RDFFormat.NTRIPLES)) {
            while (rdfAtomParser.hasNext()) {
                RDFTriple triple = rdfAtomParser.next();
                rdfAtoms.add(triple);
            }
        }
        return rdfAtoms;
    }

    /**
     * Parse un fichier de requêtes SparQL en liste de StarQuery.
     */
    private static List<StarQuery> parseSparQLQueries(String queryFilePath) throws IOException {
        List<StarQuery> starQueries = new ArrayList<>();

        try (StarQuerySparQLParser queryParser = new StarQuerySparQLParser(queryFilePath)) {
            while (queryParser.hasNext()) {
                var q = queryParser.next();
                if (q instanceof StarQuery sq) {
                    starQueries.add(sq);
                }
            }
        }
        return starQueries;
    }

    /**
     * Compare les réponses de l'oracle InteGraal et de votre store
     * pour une requête en étoile donnée.
     *
     * Affiche TOUJOURS la requête.
     * Si les réponses sont identiques : (OK)
     * Sinon : affiche Oracle vs Système.
     *
     * @return true si les ensembles de réponses sont exactement égaux.
     */
    private static boolean compareAnswers(StarQuery starQuery,
                                          FactBase oracle,
                                          RDFStorage store,
                                          FOQueryEvaluator<FOFormula> evaluator) {

        // --- Résultats oracle (InteGraal) ---
        FOQuery<FOFormulaConjunction> foQuery = starQuery.asFOQuery();
        Iterator<Substitution> itOracle = evaluator.evaluate(foQuery, oracle);
        Set<Map<Variable, Term>> oracleAnswers = new HashSet<>();
        while (itOracle.hasNext()) {
            Substitution s = itOracle.next();
            oracleAnswers.add(s.toMap());
        }

        // --- Résultats de votre système ---
        Iterator<Substitution> itSys = store.match(starQuery);
        Set<Map<Variable, Term>> sysAnswers = new HashSet<>();
        while (itSys.hasNext()) {
            Substitution s = itSys.next();
            sysAnswers.add(s.toMap());
        }

        boolean same = oracleAnswers.equals(sysAnswers);

        // 🔥 Affichage systématique de la requête
        System.out.println("\n===============================");
        System.out.println("REQUÊTE : " + starQuery.getLabel());
        printStarQuery(starQuery);

        if (same) {
            System.out.println("✔ Résultats IDENTIQUES (OK)");
            if (oracleAnswers.isEmpty()) {
                System.out.println("  Réponses : (aucune)");
            } else {
                System.out.println("  Réponses :");
                for (Map<Variable, Term> m : oracleAnswers) {
                    System.out.println("    " + m);
                }
            }
        } else {
            System.out.println("Résultats DIFFÉRENTS (PROBLÈME)");
            System.out.println("=== Oracle InteGraal ===");
            if (oracleAnswers.isEmpty()) {
                System.out.println("  (aucune réponse)");
            } else {
                for (Map<Variable, Term> m : oracleAnswers) {
                    System.out.println("  " + m);
                }
            }

            System.out.println("=== Votre Store ===");
            if (sysAnswers.isEmpty()) {
                System.out.println("  (aucune réponse)");
            } else {
                for (Map<Variable, Term> m : sysAnswers) {
                    System.out.println("  " + m);
                }
            }
        }

        return same;
    }

    /**
     * Affichage lisible d'une StarQuery pour la comparaison visuelle.
     */
    private static void printStarQuery(StarQuery q) {
        System.out.println("  Variable centrale : " + q.getCentralVariable());
        System.out.println("  Variables réponses : " + q.getAnswerVariables());
        System.out.println("  Triplets :");
        for (RDFTriple t : q.getRdfAtoms()) {
            System.out.println("    " + t);
        }
    }
}
