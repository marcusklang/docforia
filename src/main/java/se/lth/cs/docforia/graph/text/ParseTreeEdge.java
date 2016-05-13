package se.lth.cs.docforia.graph.text;

import se.lth.cs.docforia.DocumentProxy;
import se.lth.cs.docforia.Edge;
import se.lth.cs.docforia.query.EdgeTVar;

/**
 * Created by csz-mkg on 2016-05-09.
 */
public class ParseTreeEdge extends Edge<ParseTreeEdge> {
    public ParseTreeEdge() {
    }

    public ParseTreeEdge(DocumentProxy doc) {
        super(doc);
    }


    public static EdgeTVar<ParseTreeEdge> var() {
        return new EdgeTVar<ParseTreeEdge>(ParseTreeEdge.class);
    }

    public static EdgeTVar<ParseTreeEdge> var(String variant) {
        return new EdgeTVar<ParseTreeEdge>(ParseTreeEdge.class, variant);
    }
}
