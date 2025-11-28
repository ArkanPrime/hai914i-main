package qengine.storage;

import java.util.*;
import java.util.stream.Stream;

import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.api.Variable;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

/**
 * Contrat pour un système de stockage de données RDF
 */
public interface RDFStorage {

    /**
     * Ajoute un RDFAtom dans le store.
     *
     * @param t le triplet à ajouter
     * @return true si le RDFAtom a été ajouté avec succès, false s'il est déjà présent
     */
    boolean add(RDFTriple t);

    /**
     * @param a atom
     * @return un itérateur de substitutions correspondant aux match des atomes
     *          (i.e., sur quels termes s'envoient les variables)
     */
    Iterator<Substitution> match(RDFTriple a);


    /**
     * @param q star query
     * @return an itérateur de subsitutions décrivrant les réponses à la requete
     */
    default Iterator<Substitution> match(StarQuery q) {

        // Substitutions en cours (au départ : une substitution vide)
        List<Substitution> current = new ArrayList<>();
        current.add(new SubstitutionImpl());

        // Pour chaque triple de la star query
        for (RDFTriple atom : q.getRdfAtoms()) {

            Iterator<Substitution> it = match(atom); // utilise match(RDFTriple) du store
            List<Substitution> next = new ArrayList<>();

            while (it.hasNext()) {
                Substitution sigma = it.next();
                Map<Variable, Term> mapSigma = sigma.toMap();

                for (Substitution tau : current) {
                    // on part de tau, qu'on copie dans une Map mutable
                    Map<Variable, Term> mapTau = new HashMap<>(tau.toMap());

                    boolean ok = true;

                    // fusion mapTau ⨝ mapSigma
                    for (Map.Entry<Variable, Term> e : mapSigma.entrySet()) {
                        Variable var = e.getKey();
                        Term tb = e.getValue();
                        Term ta = mapTau.get(var);

                        if (ta != null && !ta.equals(tb)) {
                            // même variable, deux valeurs différentes → conflit
                            ok = false;
                            break;
                        }
                        if (ta == null) {
                            mapTau.put(var, tb);
                        }
                    }

                    if (ok) {
                        // reconstruire une Substitution à partir de mapTau
                        Substitution merged = new SubstitutionImpl();
                        for (Map.Entry<Variable, Term> e : mapTau.entrySet()) {
                            merged.add(e.getKey(), e.getValue());
                        }
                        next.add(merged);
                    }
                }
            }

            current = next;
            if (current.isEmpty()) break; // plus aucune solution possible
        }

        return current.iterator();
    }


    /**
     * @param a atom
     * @return
     */
    long howMany(RDFTriple a);


    /**
     * Retourne le nombre d'atomes dans le Store.
     *
     * @return le nombre d'atomes
     */
    long size();

    /**
     * Retourne une collections contenant tous les atomes du store.
     * Utile pour les tests unitaires.
     *
     * @return une collection d'atomes
     */
    Collection<RDFTriple> getAtoms();

    /**
     * Ajoute des RDFAtom dans le store.
     *
     * @param atoms les RDFAtom à ajouter
     * @return true si au moins un RDFAtom a été ajouté, false s'ils sont tous déjà présents
     */
    default boolean addAll(Stream<RDFTriple> atoms) {
        return atoms.map(this::add).reduce(Boolean::logicalOr).orElse(false);
    }

    /**
     * Ajoute des RDFAtom dans le store.
     *
     * @param atoms les RDFAtom à ajouter
     * @return true si au moins un RDFAtom a été ajouté, false s'ils sont tous déjà présents
     */
    default boolean addAll(Collection<RDFTriple> atoms) {
        return this.addAll(atoms.stream());
    }
}
