package se.lth.cs.docforia.graph.ast;
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

import se.lth.cs.docforia.DocumentProxy;
import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.query.NodeTVar;

import java.util.Iterator;

public class AstNode extends Node<AstNode> {
    /** Syntax type property key */
    public static final String PROPERTY_NAME = "type";

	public AstNode() {
		super();
	}

	public AstNode(DocumentProxy doc) {
		super(doc);
	}

	public <T extends Node> T get(String path) {
		for(AstEdge relation : outboundEdges(AstEdge.class)) {
			if(relation.getType().equals(path))
				return relation.getHead();
		}
		
		return null;
	}
	
	public AstEdge addNode(AstNode node) {
		return doc.add(new AstEdge(), this, node);
	}
	
	public AstEdge addText(int start, int end) {
		return doc.add(new AstEdge(), this, doc.add(new AstTextNode()).setRange(start, end)).setType("text");
	}
	
	public AstEdge addLiteralText(int start, int end, String literal) {
		return doc.add(new AstEdge(), this, new AstTextNode(doc).setLiteral(literal).setRange(start, end)).setType("text");
	}
	
	public AstEdge addNode(AstNode node, String relation) {
		return doc.add(new AstEdge().setType(relation), this, node);
	}
	
	public Iterable<String> relations() {
		return new Iterable<String>() {
			final Iterable<AstEdge> relations = outboundEdges(AstEdge.class);
			
			@Override
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					final Iterator<AstEdge> iter = relations.iterator();
					
					
					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
					
					@Override
					public String next() {
						return iter.next().getType();
					}
					
					@Override
					public boolean hasNext() {
						return iter.hasNext();
					}
				};
			}
		};
	}

	public AstNode setName(String name) {
		putProperty(PROPERTY_NAME, name);
		return this;
	}

	public String getName() {
		return getProperty(PROPERTY_NAME);
	}

	public static NodeTVar<AstNode> nodeVar() {
		return new NodeTVar<AstNode>(AstNode.class);
	}

	public static NodeTVar<AstNode> nodeVar(String variant) {
		return new NodeTVar<AstNode>(AstNode.class, variant);
	}
}
