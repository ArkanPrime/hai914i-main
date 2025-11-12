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
    @Override
    public boolean add(RDFTriple triple) {
        throw new NotImplementedException();
    }

    @Override
    public long size() {
        throw new NotImplementedException();
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
        throw new NotImplementedException();
    }
}
