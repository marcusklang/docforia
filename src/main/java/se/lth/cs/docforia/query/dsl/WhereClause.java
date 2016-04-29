package se.lth.cs.docforia.query.dsl;
/*
 * Copyright 2016 Marcus Klang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import se.lth.cs.docforia.*;
import se.lth.cs.docforia.exceptions.QueryException;
import se.lth.cs.docforia.query.*;
import se.lth.cs.docforia.query.predicates.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;

/**
 * Where clause
 */
public class WhereClause extends CommonClause {
    protected final QueryClause parent;
    protected final Var[] vars;

    public WhereClause(QueryClause parent, Var[] vars) {
        this.parent = parent;
        this.vars = vars;
    }

    @Override
    protected QueryClause root() {
        return parent;
    }

    public PropertyClause property(String propertyKey) {
        return new PropertyClause(this, propertyKey);
    }

    public WhereClause predicate(Predicate predicate) {
        parent.predicates.add(predicate);
        return this;
    }

    /**
     * Eval proposition.
     */
    public WhereClause predicate(final Function<Proposition,Boolean> pred) {
        parent.predicates.add(new Predicate(parent.doc, vars) {
            @Override
            public boolean eval(Proposition proposition) {
                return pred.apply(proposition);
            }
        });
        return this;
    }

    public WhereClause coveredBy(Range range) {
        return this.coveredBy(range.getStart(), range.getEnd());
    }

    public WhereClause coveredBy(int from, int to) {
        for(Var children : vars) {
            if(!(children instanceof NodeVar))
                throw new QueryException("var in where clause is not a NodeVar: " + children.toString());

            parent.predicates.add(new CoveredByConstantPredicate(parent.doc, (NodeVar)children, from, to));
        }

        return this;
    }

    public WhereClause coveredBy(NodeVar parentNode) {
        parent.select(parentNode);

        for(Var childNode : vars) {
            if(!(childNode instanceof NodeVar))
                throw new QueryException("var in where clause is not a NodeVar: " + childNode.toString());

            parent.predicates.add(new CoveredByPredicate(parent.doc, (NodeVar)childNode, parentNode));
        }

        return this;
    }

    public WhereClause covering(int from, int to) {
        for(Var children : vars) {
            if(!(children instanceof NodeVar))
                throw new QueryException("var in where clause is not a NodeVar: " + children.toString());

            parent.predicates.add(new CoveringConstantPredicate(parent.doc, (NodeVar)children, from, to));
        }

        return this;
    }

    public WhereClause covering(Range range) {
        return this.covering(range.getStart(), range.getEnd());
    }

    public WhereClause covering(NodeVar childNode) {
        parent.select(childNode);

        for(Var parentNode : vars) {
            parent.predicates.add(new CoveredByPredicate(parent.doc, childNode, (NodeVar)parentNode));
        }

        return this;
    }

    public WhereClause intersects(int from, int to) {
        for(Var children : vars) {
            if(!(children instanceof NodeVar))
                throw new QueryException("var in where clause is not a NodeVar: " + children.toString());

            parent.predicates.add(new IntersectConstRangePredicate(parent.doc, (NodeVar)children, from, to));
        }

        return this;
    }

    public WhereClause intersects(Range range) {
        return intersects(range.getStart(), range.getEnd());
    }

    /**
     * Window query (inflexible, window must exist)
     * @param node  the range to window around
     * @param symmetricSize the symmetric size of the window
     */
    public WhereClause inWindowOf(Range node, int symmetricSize) {
        return inWindowOf(node, symmetricSize, symmetricSize);
    }

    /**
     * Window query (inflexible, window must exist)
     * @param node  the range to window around
     * @param pre the size of the window on the left
     * @param post the size of the window on the right
     */
    public WhereClause inWindowOf(Range node, int pre, int post) {
        return inWindowOf(node, pre, post, false);
    }

