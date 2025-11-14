package qengine.storage;

import fr.boreal.model.logicalElements.factory.api.TermFactory;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import org.junit.jupiter.api.Test;
import qengine.model.RDFTriple;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RDFGiantTableTest {

    private static final TermFactory F = SameObjectTermFactory.instance();

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
}