package qengine.storage;

import fr.boreal.model.logicalElements.api.Literal;
import fr.boreal.model.logicalElements.api.Variable;
import fr.boreal.model.logicalElements.factory.api.TermFactory;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DictionaryTest {

    // ✅ déclare en type interface (ou utilise "var")
    private static final TermFactory F = SameObjectTermFactory.instance();

    private static final Literal<String> BOB   = F.createOrGetLiteral("Bob");
    private static final Literal<String> ALICE = F.createOrGetLiteral("Alice");
    private static final Literal<String> KNOWS = F.createOrGetLiteral("knows");
    private static final Variable X = F.createOrGetVariable("?x");

    @Test
    void encode() {
        Dictionary d = new Dictionary();
        int idBob1 = d.encode(BOB);
        int idBob2 = d.encode(BOB);
        int idAlice = d.encode(ALICE);
        assertEquals(idBob1, idBob2);
        assertNotEquals(idBob1, idAlice);
        assertEquals(2, d.size());
    }

    @Test
    void decode() {
        Dictionary d = new Dictionary();
        int idBob = d.encode(BOB);
        int idKnows = d.encode(KNOWS);
        int idVar = d.encode(X);
        assertEquals(BOB, d.decode(idBob));
        assertEquals(KNOWS, d.decode(idKnows));
        assertEquals(X, d.decode(idVar));
        assertNull(d.decode(999));
    }

    @Test
    void size() {
        Dictionary d = new Dictionary();
        assertEquals(0, d.size());
        d.encode(BOB);
        assertEquals(1, d.size());
        d.encode(BOB);
        assertEquals(1, d.size());
        d.encode(ALICE);
        assertEquals(2, d.size());
    }
}
