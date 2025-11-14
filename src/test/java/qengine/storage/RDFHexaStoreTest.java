package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFTriple;
import org.junit.jupiter.api.Test;

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
    public void testMatchStarQuery() {
        throw new NotImplementedException();
    }

    // Vos autres tests d'HexaStore ici
}
