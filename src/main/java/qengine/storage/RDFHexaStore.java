package qengine.storage;

import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Variable;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.*;


public class RDFHexaStore implements RDFStorage {

    private final Dictionary dict = new Dictionary();
    private final List<int[]> encodedTriples = new ArrayList<>();
    private final List<RDFTriple> atoms = new ArrayList<>();

    private final Map<Integer, Map<Integer, Set<Integer>>> SPO = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<Integer>>> SOP = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<Integer>>> PSO = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<Integer>>> POS = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<Integer>>> OSP = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<Integer>>> OPS = new HashMap<>();

    private static void put(Map<Integer, Map<Integer, Set<Integer>>> idx,
                            int a, int b, int c) {
        idx.computeIfAbsent(a, k -> new HashMap<>())
                .computeIfAbsent(b, k -> new HashSet<>())
                .add(c);
    }

    private static Set<Integer> getSet(Map<Integer, Map<Integer, Set<Integer>>> idx,
                                       Integer a, Integer b) {
        if (a == null || b == null) return Collections.emptySet();
        Map<Integer, Set<Integer>> m = idx.get(a);
        if (m == null) return Collections.emptySet();
        Set<Integer> s = m.get(b);
        return s != null ? s : Collections.emptySet();
    }

    private static Map<Integer, Set<Integer>> getMap(Map<Integer, Map<Integer, Set<Integer>>> idx,
                                                     Integer a) {
        if (a == null) return Collections.emptyMap();
        Map<Integer, Set<Integer>> m = idx.get(a);
        return m != null ? m : Collections.emptyMap();
    }

    @Override
    public boolean add(RDFTriple triple) {
        int s = dict.encode(triple.getTripleSubject());
        int p = dict.encode(triple.getTriplePredicate());
        int o = dict.encode(triple.getTripleObject());

        Map<Integer, Set<Integer>> mp = SPO.get(s);
        if (mp != null) {
            Set<Integer> os = mp.get(p);
            if (os != null && os.contains(o)) {
                return false;
            }
        }

        encodedTriples.add(new int[]{s, p, o});
        atoms.add(triple);

        // Indexation HexaStore
        put(SPO, s, p, o);
        put(SOP, s, o, p);
        put(PSO, p, s, o);
        put(POS, p, o, s);
        put(OSP, o, s, p);
        put(OPS, o, p, s);

        return true;
    }

    @Override
    public long size() {
        return atoms.size();
    }

    @Override
    public Collection<RDFTriple> getAtoms() {
        return Collections.unmodifiableList(atoms);
    }

