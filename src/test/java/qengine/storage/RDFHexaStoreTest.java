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
        // Création du store
        RDFHexaStore store = new RDFHexaStore();

        // Création d’un triplet RDF simple
        var subject = SameObjectTermFactory.instance().createOrGetLiteral("Bob");
        var predicate = SameObjectTermFactory.instance().createOrGetLiteral("knows");
        var object = SameObjectTermFactory.instance().createOrGetLiteral("Alice");
        RDFTriple triple = new RDFTriple(subject, predicate, object);

        // Ajout du triplet
        boolean added = store.add(triple);

        // Vérifications
        assertTrue(added, "Le triplet devrait être ajouté avec succès.");

    }
    @Test
    public void testEncodeDecode() {
        RDFHexaStore store = new RDFHexaStore();

        // Encodage de plusieurs termes
        int idBob1 = store.encode("Bob");
        int idKnows = store.encode("knows");
        int idAlice = store.encode("Alice");

        // Le même terme doit donner le même ID
        int idBob2 = store.encode("Bob");
        assertEquals(idBob1, idBob2, "Le même terme 'Bob' doit avoir le même identifiant.");

        // Les termes différents doivent avoir des IDs différents
        assertNotEquals(idBob1, idKnows, "'Bob' et 'knows' doivent avoir des identifiants différents.");
        assertNotEquals(idKnows, idAlice, "'knows' et 'Alice' doivent avoir des identifiants différents.");

        // Le décodage doit renvoyer les termes originaux
        assertEquals("Bob", store.decode(idBob1), "Le décodage doit retrouver 'Bob'.");
        assertEquals("knows", store.decode(idKnows), "Le décodage doit retrouver 'knows'.");
        assertEquals("Alice", store.decode(idAlice), "Le décodage doit retrouver 'Alice'.");
    }

    @Test
    public void testAddDuplicateAtom() {
        RDFHexaStore store = new RDFHexaStore();

        // Création d’un triplet RDF
        var subject = SameObjectTermFactory.instance().createOrGetLiteral("Bob");
        var predicate = SameObjectTermFactory.instance().createOrGetLiteral("knows");
        var object = SameObjectTermFactory.instance().createOrGetLiteral("Alice");
        RDFTriple triple = new RDFTriple(subject, predicate, object);

        // Premier ajout
        boolean added = store.add(triple);
        assertTrue(added, "Le premier ajout du triplet doit réussir.");

        // Deuxième ajout du même triplet
        boolean addedAgain = store.add(triple);
        assertFalse(addedAgain, "Le même triplet ne doit pas être ajouté deux fois.");


    }

    @Test
    public void testSize() {
        RDFHexaStore store = new RDFHexaStore();

        var factory = SameObjectTermFactory.instance();

        // Création de plusieurs triplets RDF
        RDFTriple t1 = new RDFTriple(
                factory.createOrGetLiteral("Bob"),
                factory.createOrGetLiteral("knows"),
                factory.createOrGetLiteral("Alice")
        );
        RDFTriple t2 = new RDFTriple(
                factory.createOrGetLiteral("Alice"),
                factory.createOrGetLiteral("likes"),
                factory.createOrGetLiteral("Music")
        );
        RDFTriple t3 = new RDFTriple(
                factory.createOrGetLiteral("Bob"),
                factory.createOrGetLiteral("likes"),
                factory.createOrGetLiteral("Music")
        );

        // Ajout des triplets
        store.add(t1);
        store.add(t2);
        store.add(t3);

        // Vérifie que la taille est correcte
        assertEquals(3, store.size(), "Le store doit contenir trois triplets.");

        // Ajout d’un doublon : ne doit pas changer la taille
        store.add(t1);
        assertEquals(3, store.size(), "L’ajout d’un doublon ne doit pas modifier la taille.");
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
        assertTrue(matchedList.contains(secondResult), "Missing substitution: " + firstResult);
        assertTrue(matchedList.contains(secondResult), "Missing substitution: " + secondResult);

        // Other cases
        throw new NotImplementedException("This test must be completed");
    }

    @Test
    public void testMatchStarQuery() {
        throw new NotImplementedException();
    }

    // Vos autres tests d'HexaStore ici
}
