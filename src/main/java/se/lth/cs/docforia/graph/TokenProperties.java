package se.lth.cs.docforia.graph;

import se.lth.cs.docforia.Node;

/**
 * Provides a set of common token properties to use
 */
public class TokenProperties {
    /** Token ID */
    public static final String ID = "id";

    /** Token Text, usually redundant, because it is provided by {@link Node#text()} */
    public static final String FORM = "form";

    /** Baseform or canonical word of a token,
     * <a href="https://en.wikipedia.org/wiki/Lemma_(morphology)">Wikipedia article</a> */
    public static final String LEMMA ="lemma";

    /** Predicted baseform of a token */
    public static final String PLEMMA = "plemma";

    /** Part of speech */
    public static final String POS = "pos";

    /** Predicted part of speech */
    public static final String PPOS = "ppos";

    /** Token Features, singular, use of FEATS instead if possible. */
    public static final String FEAT = "feat";

    /** Predicted token features */
    public static final String PFEAT = "pfeat";

    /** Coarse part-of-speech tag */
    public static final String CPOSTAG = "cpostag";

    /** Another way to name a part-of-speach tag */
    public static final String POSTAG = "postag";

    /** Token features, multiple */
    public static final String FEATS = "feats";

    /** Token head, used in dependency grammar */
    public static final String HEAD = "head";

    /** The dependency label, or relation in dependency grammar */
    public static final String DEPREL = "deprel";

    /** Predicted token head */
    public static final String PHEAD = "phead";

    /** Predicted dependency label or relation */
    public static final String PDEPREL = "pdeprel";

    /** Boolean property indicating if token should be considered a predicate */
    public static final String FILLPRED = "fillpred";

    /** Predicate */
    public static final String PRED = "pred";

    /** Predicates argument dependencies and labels */
    public static final String APRED = "apred";

    /** Coreference */
    public static final String COREF = "coref";

    /** Named Entity */
    public static final String NE = "ne";

    /** Predicted Named Entity */
    public static final String PNE = "pne";

    /** Word stem */
    public static final String STEM = "stem";

    /** Normalized word */
    public static final String NORMALIZED = "norm";

    /** Stopword indication */
    public static final String STOPWORD = "stopword";

    /** Named Entity Linking */
    public static final String NEL = "nel";

    /** Predicted Named Entity Linking */
    public static final String PNEL = "pnel";

    /** General link */
    public static final String LINK = "link";
}
