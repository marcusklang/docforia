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
import se.lth.cs.docforia.query.NodeTVar;

public class AstTextNode extends AstNode {
    /** Literal property key */
    public static final String PROPERTY_LITERAL = "literal";

	public AstTextNode(DocumentProxy doc) {
		super(doc);
	}

	public AstTextNode() {
		super();
	}
	
	
	@Override
	public String text() {
		String literal = this.getProperty(PROPERTY_LITERAL);
		if(literal != null)
			return literal;
		else
			return super.text();
	}

    public AstTextNode setLiteral(String literal) {
        putProperty(PROPERTY_LITERAL, literal);
        return this;
    }

	public boolean hasLiteral() {
		return this.hasProperty(PROPERTY_LITERAL);
	}

	public static NodeTVar<AstTextNode> textVar() {
		return new NodeTVar<AstTextNode>(AstTextNode.class);
	}

	public static NodeTVar<AstTextNode> textVar(String variant) {
		return new NodeTVar<AstTextNode>(AstTextNode.class, variant);
	}
}
