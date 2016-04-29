package se.lth.cs.docforia;
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

import se.lth.cs.docforia.graph.hypertext.Anchor;
import se.lth.cs.docforia.graph.text.*;

/**
 * Test case file
 * Source text: http://sv.wikipedia.org/wiki/Conny_Andersson
 * Licence: Creative Commons Attribution-Share Alike 3.0 Unported.
 */
public class Conny_Andersson {

	public Document createDocument(DocumentFactory factory) {
		String text = "Conny Andersson är namnet på flera personer:\n\n Con"
		 + "ny Andersson (skådespelare) Conny Andersson (racer"
		 + "förare) Conny Andersson (politiker)\n\n";

		Document doc = factory.createFragment("wikipedia.sv:Conny_Andersson", text);

		doc.putProperty("title", "Conny Andersson");
		doc.putProperty("wikidata-alias", "[]");

		String temp = "'''Conny Andersson''' är namnet på flera personer:"
		 + "\n* [[Conny Andersson (skådespelare)]]\n* [[Conny An"
		 + "dersson (racerförare)]]\n* [[Conny Andersson (polit"
		 + "iker)]]\n\n{{gren}}";

		doc.putProperty("markup", temp);
		doc.putProperty("wikidata-representation-of", "[4167410]");
		doc.putProperty("wikidata-id", "Q10458291");
		doc.putProperty("revision", "1285596444000");
		doc.putProperty("type", "DISAMBIGUATION");
		doc.putProperty("wikidata-label", "Conny Andersson");

		doc.add(new Anchor()).setRange(47, 77).putProperty("resolved", "wikidata:Q5162071")
		                                      .putProperty("target", "Conny Andersson (skådespelare)");

		doc.add(new Anchor()).setRange(78, 107).putProperty("resolved", "wikidata:Q172242")
		                                       .putProperty("target", "Conny Andersson (racerförare)");

		doc.add(new Anchor()).setRange(108, 135).putProperty("target", "Conny Andersson (politiker)");

		doc.add(new NamedEntity()).setRange(0, 15).setLabel("person");

		doc.add(new NamedEntity()).setRange(47, 62).setLabel("person");

		doc.add(new NamedEntity()).setRange(78, 93).setLabel("person");

		doc.add(new NamedEntity()).setRange(108, 123).setLabel("person");

		doc.add(new Paragraph()).setRange(0, 44);

		doc.add(new Sentence()).setRange(0, 44);

		doc.add(new Sentence()).setRange(47, 135);

		Token node10 = doc.add(new Token()).setRange(0, 5).putProperty("head", "3")
		                                   .putProperty("stem", "conny")
		                                   .putProperty("lemma", "Conny")
		                                   .putProperty("deprel", "SS")
		                                   .putProperty("norm", "conny")
		                                   .putProperty("ppos", "PM")
		                                   .putProperty("id", "1")
		                                   .putProperty("feats", "NOM");

		Token node11 = doc.add(new Token()).setRange(6, 15).putProperty("head", "1")
		                                    .putProperty("stem", "andersson")
		                                    .putProperty("lemma", "Andersson")
		                                    .putProperty("deprel", "HD")
		                                    .putProperty("norm", "andersson")
		                                    .putProperty("ppos", "PM")
		                                    .putProperty("id", "2")
		                                    .putProperty("feats", "NOM");

		Token node12 = doc.add(new Token()).setRange(16, 18).putProperty("head", "0")
		                                     .putProperty("stem", "är")
		                                     .putProperty("stopword", "true")
		                                     .putProperty("lemma", "vara")
		                                     .putProperty("deprel", "ROOT")
		                                     .putProperty("norm", "vara")
		                                     .putProperty("ppos", "VB")
		                                     .putProperty("id", "3")
		                                     .putProperty("feats", "PRS|AKT");

		Token node13 = doc.add(new Token()).setRange(19, 25).putProperty("head", "3")
		                                     .putProperty("stem", "namnet")
		                                     .putProperty("lemma", "namn")
		                                     .putProperty("deprel", "OO")
		                                     .putProperty("norm", "namn")
		                                     .putProperty("ppos", "NN")
		                                     .putProperty("id", "4")
		                                     .putProperty("feats", "NEU|SIN|DEF|NOM");

		Token node14 = doc.add(new Token()).setRange(26, 28).putProperty("head", "3")
		                                     .putProperty("stem", "på")
		                                     .putProperty("stopword", "true")
		                                     .putProperty("lemma", "på")
		                                     .putProperty("deprel", "RA")
		                                     .putProperty("ppos", "PP")
		                                     .putProperty("id", "5")
		                                     .putProperty("norm", "på");

		Token node15 = doc.add(new Token()).setRange(29, 34).putProperty("head", "7")
		                                     .putProperty("stem", "fler")
		                                     .putProperty("lemma", "flera")
		                                     .putProperty("deprel", "DT")
		                                     .putProperty("norm", "flera")
		                                     .putProperty("ppos", "JJ")
		                                     .putProperty("id", "6")
		                                     .putProperty("feats", "POS|UTR/NEU|PLU|IND|NOM");

		Token node16 = doc.add(new Token()).setRange(35, 43).putProperty("head", "5")
		                                     .putProperty("stem", "person")
		                                     .putProperty("lemma", "person")
		                                     .putProperty("deprel", "PA")
		                                     .putProperty("norm", "person")
		                                     .putProperty("ppos", "NN")
		                                     .putProperty("id", "7")
		                                     .putProperty("feats", "UTR|PLU|IND|NOM");

		Token node17 = doc.add(new Token()).setRange(43, 44).putProperty("lemma", ":")
		                                     .putProperty("head", "3")
		                                     .putProperty("ppos", "MAD")
		                                     .putProperty("id", "8")
		                                     .putProperty("deprel", "IP");

		Token node18 = doc.add(new Token()).setRange(47, 52).putProperty("head", "0")
		                                     .putProperty("stem", "conny")
		                                     .putProperty("lemma", "Conny")
		                                     .putProperty("deprel", "ROOT")
		                                     .putProperty("norm", "conny")
		                                     .putProperty("ppos", "PM")
		                                     .putProperty("id", "1")
		                                     .putProperty("feats", "NOM");

		Token node19 = doc.add(new Token()).setRange(53, 62).putProperty("head", "1")
		                                     .putProperty("stem", "andersson")
		                                     .putProperty("lemma", "Andersson")
		                                     .putProperty("deprel", "HD")
		                                     .putProperty("norm", "andersson")
		                                     .putProperty("ppos", "PM")
		                                     .putProperty("id", "2")
		                                     .putProperty("feats", "NOM");

		Token node20 = doc.add(new Token()).setRange(63, 64).putProperty("lemma", "(")
		                                     .putProperty("head", "1")
		                                     .putProperty("ppos", "PAD")
		                                     .putProperty("id", "3")
		                                     .putProperty("deprel", "IR");

		Token node21 = doc.add(new Token()).setRange(64, 76).putProperty("head", "1")
		                                     .putProperty("stem", "skådespel")
		                                     .putProperty("lemma", "skådespelare")
		                                     .putProperty("deprel", "AN")
		                                     .putProperty("norm", "skådespelare")
		                                     .putProperty("ppos", "NN")
		                                     .putProperty("id", "4")
		                                     .putProperty("feats", "UTR|PLU|IND|NOM");

		Token node22 = doc.add(new Token()).setRange(76, 77).putProperty("lemma", ")")
		                                     .putProperty("head", "4")
		                                     .putProperty("ppos", "PAD")
		                                     .putProperty("id", "5")
		                                     .putProperty("deprel", "IR");

		Token node23 = doc.add(new Token()).setRange(78, 83).putProperty("head", "4")
		                                     .putProperty("stem", "conny")
		                                     .putProperty("lemma", "Conny")
		                                     .putProperty("deprel", "ET")
		                                     .putProperty("norm", "conny")
		                                     .putProperty("ppos", "PM")
		                                     .putProperty("id", "6")
		                                     .putProperty("feats", "NOM");

		Token node24 = doc.add(new Token()).setRange(84, 93).putProperty("head", "6")
		                                     .putProperty("stem", "andersson")
		                                     .putProperty("lemma", "Andersson")
		                                     .putProperty("deprel", "HD")
		                                     .putProperty("norm", "andersson")
		                                     .putProperty("ppos", "PM")
		                                     .putProperty("id", "7")
		                                     .putProperty("feats", "NOM");

		Token node25 = doc.add(new Token()).setRange(94, 95).putProperty("lemma", "(")
		                                     .putProperty("head", "4")
		                                     .putProperty("ppos", "PAD")
		                                     .putProperty("id", "8")
		                                     .putProperty("deprel", "IR");

		Token node26 = doc.add(new Token()).setRange(95, 106).putProperty("head", "4")
		                                      .putProperty("stem", "racerför")
		                                      .putProperty("lemma", "racerförare")
		                                      .putProperty("deprel", "AN")
		                                      .putProperty("norm", "racerförare")
		                                      .putProperty("ppos", "NN")
		                                      .putProperty("id", "9")
		                                      .putProperty("feats", "UTR|SIN|IND|NOM");

		Token node27 = doc.add(new Token()).setRange(106, 107).putProperty("lemma", ")")
		                                       .putProperty("head", "4")
		                                       .putProperty("ppos", "PAD")
		                                       .putProperty("id", "10")
		                                       .putProperty("deprel", "IR");

		Token node28 = doc.add(new Token()).setRange(108, 113).putProperty("head", "4")
		                                       .putProperty("stem", "conny")
		                                       .putProperty("lemma", "Conny")
		                                       .putProperty("deprel", "ET")
		                                       .putProperty("norm", "conny")
		                                       .putProperty("ppos", "PM")
		                                       .putProperty("id", "11")
		                                       .putProperty("feats", "NOM");

		Token node29 = doc.add(new Token()).setRange(114, 123).putProperty("head", "11")
		                                       .putProperty("stem", "andersson")
		                                       .putProperty("lemma", "Andersson")
		                                       .putProperty("deprel", "HD")
		                                       .putProperty("norm", "andersson")
		                                       .putProperty("ppos", "PM")
		                                       .putProperty("id", "12")
		                                       .putProperty("feats", "NOM");

		Token node30 = doc.add(new Token()).setRange(124, 125).putProperty("lemma", "(")
		                                       .putProperty("head", "4")
		                                       .putProperty("ppos", "PAD")
		                                       .putProperty("id", "13")
		                                       .putProperty("deprel", "IR");

		Token node31 = doc.add(new Token()).setRange(125, 134).putProperty("head", "4")
		                                       .putProperty("stem", "politik")
		                                       .putProperty("lemma", "politiker")
		                                       .putProperty("deprel", "AN")
		                                       .putProperty("norm", "politiker")
		                                       .putProperty("ppos", "NN")
		                                       .putProperty("id", "14")
		                                       .putProperty("feats", "UTR|PLU|IND|NOM");

		Token node32 = doc.add(new Token()).setRange(134, 135).putProperty("lemma", ")")
		                                       .putProperty("head", "4")
		                                       .putProperty("ppos", "PAD")
		                                       .putProperty("id", "15")
		                                       .putProperty("deprel", "JR");

		doc.add(new DependencyRelation(),  node10, node12).setRelation("SS");

		doc.add(new DependencyRelation(),  node11, node10).setRelation("HD");

		doc.add(new DependencyRelation(),  node13, node12).setRelation("OO");

		doc.add(new DependencyRelation(),  node14, node12).setRelation("RA");

		doc.add(new DependencyRelation(),  node15, node16).setRelation("DT");

		doc.add(new DependencyRelation(),  node16, node14).setRelation("PA");

		doc.add(new DependencyRelation(),  node17, node12).setRelation("IP");

		doc.add(new DependencyRelation(),  node19, node18).setRelation("HD");

		doc.add(new DependencyRelation(),  node20, node18).setRelation("IR");

		doc.add(new DependencyRelation(),  node21, node18).setRelation("AN");

		doc.add(new DependencyRelation(),  node22, node21).setRelation("IR");

		doc.add(new DependencyRelation(),  node23, node21).setRelation("ET");

		doc.add(new DependencyRelation(),  node24, node23).setRelation("HD");

		doc.add(new DependencyRelation(),  node25, node21).setRelation("IR");

		doc.add(new DependencyRelation(),  node26, node21).setRelation("AN");

		doc.add(new DependencyRelation(),  node27, node21).setRelation("IR");

		doc.add(new DependencyRelation(),  node28, node21).setRelation("ET");

		doc.add(new DependencyRelation(),  node29, node28).setRelation("HD");

		doc.add(new DependencyRelation(),  node30, node21).setRelation("IR");

		doc.add(new DependencyRelation(),  node31, node21).setRelation("AN");

		doc.add(new DependencyRelation(),  node32, node21).setRelation("JR");

		return doc;
	}

}
