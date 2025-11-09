package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.*;

/**
 * Implémentation d'un HexaStore pour stocker des RDFAtom.
 * Cette classe utilise six index pour optimiser les recherches.
 * Les index sont basés sur les combinaisons (Sujet, Prédicat, Objet), (Sujet, Objet, Prédicat),
 * (Prédicat, Sujet, Objet), (Prédicat, Objet, Sujet), (Objet, Sujet, Prédicat) et (Objet, Prédicat, Sujet).
 */
public class RDFHexaStore implements RDFStorage {

    private final Map<String, Integer> termToId = new HashMap<>();
    private final Map<Integer, String> idToTerm = new HashMap<>();
    private int nextId = 1;

    private final List<int[]> encodedTriples = new ArrayList<>();
    private final List<RDFTriple> atoms = new ArrayList<>();


    protected int encode(String term) {
        return termToId.computeIfAbsent(term, t -> {
            idToTerm.put(nextId, t);
            return nextId++;
        });
    }


    protected String decode(int id) {
        return idToTerm.get(id);
    }

    @Override
    public boolean add(RDFTriple triple) {
        int s = encode(triple.getTripleSubject().toString());
        int p = encode(triple.getTriplePredicate().toString());
        int o = encode(triple.getTripleObject().toString());

        int[] encoded = new int[]{s, p, o};

        // éviter les doublons, car pourraient nous gené/rendre faux plus tard
        for (int[] t : encodedTriples) {
            if (t[0] == s && t[1] == p && t[2] == o) {
                return false;
            }
        }

        encodedTriples.add(encoded);
        atoms.add(triple);
        return true;
    }




    @Override
    public long size() {
        return atoms.size();
    }



    @Override
    public Iterator<Substitution> match(RDFTriple triple) {
        throw new NotImplementedException();
    }

    @Override
    public Iterator<Substitution> match(StarQuery q) {
        throw new NotImplementedException();
    }

    @Override
    public long howMany(RDFTriple triple) {
        throw new NotImplementedException();
    }

    @Override
    public Collection<RDFTriple> getAtoms() {
        return Collections.unmodifiableList(atoms);
    }
}
