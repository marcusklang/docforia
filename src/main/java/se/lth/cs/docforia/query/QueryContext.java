package se.lth.cs.docforia.query;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import se.lth.cs.docforia.Document;

import java.util.Set;

/**
 * Query Context information
 */
public class QueryContext {
    public final Document doc;
    public final Reference2IntOpenHashMap<Var> var2index;

    public QueryContext(Document doc) {
        this.doc = doc;
        this.var2index = new Reference2IntOpenHashMap<>();
    }

    public Document getDoc() {
        return doc;
    }

    public void addVar(Var var) {
        if(!var2index.containsKey(var)) {
            var2index.put(var, var2index.size());
        }
    }

    public int indexOf(Var var) {
        return var2index.getInt(var);
    }

    public Set<Var> vars() {
        return var2index.keySet();
    }

    public int numVars() {
        return var2index.size();
    }
}