    /**
     * Window query
     * @param node  the range to window around
     * @param symmetricSize the symmetric size of the window
     * @param flexible if the window must exist or if it can be truncated from either side
     */
    public WhereClause inWindowOf(Range node, int symmetricSize, boolean flexible) {
        return inWindowOf(node, symmetricSize, symmetricSize, flexible);
    }

    /**
     * Window query
     * @param node  the range to window around
     * @param pre the size of the window on the left (#elems)
     * @param post the size of the window on the right (#elems)
     * @param flexible if the window must exist or if it can be truncated from either side
     */
    public WhereClause inWindowOf(Range node, int pre, int post, boolean flexible) {
        if(pre == 0 && post == 0)
            return coveredBy(node);

        for(Var childNode : vars) {
            if(!(childNode instanceof NodeVar))
                throw new QueryException("var in where clause is not a NodeVar: " + childNode.toString());

            NodeInWindowPredicate pred = new NodeInWindowPredicate(parent.doc, (NodeVar) childNode, childNode.getLayer(), childNode.getVariant(), node, pre, post, flexible);
            if(pred.getStart() != -1 && pred.getEnd() != -1 && parent.predicates.size() > 0)
                coveredBy(pred.getStart(), pred.getEnd());

            parent.predicates.add(pred);
        }

        return this;
    }

    /**
     * Only look at these tokens
     * @param nodes the nodes to look for
     */
    public WhereClause isOneOf(Node...nodes) {
        for (Var childNode : vars) {
            if(!(childNode instanceof NodeVar))
                throw new QueryException("var in where clause is not a NodeVar: " + childNode.toString());

            HashSet<NodeRef> nodeRefs = new HashSet<>();
            for (Node node : nodes) {
                nodeRefs.add(node.getRef());
            }

            ConstantNodeCandidates candidates = new ConstantNodeCandidates(parent.doc, (NodeVar)childNode, nodeRefs);
            parent.predicates.add(candidates);
        }
        return this;
    }

    public WhereClause isOneOf(Edge...edges) {
        for (Var childEdge : vars) {
            if(!(childEdge instanceof EdgeVar))
                throw new QueryException("var in where clause is not a EdgeVar: " + childEdge.toString());

            HashSet<EdgeRef> edgeRefs = new HashSet<>();
            for (Edge edge : edges) {
                edgeRefs.add(edge.getRef());
            }

            ConstantEdgeCandidates candidates = new ConstantEdgeCandidates(parent.doc, (EdgeVar)childEdge, edgeRefs);
            parent.predicates.add(candidates);
        }

        return this;
    }

    public WhereClause isOneOfNodes(Collection<? extends Node> nodes) {
        for (Var childNode : vars) {
            if(!(childNode instanceof NodeVar))
                throw new QueryException("var in where clause is not a NodeVar: " + childNode.toString());

            HashSet<NodeRef> nodeRefs = new HashSet<>();
            for (Node node : nodes) {
                nodeRefs.add(node.getRef());
            }

            ConstantNodeCandidates candidates = new ConstantNodeCandidates(parent.doc, (NodeVar)childNode, nodeRefs);
            parent.predicates.add(candidates);
        }
        return this;
    }

    public WhereClause isOneOfEdges(Collection<? extends Edge> edges) {
        for (Var childEdge : vars) {
            if(!(childEdge instanceof EdgeVar))
                throw new QueryException("var in where clause is not a EdgeVar: " + childEdge.toString());

            HashSet<EdgeRef> edgeRefs = new HashSet<>();
            for (Edge edge : edges) {
                edgeRefs.add(edge.getRef());
            }

            ConstantEdgeCandidates candidates = new ConstantEdgeCandidates(parent.doc, (EdgeVar)childEdge, edgeRefs);
            parent.predicates.add(candidates);
        }

        return this;
    }

    public EdgeClause hasEdge(EdgeVar edgeVar) {
        return new EdgeClause(this, edgeVar, true);
    }

    public EdgeClause hasNotEdge(EdgeVar edgeVar) {
        return new EdgeClause(this, edgeVar, false);
    }
}
