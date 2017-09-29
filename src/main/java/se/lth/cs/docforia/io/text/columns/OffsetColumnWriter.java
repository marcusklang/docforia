package se.lth.cs.docforia.io.text.columns;

import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.NodeStore;
import se.lth.cs.docforia.io.text.ColumnReader;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

import java.util.List;


/**
 * Write the raw offset as two columns start, end
  */
public class OffsetColumnWriter implements ColumnReader {

    int offset;

    /**
     * Offset column writer, simplified constructor
     */
    public OffsetColumnWriter() {
        this(-1);
    }

    /**
     * Offset column writer, Full constructor
     * @param offset position or -1 if add to end
     */
    public OffsetColumnWriter(int offset) {
        this.offset = offset;
    }


    @Override
    public void load(Document doc, List<TextSentence> sentences) {
        for (TextSentence sentence : sentences) {
            int realoffset = offset;
            if(offset == -1)
                realoffset = sentence.getMinNumCols();

            for (TextToken token : sentence) {
                NodeStore raw = token.token();
                token.setProperty(realoffset, String.valueOf(raw.getStart()));
                token.setProperty(realoffset+1, String.valueOf(raw.getEnd()));
            }

            sentence.setMinCols(realoffset+2);
        }
    }
}
