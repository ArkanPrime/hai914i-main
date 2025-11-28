package qengine.storage;

import fr.boreal.model.logicalElements.api.Literal;
import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Variable;
import fr.boreal.model.logicalElements.factory.api.TermFactory;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import org.junit.jupiter.api.Test;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RDFGiantTableTest {

    private static final TermFactory F = SameObjectTermFactory.instance();

    private static final Literal<String> SUBJECT_1   = F.createOrGetLiteral("subject1");
    private static final Literal<String> PREDICATE_1 = F.createOrGetLiteral("predicate1");
    private static final Literal<String> OBJECT_1    = F.createOrGetLiteral("object1");
    private static final Literal<String> SUBJECT_2   = F.createOrGetLiteral("subject2");
    private static final Literal<String> PREDICATE_2 = F.createOrGetLiteral("predicate2");
    private static final Literal<String> OBJECT_2    = F.createOrGetLiteral("object2");
    private static final Literal<String> OBJECT_3    = F.createOrGetLiteral("object3");
    private static final Variable VAR_X              = F.createOrGetVariable("?x");
    private static final Variable VAR_Y              = F.createOrGetVariable("?y");

    // Helper pour créer rapidement un RDFTriple à partir de chaînes (tu avais déjà ce helper)
    private static RDFTriple t(String s, String p, String o) {
        return new RDFTriple(
                F.createOrGetLiteral(s),
                F.createOrGetLiteral(p),
                F.createOrGetLiteral(o)
        );
    }

    @Test
    void add() {
        RDFGiantTable store = new RDFGiantTable();
        RDFTriple x = t("Bob", "knows", "Alice");

        // premier ajout
        assertTrue(store.add(x), "Premier ajout doit réussir");
        assertEquals(1, store.size(), "La taille doit refléter le nb de triplets distincts");
        assertTrue(store.getAtoms().contains(x), "Le triplet doit être présent");

        // doublon rejeté
        assertFalse(store.add(x), "Le même triplet ne doit pas être ajouté deux fois");
        assertEquals(1, store.size(), "Un doublon ne doit pas augmenter la taille");
    }

    @Test
    void size() {
        RDFGiantTable store = new RDFGiantTable();
        RDFTriple a = t("s1", "p1", "o1");
        RDFTriple b = t("s2", "p2", "o2");

        store.add(a);
        store.add(b);

        assertEquals(2, store.size(), "Deux triplets distincts ⇒ size = 2");
    }

    @Test
    void getAtoms() {
        RDFGiantTable store = new RDFGiantTable();
        RDFTriple a = t("s1", "p1", "o1");
        RDFTriple b = t("s2", "p2", "o2");

        store.add(a);
        store.add(b);

        Collection<RDFTriple> atoms = store.getAtoms();
        assertTrue(atoms.containsAll(List.of(a, b)), "getAtoms doit contenir tous les triplets ajoutés");

        // la collection doit être non-modifiable
        assertThrows(UnsupportedOperationException.class, () -> atoms.add(t("x", "y", "z")),
                "getAtoms doit renvoyer une vue non modifiable");
    }

    @Test
    void testMatchBasic() {
        RDFGiantTable store = new RDFGiantTable();

        // ajouter des triples
        store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1)); // (subject1, predicate1, object1)
        store.add(new RDFTriple(SUBJECT_2, PREDICATE_1, OBJECT_2)); // (subject2, predicate1, object2)
        store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_3)); // (subject1, predicate1, object3)

        // pattern : (subject1, predicate1, ?x) -> doit correspondre à object1 et object3
        RDFTriple pattern = new RDFTriple(SUBJECT_1, PREDICATE_1, VAR_X);
        List<Substitution> results = new ArrayList<>();
        store.match(pattern).forEachRemaining(results::add);

        assertEquals(2, results.size(), "Deux substitutions attendues pour (subject1,predicate1,?x)");

        SubstitutionImpl expected1 = new SubstitutionImpl();
        expected1.add(VAR_X, OBJECT_1);
        SubstitutionImpl expected2 = new SubstitutionImpl();
        expected2.add(VAR_X, OBJECT_3);

        assertTrue(results.contains(expected1), "La substitution pour object1 doit être présente");
        assertTrue(results.contains(expected2), "La substitution pour object3 doit être présente");

        // autre pattern : (?x, predicate1, object2) -> subject2
        RDFTriple pattern2 = new RDFTriple(VAR_X, PREDICATE_1, OBJECT_2);
        List<Substitution> results2 = new ArrayList<>();
        store.match(pattern2).forEachRemaining(results2::add);
        assertEquals(1, results2.size(), "Une substitution attendue pour (?x,predicate1,object2)");
        SubstitutionImpl expected3 = new SubstitutionImpl();
        expected3.add(VAR_X, SUBJECT_2);
        assertTrue(results2.contains(expected3), "La substitution pour subject2 doit être présente");
    }

    @Test
    void testHowMany() {
        RDFGiantTable store = new RDFGiantTable();
        store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1));
        store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_3));
        // one different predicate so it doesn't match
        store.add(new RDFTriple(SUBJECT_1, PREDICATE_2, OBJECT_2));

        long count = store.howMany(new RDFTriple(SUBJECT_1, PREDICATE_1, VAR_X));
        assertEquals(2, count, "howMany doit compter 2 triplets pour (subject1,predicate1,?x)");
    }

    @Test
    public void testMatchStarQuery() {
        RDFHexaStore store = new RDFHexaStore();

        // --- Données dans le store ---
        // subject1 predicate1 object1
        // subject1 predicate2 object2
        // subject2 predicate1 object1
        RDFTriple t1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFTriple t2 = new RDFTriple(SUBJECT_1, PREDICATE_2, OBJECT_2);
        RDFTriple t3 = new RDFTriple(SUBJECT_2, PREDICATE_1, OBJECT_1);

        store.add(t1);
        store.add(t2);
        store.add(t3);

        // --- StarQuery : centre ?x ---
        // ?x predicate1 object1
        // ?x predicate2 object2
        RDFTriple q1 = new RDFTriple(VAR_X, PREDICATE_1, OBJECT_1);
        RDFTriple q2 = new RDFTriple(VAR_X, PREDICATE_2, OBJECT_2);

        List<RDFTriple> atoms = Arrays.asList(q1, q2);
        Collection<Variable> answerVars = List.of(VAR_X);

        StarQuery starQuery = new StarQuery("q1", atoms, answerVars);

        // --- Exécution de la requête en étoile ---
        Iterator<Substitution> it = store.match(starQuery);
        List<Substitution> results = new ArrayList<>();
        it.forEachRemaining(results::add);

        // On s'attend à UNE seule substitution : ?x -> subject1
        assertEquals(1, results.size(), "On doit trouver exactement une réponse à la star query.");

        Substitution expected = new SubstitutionImpl();
        expected.add(VAR_X, SUBJECT_1);

        assertTrue(results.contains(expected),
                "La substitution attendue (?x -> subject1) doit être présente parmi les résultats.");
    }

            @Test
        public void testMatchStarQuery_MultipleSolutions() {
        RDFHexaStore store = new RDFHexaStore();

        // Données :
        // s1 p1 o1
        // s1 p2 o2
        // s2 p1 o1
        // s2 p2 o2
        RDFTriple t1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFTriple t2 = new RDFTriple(SUBJECT_1, PREDICATE_2, OBJECT_2);
        RDFTriple t3 = new RDFTriple(SUBJECT_2, PREDICATE_1, OBJECT_1);
        RDFTriple t4 = new RDFTriple(SUBJECT_2, PREDICATE_2, OBJECT_2);

        store.add(t1);
        store.add(t2);
        store.add(t3);
        store.add(t4);

        // StarQuery (centre ?x) :
        // ?x p1 o1
        // ?x p2 o2
        RDFTriple q1 = new RDFTriple(VAR_X, PREDICATE_1, OBJECT_1);
        RDFTriple q2 = new RDFTriple(VAR_X, PREDICATE_2, OBJECT_2);

        List<RDFTriple> atoms = Arrays.asList(q1, q2);
        Collection<Variable> answerVars = List.of(VAR_X);

        StarQuery starQuery = new StarQuery("q_multi", atoms, answerVars);

        Iterator<Substitution> it = store.match(starQuery);
        List<Substitution> results = new ArrayList<>();
        it.forEachRemaining(results::add);

        // On doit avoir 2 réponses : ?x -> subject1 et ?x -> subject2
        assertEquals(2, results.size(), "La star query doit retourner deux solutions.");

        Substitution sigma1 = new SubstitutionImpl();
        sigma1.add(VAR_X, SUBJECT_1);

        Substitution sigma2 = new SubstitutionImpl();
        sigma2.add(VAR_X, SUBJECT_2);

        assertTrue(results.contains(sigma1), "La solution ?x -> subject1 doit être présente.");
        assertTrue(results.contains(sigma2), "La solution ?x -> subject2 doit être présente.");
    }

        @Test
        public void testMatchStarQuery_MultipleVariables() {
        RDFHexaStore store = new RDFHexaStore();

        // On réutilise les mêmes constantes pour la lisibilité
        // s1 p1 o1
        // s1 p2 o2
        // s2 p1 o1
        // s2 p2 o3
        RDFTriple t1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFTriple t2 = new RDFTriple(SUBJECT_1, PREDICATE_2, OBJECT_2);
        RDFTriple t3 = new RDFTriple(SUBJECT_2, PREDICATE_1, OBJECT_1);
        RDFTriple t4 = new RDFTriple(SUBJECT_2, PREDICATE_2, OBJECT_3);

        store.add(t1);
        store.add(t2);
        store.add(t3);
        store.add(t4);

        // StarQuery :
        // ?x p1 ?y
        // ?x p2 ?z
        RDFTriple q1 = new RDFTriple(VAR_X, PREDICATE_1, VAR_Y);
        RDFTriple q2 = new RDFTriple(VAR_X, PREDICATE_2, SameObjectTermFactory.instance().createOrGetVariable("?z"));

        Variable VAR_Z = SameObjectTermFactory.instance().createOrGetVariable("?z");

        List<RDFTriple> atoms = Arrays.asList(q1, q2);
        Collection<Variable> answerVars = List.of(VAR_X, VAR_Y, VAR_Z);

        StarQuery starQuery = new StarQuery("q_vars", atoms, answerVars);

        Iterator<Substitution> it = store.match(starQuery);
        List<Substitution> results = new ArrayList<>();
        it.forEachRemaining(results::add);

        // On doit obtenir deux solutions :
        // 1) ?x = s1, ?y = o1, ?z = o2
        // 2) ?x = s2, ?y = o1, ?z = o3
        assertEquals(2, results.size(), "La star query doit retourner deux solutions.");

        Substitution s1 = new SubstitutionImpl();
        s1.add(VAR_X, SUBJECT_1);
        s1.add(VAR_Y, OBJECT_1);
        s1.add(VAR_Z, OBJECT_2);

        Substitution s2 = new SubstitutionImpl();
        s2.add(VAR_X, SUBJECT_2);
        s2.add(VAR_Y, OBJECT_1);
        s2.add(VAR_Z, OBJECT_3);

        assertTrue(results.contains(s1), "La solution pour subject1 doit être présente.");
        assertTrue(results.contains(s2), "La solution pour subject2 doit être présente.");
    }

        @Test
        public void testMatchStarQuery_NoSolution() {
        RDFHexaStore store = new RDFHexaStore();

        // Données :
        // s1 p1 o1
        // s1 p2 o2
        RDFTriple t1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFTriple t2 = new RDFTriple(SUBJECT_1, PREDICATE_2, OBJECT_2);

        store.add(t1);
        store.add(t2);

        // Star incohérente :
        // ?x p1 o1   (ok : ?x = s1)
        // ?x p2 o1   (n'existe pas : p2,o1 n'est jamais ensemble)
        RDFTriple q1 = new RDFTriple(VAR_X, PREDICATE_1, OBJECT_1);
        RDFTriple q2 = new RDFTriple(VAR_X, PREDICATE_2, OBJECT_1); // objet différent de t2

        List<RDFTriple> atoms = Arrays.asList(q1, q2);
        Collection<Variable> answerVars = List.of(VAR_X);

        StarQuery starQuery = new StarQuery("q_none", atoms, answerVars);

        Iterator<Substitution> it = store.match(starQuery);
        List<Substitution> results = new ArrayList<>();
        it.forEachRemaining(results::add);

        assertTrue(results.isEmpty(), "La star query doit retourner 0 solution car les branches sont incompatibles.");
    }

}
