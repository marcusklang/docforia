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
    UNKNOWN(-1, null, null, null),
    TOKEN(0, "!Token", Token.class, Token::new),
    SENTENCE(1, "!Sentence", Sentence.class, Sentence::new),
    PARAGRAPH(2, "!Paragraph", Paragraph.class, Paragraph::new),

    ANCHOR(3, "!Anchor", Anchor.class, Anchor::new),

    SECTION(4, "!Section", Section.class, Section::new),
    ABSTRACT(5, "!Abstract", Abstract.class, Abstract::new),
    HEADING(6, "!Heading", Heading.class, Heading::new),
    CLAUSE(7, "!Clause", Clause.class, Clause::new),
    PHRASE(8, "!Phrase", Phrase.class, Phrase::new),

    PREDICATE(9, "!Predicate", Predicate.class, Predicate::new),
    ARGUMENT(10, "!Argument", Argument.class, Argument::new),
    PROPOSITION(11, "!Proposition", Proposition.class, Proposition::new),

    ENTITY(12, "!Entity", Entity.class, Entity::new),
    NAMED_ENTITY(13, "!NamedEntity", NamedEntity.class, NamedEntity::new),

    COREF_MENTION(14, "!CorefMention", CoreferenceMention.class, CoreferenceMention::new),
    COREF_CHAIN(15, "!CorefChain", CoreferenceChain.class, CoreferenceChain::new),

    MENTION(16, "!Mention", Mention.class, Mention::new),
    ENTITY_DISAMBIGUATION(17, "!EntityDisambig", EntityDisambiguation.class, EntityDisambiguation::new),
    NAMED_ENTITY_DISAMBIGUATION(18, "!NamedEntityDisambig", NamedEntityDisambiguation.class, NamedEntityDisambiguation::new),
    SENSE_DISAMBIGUATION(19, "!SenseDisambig", SenseDisambiguation.class, SenseDisambiguation::new),
    COMPOUND(20, "!Compound", Compound.class, Compound::new),

    LIST_ITEM(21, "!ListItem", ListItem.class, ListItem::new),
    LIST_SECTION(22,"!ListSection", ListSection.class, ListSection::new),

    TABLE_OF_CONTENTS(23, "!TOC", TableOfContents.class, TableOfContents::new),
    AST_NODE(24, "!Ast", AstNode.class, AstNode::new),
    AST_TEXT_NODE(25, "!AstText", AstTextNode.class, AstTextNode::new),

    PARSE_TREE_NODE(26, "!ParseTreeNode", ParseTreeNode.class, ParseTreeNode::new);

    public final int id;
    public final String layer;
    public final String jsonLayer;
    public final MemoryNodeFactory factory;

    private static final Object2ObjectOpenHashMap<String,MemoryCoreNodeLayer> name2layer;
    private static final Object2ObjectOpenHashMap<String,MemoryCoreNodeLayer> jsonname2layer;

    static {
        name2layer = new Object2ObjectOpenHashMap<>();
        name2layer.defaultReturnValue(MemoryCoreNodeLayer.UNKNOWN);

        jsonname2layer = new Object2ObjectOpenHashMap<>();
        jsonname2layer.defaultReturnValue(MemoryCoreNodeLayer.UNKNOWN);

        for (MemoryCoreNodeLayer coreNodeLayer : MemoryCoreNodeLayer.values()) {
            name2layer.put(coreNodeLayer.layer, coreNodeLayer);
            jsonname2layer.put(coreNodeLayer.jsonLayer, coreNodeLayer);
        }
    }

    MemoryCoreNodeLayer(int id, String jsonLayer, Class<? extends Node> layer, MemoryNodeFactory factory) {
        this.id = id;
        this.jsonLayer = jsonLayer;
        this.layer = layer == null ? null : layer.getName();
        this.factory = factory;
    }

    public static MemoryCoreNodeLayer fromJsonLayerName(String name)
    {
        return jsonname2layer.get(name);
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
