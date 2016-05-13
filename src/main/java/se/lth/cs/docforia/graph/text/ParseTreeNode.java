package se.lth.cs.docforia.graph.text;

import se.lth.cs.docforia.DocumentProxy;
import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.query.NodeTVar;

/**
 * Created by csz-mkg on 2016-05-09.
 */
public class ParseTreeNode extends Node<ParseTreeNode> {
    public static final String PROPERTY_LABEL = "label";

    public ParseTreeNode() {
    }

    public ParseTreeNode(DocumentProxy doc) {
        super(doc);
    }

    public ParseTreeNode parent() {
        return inboundNodes(ParseTreeEdge.class, ParseTreeNode.class).first();
    }

    public Iterable<ParseTreeNode> children() {
        return outboundNodes(ParseTreeEdge.class, ParseTreeNode.class);
    }

    public ParseTreeNode setLabel(String label) {
        putProperty(PROPERTY_LABEL, label);
        return this;
    }

    public String getLabel() {
        return getProperty(PROPERTY_LABEL);
    }

    public static NodeTVar<ParseTreeNode> var() {
        return new NodeTVar<ParseTreeNode>(ParseTreeNode.class);
    }

    public static NodeTVar<ParseTreeNode> var(String variant) {
        return new NodeTVar<ParseTreeNode>(ParseTreeNode.class, variant);
    }
}
