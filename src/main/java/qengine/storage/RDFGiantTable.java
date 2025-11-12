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
    public boolean add(RDFTriple triple) {
        int s = dict.encode(triple.getTripleSubject().toString());
        int p = dict.encode(triple.getTriplePredicate().toString());
        int o = dict.encode(triple.getTripleObject().toString());

        for (int[] t : encodedTriples)
            if (t[0]==s && t[1]==p && t[2]==o) return false;

        encodedTriples.add(new int[]{s,p,o});
        atoms.add(triple);
        return true;
    }

    @Override public long size() { return atoms.size(); }
    @Override public Collection<RDFTriple> getAtoms() { return Collections.unmodifiableList(atoms); }
    @Override public Iterator<Substitution> match(RDFTriple a){ throw new UnsupportedOperationException(); }
    @Override public Iterator<Substitution> match(StarQuery q){ throw new UnsupportedOperationException(); }
    @Override public long howMany(RDFTriple a){ throw new UnsupportedOperationException(); }
}

