package qengine.storage;

import fr.boreal.model.logicalElements.api.Term;
import java.util.HashMap;
import java.util.Map;

public final class Dictionary {
    private final Map<Term, Integer> termToId = new HashMap<>();
    private final Map<Integer, Term> idToTerm = new HashMap<>();
    private int nextId = 1;

    public int encode(Term term) {
        if (term == null) throw new IllegalArgumentException("term null");
        Integer id = termToId.get(term);
        if (id != null) return id;
        int newId = nextId++;
        termToId.put(term, newId);
        idToTerm.put(newId, term);
        return newId;
    }

    public Term decode(int id) { return idToTerm.get(id); }
    public int size() { return termToId.size(); }

    public void clear() {
        termToId.clear();
        idToTerm.clear();
        nextId = 1;
    }
}
