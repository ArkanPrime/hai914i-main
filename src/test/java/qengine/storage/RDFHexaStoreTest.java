package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFTriple;
import org.junit.jupiter.api.Test;
import qengine.model.StarQuery;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe {@link RDFHexaStore}.
 */
public class RDFHexaStoreTest {
    private static final Literal<String> SUBJECT_1 = SameObjectTermFactory.instance().createOrGetLiteral("subject1");
    private static final Literal<String> PREDICATE_1 = SameObjectTermFactory.instance().createOrGetLiteral("predicate1");
    private static final Literal<String> OBJECT_1 = SameObjectTermFactory.instance().createOrGetLiteral("object1");
    private static final Literal<String> SUBJECT_2 = SameObjectTermFactory.instance().createOrGetLiteral("subject2");
    private static final Literal<String> PREDICATE_2 = SameObjectTermFactory.instance().createOrGetLiteral("predicate2");
    private static final Literal<String> OBJECT_2 = SameObjectTermFactory.instance().createOrGetLiteral("object2");
    private static final Literal<String> OBJECT_3 = SameObjectTermFactory.instance().createOrGetLiteral("object3");
    private static final Variable VAR_X = SameObjectTermFactory.instance().createOrGetVariable("?x");
    private static final Variable VAR_Y = SameObjectTermFactory.instance().createOrGetVariable("?y");


    @Test
    public void testAddAllRDFAtoms() {
        RDFHexaStore store = new RDFHexaStore();

        // Version stream
        // Ajouter plusieurs RDFAtom
        RDFTriple rdfAtom1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFTriple rdfAtom2 = new RDFTriple(SUBJECT_2, PREDICATE_2, OBJECT_2);

        Set<RDFTriple> rdfAtoms = Set.of(rdfAtom1, rdfAtom2);

        assertTrue(store.addAll(rdfAtoms.stream()), "Les RDFAtoms devraient être ajoutés avec succès.");

        // Vérifier que tous les atomes sont présents
        Collection<RDFTriple> atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom1), "La base devrait contenir le premier RDFAtom ajouté.");
        assertTrue(atoms.contains(rdfAtom2), "La base devrait contenir le second RDFAtom ajouté.");

        // Version collection
        store = new RDFHexaStore();
        assertTrue(store.addAll(rdfAtoms), "Les RDFAtoms devraient être ajoutés avec succès.");

        // Vérifier que tous les atomes sont présents
        atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom1), "La base devrait contenir le premier RDFAtom ajouté.");
        assertTrue(atoms.contains(rdfAtom2), "La base devrait contenir le second RDFAtom ajouté.");
    }

    @Test
    public void testAddRDFAtom() {
        RDFHexaStore store = new RDFHexaStore();
        RDFTriple t = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);

        boolean added = store.add(t);
        assertTrue(added, "Le premier ajout doit retourner true.");

        Collection<RDFTriple> atoms = store.getAtoms();
        assertTrue(atoms.contains(t), "La collection d'atomes doit contenir le triplet ajouté.");

        assertEquals(1, store.size(), "La taille du store doit être 1 après un ajout.");
    }


    @Test
    public void testAddDuplicateAtom() {
        RDFHexaStore store = new RDFHexaStore();
        RDFTriple t = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);

        boolean first = store.add(t);
        assertTrue(first, "Premier ajout doit réussir.");

        boolean second = store.add(t);
        assertFalse(second, "Deuxième ajout du même triplet doit retourner false si doublons ignorés.");

        assertEquals(1, store.size(), "La taille ne doit pas augmenter après tentative d'ajout en doublon.");

        Collection<RDFTriple> atoms = store.getAtoms();
        assertTrue(atoms.contains(t), "La collection doit contenir l'atome.");
    }


    @Test
    public void testSize() {
        RDFHexaStore store = new RDFHexaStore();

        assertEquals(0, store.size(), "Taille initiale doit être 0.");

        store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1));
        assertEquals(1, store.size(), "Taille après 1 ajout doit être 1.");

        store.add(new RDFTriple(SUBJECT_2, PREDICATE_1, OBJECT_2));
        assertEquals(2, store.size(), "Taille après 2 ajouts différents doit être 2.");

        store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1));
        assertEquals(2, store.size(), "Taille doit rester 2 après tentative d'ajout en doublon.");
    }


    @Test
    public void testMatchAtom() {
        RDFHexaStore store = new RDFHexaStore();
        store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1)); // RDFAtom(subject1, triple, object1)
        store.add(new RDFTriple(SUBJECT_2, PREDICATE_1, OBJECT_2)); // RDFAtom(subject2, triple, object2)
        store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_3)); // RDFAtom(subject1, triple, object3)

        // Case 1
        RDFTriple matchingAtom = new RDFTriple(SUBJECT_1, PREDICATE_1, VAR_X); // RDFAtom(subject1, predicate1, X)
        Iterator<Substitution> matchedAtoms = store.match(matchingAtom);
        List<Substitution> matchedList = new ArrayList<>();
        matchedAtoms.forEachRemaining(matchedList::add);

        Substitution firstResult = new SubstitutionImpl();
        firstResult.add(VAR_X, OBJECT_1);
        Substitution secondResult = new SubstitutionImpl();
        secondResult.add(VAR_X, OBJECT_3);

        assertEquals(2, matchedList.size(), "There should be two matched RDFAtoms");
        assertTrue(matchedList.contains(firstResult), "Missing substitution: " + firstResult);
        assertTrue(matchedList.contains(secondResult), "Missing substitution: " + secondResult);


    }

        @Test
    public void testHowMany() {
        RDFHexaStore store = new RDFHexaStore();

        // t1 : s1 p1 o1
        RDFTriple t1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);
        // t2 : s1 p1 o2
        RDFTriple t2 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_2);
        // t3 : s1 p2 o2
        RDFTriple t3 = new RDFTriple(SUBJECT_1, PREDICATE_2, OBJECT_2);
        // t4 : s2 p1 o1
        RDFTriple t4 = new RDFTriple(SUBJECT_2, PREDICATE_1, OBJECT_1);

        store.add(t1);
        store.add(t2);
        store.add(t3);
        store.add(t4);

        assertEquals(1,
                store.howMany(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1)),
                "should count exactly 1 existing triple");

        assertEquals(0,
                store.howMany(new RDFTriple(SUBJECT_1, PREDICATE_2, OBJECT_3)),
                "non existing triple should give 0");

        assertEquals(2,
                store.howMany(new RDFTriple(SUBJECT_1, PREDICATE_1, VAR_X)),
                "(s1,p1,?x) should match 2 triples");

        assertEquals(2,
                store.howMany(new RDFTriple(VAR_X, PREDICATE_1, OBJECT_1)),
                "(?x,p1,o1) should match 2 triples");

        assertEquals(2,
                store.howMany(new RDFTriple(SUBJECT_1, VAR_X, OBJECT_2)),
                "(s1,?p,o2) should match 2 triples");


        assertEquals(3,
                store.howMany(new RDFTriple(SUBJECT_1, VAR_X, VAR_Y)),
                "(s1,?p,?o) should match 3 triples");

        assertEquals(3,
                store.howMany(new RDFTriple(VAR_X, PREDICATE_1, VAR_Y)),
                "(?s,p1,?o) should match 3 triples");

        assertEquals(2,
                store.howMany(new RDFTriple(VAR_X, VAR_Y, OBJECT_2)),
                "(?s,?p,o2) should match 2 triples");

        assertEquals(4,
                store.howMany(new RDFTriple(VAR_X, VAR_Y, VAR_X)),
                "all variables should count all stored triples");
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

}
