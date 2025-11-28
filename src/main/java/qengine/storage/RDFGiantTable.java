package qengine.storage;

import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Variable;
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

    @Override
    public Iterator<Substitution> match(RDFTriple a) {
        List<Substitution> out = new ArrayList<>();
        boolean sVar = a.getTripleSubject()  instanceof Variable;
        boolean pVar = a.getTriplePredicate() instanceof Variable;
        boolean oVar = a.getTripleObject()   instanceof Variable;

        Integer sId = sVar ? null : dict.encode(a.getTripleSubject());
        Integer pId = pVar ? null : dict.encode(a.getTriplePredicate());
        Integer oId = oVar ? null : dict.encode(a.getTripleObject());

        for (int[] e : encodedTriples) {
            if (sId != null && e[0] != sId) continue;
            if (pId != null && e[1] != pId) continue;
            if (oId != null && e[2] != oId) continue;

            Substitution sigma = new fr.boreal.model.logicalElements.impl.SubstitutionImpl();
            if (sVar) sigma.add((Variable) a.getTripleSubject(),  dict.decode(e[0]));
            if (pVar) sigma.add((Variable) a.getTriplePredicate(),dict.decode(e[1]));
            if (oVar) sigma.add((Variable) a.getTripleObject(),   dict.decode(e[2]));
            out.add(sigma);
        }
        return out.iterator();
    }

    @Override
    public long howMany(RDFTriple a) {
        long c = 0;
        Integer sId = (a.getTripleSubject()  instanceof Variable) ? null : dict.encode(a.getTripleSubject());
        Integer pId = (a.getTriplePredicate() instanceof Variable) ? null : dict.encode(a.getTriplePredicate());
        Integer oId = (a.getTripleObject()   instanceof Variable) ? null : dict.encode(a.getTripleObject());
        for (int[] e : encodedTriples) {
            if (sId != null && e[0] != sId) continue;
            if (pId != null && e[1] != pId) continue;
            if (oId != null && e[2] != oId) continue;
            c++;
        }
        return c;
    }
}
