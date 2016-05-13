package se.lth.cs.docforia.memstore;
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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.graph.ast.AstNode;
import se.lth.cs.docforia.graph.ast.AstTextNode;
import se.lth.cs.docforia.graph.disambig.EntityDisambiguation;
import se.lth.cs.docforia.graph.disambig.NamedEntityDisambiguation;
import se.lth.cs.docforia.graph.disambig.SenseDisambiguation;
import se.lth.cs.docforia.graph.hypertext.Anchor;
import se.lth.cs.docforia.graph.hypertext.ListItem;
import se.lth.cs.docforia.graph.hypertext.ListSection;
import se.lth.cs.docforia.graph.outline.Abstract;
import se.lth.cs.docforia.graph.outline.Heading;
import se.lth.cs.docforia.graph.outline.Section;
import se.lth.cs.docforia.graph.outline.TableOfContents;
import se.lth.cs.docforia.graph.text.*;

/**
 * Core Node Layers
 */
public enum MemoryCoreNodeLayer {
    UNKNOWN(-1, null, null),
    TOKEN(0, Token.class, Token::new),
    SENTENCE(1, Sentence.class, Sentence::new),
    PARAGRAPH(2, Paragraph.class, Paragraph::new),

    ANCHOR(3, Anchor.class, Anchor::new),

    SECTION(4, Section.class, Section::new),
    ABSTRACT(5, Abstract.class, Abstract::new),
    HEADING(6, Heading.class, Heading::new),
    CLAUSE(7, Clause.class, Clause::new),
    PHRASE(8, Phrase.class, Phrase::new),

    PREDICATE(9, Predicate.class, Predicate::new),
    ARGUMENT(10, Argument.class, Argument::new),
    PROPOSITION(11, Proposition.class, Proposition::new),

    ENTITY(12, Entity.class, Entity::new),
    NAMED_ENTITY(13, NamedEntity.class, NamedEntity::new),

    COREF_MENTION(14, CoreferenceMention.class, CoreferenceMention::new),
    COREF_CHAIN(15, CoreferenceChain.class, CoreferenceChain::new),

    MENTION(16, Mention.class, Mention::new),
    ENTITY_DISAMBIGUATION(17, EntityDisambiguation.class, EntityDisambiguation::new),
    NAMED_ENTITY_DISAMBIGUATION(18, NamedEntityDisambiguation.class, NamedEntityDisambiguation::new),
    SENSE_DISAMBIGUATION(19, SenseDisambiguation.class, SenseDisambiguation::new),
    COMPOUND(20, Compound.class, Compound::new),

    LIST_ITEM(21, ListItem.class, ListItem::new),
    LIST_SECTION(22,ListSection.class, ListSection::new),

    TABLE_OF_CONTENTS(23, TableOfContents.class, TableOfContents::new),
    AST_NODE(24, AstNode.class, AstNode::new),
    AST_TEXT_NODE(25, AstTextNode.class, AstTextNode::new),

    PARSE_TREE_NODE(26, ParseTreeNode.class, ParseTreeNode::new);

    public final int id;
    public final String layer;
    public final MemoryNodeFactory factory;

    private static final Object2ObjectOpenHashMap<String,MemoryCoreNodeLayer> name2layer;

    static {
        name2layer = new Object2ObjectOpenHashMap<>();
        name2layer.defaultReturnValue(MemoryCoreNodeLayer.UNKNOWN);
        for (MemoryCoreNodeLayer coreNodeLayer : MemoryCoreNodeLayer.values()) {
            name2layer.put(coreNodeLayer.layer, coreNodeLayer);
        }
    }

    MemoryCoreNodeLayer(int id, Class<? extends Node> layer, MemoryNodeFactory factory) {
        this.id = id;
        this.layer = layer == null ? null : layer.getName();
        this.factory = factory;
    }

    public static MemoryCoreNodeLayer fromLayerName(String name) {
        return name2layer.get(name);
    }

    public static MemoryCoreNodeLayer fromId(int id) {
        if(id >= values().length-1)
            return UNKNOWN;

        return values()[id+1];
    }
}
