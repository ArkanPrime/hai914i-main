package qengine.storage;

import java.util.HashMap;
import java.util.Map;

public final class Dictionary {
    private final Map<String,Integer> termToId = new HashMap<>();
    private final Map<Integer,String> idToTerm = new HashMap<>();
    private int nextId = 1;

    public int encode(String term) {
        Integer id = termToId.get(term);
        if (id != null) return id;
        int newId = nextId++;
        termToId.put(term, newId);
        idToTerm.put(newId, term);
        return newId;
    }

    public String decode(int id) { return idToTerm.get(id); }
    public int size() { return termToId.size(); }
}
