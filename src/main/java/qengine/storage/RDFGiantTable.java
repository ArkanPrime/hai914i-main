package qengine.storage;

import fr.boreal.model.logicalElements.api.Substitution;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.*;

public class RDFGiantTable implements RDFStorage {
    private final Dictionary dict = new Dictionary();
    private final List<int[]> encodedTriples = new ArrayList<>();
    private final List<RDFTriple> atoms = new ArrayList<>();

    @Override
    public boolean add(RDFTriple t) {
        int s = dict.encode(t.getTripleSubject());
        int p = dict.encode(t.getTriplePredicate());
        int o = dict.encode(t.getTripleObject());
        for (int[] e : encodedTriples)
            if (e[0]==s && e[1]==p && e[2]==o) return false; // évite doublons
        encodedTriples.add(new int[]{s,p,o});
        atoms.add(t);
        return true;
    }

    @Override public long size() { return atoms.size(); }
    @Override public Collection<RDFTriple> getAtoms() { return Collections.unmodifiableList(atoms); }

    @Override public Iterator<Substitution> match(RDFTriple a) { throw new UnsupportedOperationException(); }
    @Override public Iterator<Substitution> match(StarQuery q) { throw new UnsupportedOperationException(); }
    @Override public long howMany(RDFTriple a) { throw new UnsupportedOperationException(); }
}