    @Override
    public Iterator<Substitution> match(RDFTriple a) {

        boolean sVar = a.getTripleSubject()  instanceof Variable;
        boolean pVar = a.getTriplePredicate() instanceof Variable;
        boolean oVar = a.getTripleObject()   instanceof Variable;

        Integer sId = sVar ? null : dict.encode(a.getTripleSubject());
        Integer pId = pVar ? null : dict.encode(a.getTriplePredicate());
        Integer oId = oVar ? null : dict.encode(a.getTripleObject());

        List<Substitution> out = new ArrayList<>();

        if (!sVar && !pVar && !oVar) {
            if (getSet(SPO, sId, pId).contains(oId)) {
                out.add(new SubstitutionImpl());
            }
            return out.iterator();
        }

        if (!sVar && !pVar && oVar) {
            for (int o : getSet(SPO, sId, pId)) {
                Substitution sigma = new SubstitutionImpl();
                sigma.add((Variable) a.getTripleObject(), dict.decode(o));
                out.add(sigma);
            }
            return out.iterator();
        }

        if (sVar && !pVar && !oVar) {
            for (int s : getSet(POS, pId, oId)) {
                Substitution sigma = new SubstitutionImpl();
                sigma.add((Variable) a.getTripleSubject(), dict.decode(s));
                out.add(sigma);
            }
            return out.iterator();
        }

        // ===== Cas (s,?p,o) =====
        if (!sVar && pVar && !oVar) {
            for (int p : getSet(SOP, sId, oId)) {
                Substitution sigma = new SubstitutionImpl();
                sigma.add((Variable) a.getTriplePredicate(), dict.decode(p));
                out.add(sigma);
            }
            return out.iterator();
        }

        if (!sVar && pVar && oVar) {
            for (var entry : getMap(SPO, sId).entrySet()) {
                int p = entry.getKey();
                for (int o : entry.getValue()) {
                    Substitution sigma = new SubstitutionImpl();
                    sigma.add((Variable) a.getTriplePredicate(), dict.decode(p));
                    sigma.add((Variable) a.getTripleObject(), dict.decode(o));
                    out.add(sigma);
                }
            }
            return out.iterator();
        }

        if (sVar && !pVar && oVar) {
            for (var entry : getMap(PSO, pId).entrySet()) {
                int s = entry.getKey();
                for (int o : entry.getValue()) {
                    Substitution sigma = new SubstitutionImpl();
                    sigma.add((Variable) a.getTripleSubject(), dict.decode(s));
                    sigma.add((Variable) a.getTripleObject(), dict.decode(o));
                    out.add(sigma);
                }
            }
            return out.iterator();
        }

        if (sVar && pVar && !oVar) {
            for (var entry : getMap(OSP, oId).entrySet()) {
                int s = entry.getKey();
                for (int p : entry.getValue()) {
                    Substitution sigma = new SubstitutionImpl();
                    sigma.add((Variable) a.getTripleSubject(), dict.decode(s));
                    sigma.add((Variable) a.getTriplePredicate(), dict.decode(p));
                    out.add(sigma);
                }
            }
            return out.iterator();
        }

        for (int[] enc : encodedTriples) {
            int s = enc[0], p = enc[1], o = enc[2];
            if (sId != null && s != sId) continue;
            if (pId != null && p != pId) continue;
            if (oId != null && o != oId) continue;

            Substitution sigma = new SubstitutionImpl();
            if (sVar) sigma.add((Variable) a.getTripleSubject(), dict.decode(s));
            if (pVar) sigma.add((Variable) a.getTriplePredicate(), dict.decode(p));
            if (oVar) sigma.add((Variable) a.getTripleObject(), dict.decode(o));
            out.add(sigma);
        }

        return out.iterator();
    }

    @Override
    public Iterator<Substitution> match(StarQuery q) {
        throw new UnsupportedOperationException("match(StarQuery) not implemented yet");
    }

    // test de push
    @Override
    public long howMany(RDFTriple a) {

        boolean sVar = a.getTripleSubject()  instanceof Variable;
        boolean pVar = a.getTriplePredicate() instanceof Variable;
        boolean oVar = a.getTripleObject()   instanceof Variable;

        Integer sId = sVar ? null : dict.encode(a.getTripleSubject());
        Integer pId = pVar ? null : dict.encode(a.getTriplePredicate());
        Integer oId = oVar ? null : dict.encode(a.getTripleObject());

        // 3 constantes
        if (!sVar && !pVar && !oVar)
            return getSet(SPO, sId, pId).contains(oId) ? 1 : 0;

        // 2 constantes
        if (!sVar && !pVar && oVar)
            return getSet(SPO, sId, pId).size();

        if (sVar && !pVar && !oVar)
            return getSet(POS, pId, oId).size();

        if (!sVar && pVar && !oVar)
            return getSet(SOP, sId, oId).size();

        // 1 constante
        long c = 0;
        if (!sVar && pVar && oVar)
            for (Set<Integer> set : getMap(SPO, sId).values()) c += set.size();

        if (sVar && !pVar && oVar)
            for (Set<Integer> set : getMap(PSO, pId).values()) c += set.size();

        if (sVar && pVar && !oVar)
            for (Set<Integer> set : getMap(OSP, oId).values()) c += set.size();




        if (sVar && pVar && oVar)
            return encodedTriples.size();

        return c;
    }
}
