package org.apache.lucene.analysis.miscellaneous;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

import java.io.IOException;
import java.io.StringReader;

public class ConcatenateBetweenFilterTest extends BaseTokenStreamTestCase {

    public void testDefaults() throws IOException {
        String inputText = "one two three";
        TokenStream stream = new WhitespaceTokenizer(new StringReader(inputText));
        ConcatenateBetweenFilter filter = new ConcatenateBetweenFilter(stream);

        assertTokenStreamContents(
                filter, //tokenstream
                new String[]{inputText}, //output: one two three
                new int[]{0}, //startOffsets: 0
                new int[]{inputText.length()}, //endOffsets: 13
                new String[]{"shingle"}, //type: shingle
                new int[]{1}, //positionIncrements: 1
                new int[]{1}, //positionLengths: 1
                inputText.length(), //finalOffsets
                true); //offsets are correct
    }


    public void testSeparator() throws IOException {
        String inputText = "one two | three four five";
        TokenStream stream = new WhitespaceTokenizer(new StringReader(inputText));
        ConcatenateBetweenFilter filter = new ConcatenateBetweenFilter(stream);
        filter.setStartToken("|");
        filter.setTokenSeparator("_xyz_");

        assertTokenStreamContents(
                filter,
                new String[]{"one", "two", "three_xyz_four_xyz_five"}
        );
    }

    public void testTokenHandling() throws IOException {
        String inputText = "one START two three END four";
        TokenStream stream;
        ConcatenateBetweenFilter filter;

        //StartTokenHandling=include
        stream = new WhitespaceTokenizer(new StringReader(inputText));
        filter = new ConcatenateBetweenFilter(stream);
        filter.setStartTokenHandling(ConcatenateBetweenFilter.TokenHandling.include);
        filter.setStartToken("START");
        filter.setEndToken("END");
        assertTokenStreamContents(
                filter,
                new String[]{"one", "START two three", "four"}
        );


        //EndTokenHandling=include
        stream = new WhitespaceTokenizer(new StringReader(inputText));
        filter = new ConcatenateBetweenFilter(stream);
        filter.setEndTokenHandling(ConcatenateBetweenFilter.TokenHandling.include);
        filter.setStartToken("START");
        filter.setEndToken("END");
        assertTokenStreamContents(
                filter,
                new String[]{"one", "two three END", "four"}
        );

        //StartTokenHandling=exclude
        stream = new WhitespaceTokenizer(new StringReader(inputText));
        filter = new ConcatenateBetweenFilter(stream);
        filter.setStartTokenHandling(ConcatenateBetweenFilter.TokenHandling.exclude);
        filter.setStartToken("START");
        filter.setEndToken("END");
        assertTokenStreamContents(
                filter,
                new String[]{"one", "START", "two three", "four"}
        );

        //EndTokenHandling=exclude
        stream = new WhitespaceTokenizer(new StringReader(inputText));
        filter = new ConcatenateBetweenFilter(stream);
        filter.setEndTokenHandling(ConcatenateBetweenFilter.TokenHandling.exclude);
        filter.setStartToken("START");
        filter.setEndToken("END");
        assertTokenStreamContents(
                filter,
                new String[]{"one", "two three", "END", "four"}
        );

        //startToken and endToken are the same string
        inputText = "one two | three four | five six | | seven eight | | | nine ten";
        stream = new WhitespaceTokenizer(new StringReader(inputText));
        filter = new ConcatenateBetweenFilter(stream);
        filter.setStartToken("|");
        filter.setEndToken("|");
        filter.setTokenSeparator("+");
        filter.setStartTokenHandling(ConcatenateBetweenFilter.TokenHandling.include);
        filter.setEndTokenHandling(ConcatenateBetweenFilter.TokenHandling.include);

        assertTokenStreamContents(
                filter,
                new String[]{"one", "two", "|+three+four+|", "five", "six", "|+|", "seven", "eight", "|+|", "|+nine+ten"}
        );

    }

    public void testMultipleStartAndEndTokens() throws IOException {
        String inputText = "zero one <concat> two three </concat> four <concat> five six seven </concat>";
        TokenStream stream = new WhitespaceTokenizer(new StringReader(inputText));
        ConcatenateBetweenFilter filter = new ConcatenateBetweenFilter(stream);
        filter.setStartToken("<concat>");
        filter.setEndToken("</concat>");
        assertTokenStreamContents(
                filter, //tokenstream
                new String[]{"zero", "one", "two three", "four", "five six seven"}, //output
                new int[]{0, //startOffsets
                        "zero ".length(),
                        "zero one <concat> ".length(),
                        "zero one <concat> two three </concat> ".length(),
                        "zero one <concat> two three </concat> four <concat> ".length()},

                new int[]{"zero".length(),
                        "zero one".length(), //endOffsets
                        "zero one <concat> two three".length(),
                        "zero one <concat> two three </concat> four".length(),
                        "zero one <concat> two three </concat> four <concat> five six seven".length()},

                new String[]{"word", "word", "shingle", "word", "shingle"}, //type: shingle
                new int[]{1, 1, 1, 1, 1}, //positionIncrements: 1
                new int[]{1, 1, 1, 1, 1}, //positionLengths: 3
                inputText.length(), //finalOffsets
                true); //offsets are correct
    }

}
