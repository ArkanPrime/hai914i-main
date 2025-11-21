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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    void testMatchStarQueryUnsupported() {
        RDFGiantTable store = new RDFGiantTable();
        // On passe null car l'implémentation actuelle jette l'exception indépendamment du contenu.
        assertThrows(UnsupportedOperationException.class, () -> store.match((StarQuery) null),
                "match(StarQuery) doit lancer UnsupportedOperationException pour cette implémentation");
    }
}
