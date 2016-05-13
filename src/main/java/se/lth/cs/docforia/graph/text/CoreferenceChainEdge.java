package se.lth.cs.docforia.graph.text;

import se.lth.cs.docforia.DocumentProxy;
import se.lth.cs.docforia.Edge;
import se.lth.cs.docforia.query.EdgeTVar;

/**
 * Coreference chain edge
 */
public class CoreferenceChainEdge extends Edge<CoreferenceChainEdge> {
    public CoreferenceChainEdge(DocumentProxy doc) {
        super(doc);
    }

    public CoreferenceChainEdge() {
    }

    public static EdgeTVar<CoreferenceChainEdge> var() {
        return new EdgeTVar<CoreferenceChainEdge>(CoreferenceChainEdge.class);
    }

    public static EdgeTVar<CoreferenceChainEdge> var(String variant) {
        return new EdgeTVar<CoreferenceChainEdge>(CoreferenceChainEdge.class, variant);
    }
}
