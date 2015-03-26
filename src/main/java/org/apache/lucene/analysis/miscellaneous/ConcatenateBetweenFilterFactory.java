package org.apache.lucene.analysis.miscellaneous;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

/**
 * @see ConcatenateBetweenFilter
 */
public class ConcatenateBetweenFilterFactory extends TokenFilterFactory {

    private final String tokenSeparator;
    private final String startToken;
    private final String endToken;
    private ConcatenateBetweenFilter.TokenHandling startTokenHandling;
    private ConcatenateBetweenFilter.TokenHandling endTokenHandling;

    /**
     * Initialize this factory via a set of key-value pairs.
     */
    public ConcatenateBetweenFilterFactory(Map<String, String> args) {
        super(args);
        tokenSeparator = get(args, "tokenSeparator", " ");
        startToken = get(args, "startToken", "");
        endToken = get(args, "endToken", "");
        startTokenHandling = ConcatenateBetweenFilter.TokenHandling.valueOf(get(args, "startTokenHandling", ConcatenateBetweenFilter.TokenHandling.drop.toString()));
        endTokenHandling = ConcatenateBetweenFilter.TokenHandling.valueOf(get(args, "endTokenHandling", ConcatenateBetweenFilter.TokenHandling.drop.toString()));
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public TokenStream create(TokenStream input) {
        ConcatenateBetweenFilter filter = new ConcatenateBetweenFilter(input);
        filter.setTokenSeparator(tokenSeparator);
        filter.setStartToken(startToken);
        filter.setEndToken(endToken);
        filter.setStartTokenHandling(startTokenHandling);
        filter.setEndTokenHandling(endTokenHandling);
        return filter;
    }
}
