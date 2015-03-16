package org.apache.lucene.analysis.miscellaneous;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.tokenattributes.*;

import java.io.IOException;

/**
 * Concatenate all tokens between a startToken and endToken with a String separator between them.
 * If startToken is unset, concatenation will start at the beginning of the token stream, so the
 * whole token stream will be concatenated into a single token if endToken is also unset.
 * <p/>
 * Examples:
 *   Default Behavior:
 *     ['the', 'quick', 'brown', fox'] => ['the quick brown fox']
 *   Settings: startToken='<concat>', endToken='</concat>'
 *     ['the', '<concat>', 'quick', 'brown', '</concat>', fox'] => ['the', 'quick brown', 'fox']
 *   Settings: startToken='START', startTokenHandling='exclude'
 *     ['the', 'START', 'quick', 'brown', fox'] =>  ['the', 'START', 'quick brown fox']
 *   Settings: separator='_', startToken='^', endToken='$', startTokenHandling='include', endTokenHandling='exclude'
 *     ['^', 'the', '$', '^', 'quick', 'brown', '$', fox', 'jumped', 'over', '^', 'the', 'lazy', 'dog']
 *         =>  ['^_the', '$', '^_quick_brown', '$', 'fox', jumped', 'over', '^_the_lazy_dog']
 *
 * @param separator          the text to insert between each concatenated token. Defaults to space.
 * @param startToken         if set, only tokens after the startToken and prior to the next endToken will be
 *                           concatenated.  If unset, concatenation starts at the beginning of the token stream.
 * @param endToken           if set, stops concatenating tokens after the immediately preceding token.
 * @param startTokenHandling Supported options: 'exclude', 'include', 'drop' (the default). If set to exclude,
 *                           the start token will not be included in the subsequent concatenated token.
 *                           If set to include, the start token will be included in the subsequent concatenated token.
 *                           If set to drop, the start token will be removed from the token stream.
 * @param endTokenHandling   Supported options: 'exclude', 'include', 'drop' (the default). If set to exclude,
 *                           the end token will not be included in the preceding concatenated token.
 *                           If set to include, the end token will be included in the preceding concatenated token.
 *                           If set to drop, the start token will be removed from the token stream.
 */
public class ConcatenateBetweenFilter extends TokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final PositionLengthAttribute posLenAtt = addAttribute(PositionLengthAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
    //Tokens
    State currentTokenBeingConcatenated;
    State nextTokenAfterConcatenation;
    //Concatenation variables
    boolean isInConcatenatingMode = true;
    boolean isFirstSubToken = true;
    private String separator = " ";
    private String startToken = "";
    private String endToken = "";
    private TokenHandling startTokenHandling = TokenHandling.drop;
    private TokenHandling endTokenHandling = TokenHandling.drop;
    private int concat_posIncrement;
    private int concat_startOffset;
    private int concat_endOffset;
    private boolean done;

    protected ConcatenateBetweenFilter(TokenStream input) {
        super(input);
    }

    public void setTokenSeparator(String separator) {
        this.separator = separator;
    }

    public void setStartToken(String startToken) {
        this.startToken = startToken;
        if (!this.startToken.equals("")) {
            this.isInConcatenatingMode = false;
        }
    }

    public void setEndToken(String endToken) {
        this.endToken = endToken;
    }

    public void setStartTokenHandling(TokenHandling startTokenHandling) {
        this.startTokenHandling = startTokenHandling;
    }

    public void setEndTokenHandling(TokenHandling endTokenHandling) {
        this.endTokenHandling = endTokenHandling;
    }

    @Override
    public void reset() throws IOException {
        input.reset();
        done = false;
    }

    @Override
    public final boolean incrementToken() throws IOException {

        if (nextTokenAfterConcatenation != null) {
            this.restoreState(nextTokenAfterConcatenation);
            nextTokenAfterConcatenation = null;
            return true;
        }

        if (done) {
            return false;
        }

        while (input.incrementToken()) {
            boolean emitTokenNow;

            if (isInConcatenatingMode) {
                emitTokenNow = continueConcatenating();
            } else {
                emitTokenNow = handleTokensNormally();
            }

            if (emitTokenNow) {
                return true;
            }
        }


        //If the stream didn't end with an endToken, there's still one more token to emit
        if (currentTokenBeingConcatenated != null) {
            emitConcatenatedToken();
            return true;
        }

        done = true;
        return false;
    }

    /**
     * Should only be called when isInConcatenatingMode is false. If the next token
     * matches the specified startToken then this method will switch to concatenation mode for
     * the next run. Otherwise, this method will return each subsequent token as-is until a
     * startToken is hit.
     *
     * @return true if a token ready to be emitted, false otherwise
     */
    protected boolean handleTokensNormally() {
        //Not currently concatenating. Look for start start token, otherwise, emit token

        boolean haveTokenToEmit = false;

        String currentTokenText = termAtt.toString();

        if (!this.startToken.equals("") && currentTokenText.equals(this.startToken)) {
            isInConcatenatingMode = true; //turn on concatenation since we hit start token

            //we hit our start token
            if (this.startTokenHandling == TokenHandling.include) {
                this.isFirstSubToken = false;
                this.currentTokenBeingConcatenated = this.captureState();
                haveTokenToEmit = false;
            } else if (this.startTokenHandling == TokenHandling.exclude) {
                this.isFirstSubToken = true;
                //just emit the starting token as-is
                haveTokenToEmit = true;
            } else if (this.startTokenHandling == TokenHandling.drop) {
                haveTokenToEmit = false;
            }
        } else {
            //just emit the current token as-is.
            haveTokenToEmit = true;
        }
        return haveTokenToEmit;
    }

    /**
     * Should only be called when isInConcatenatingMode is false. If the next token
     * matches the specified startToken then this method will switch to concatenation mode for
     * the next run. Otherwise, this method will return each subsequent token as-is until a
     * startToken is hit.
     *
     * @return true if a token ready to be emitted, false otherwise
     */
    protected boolean continueConcatenating() {

        boolean haveTokenToEmit; //false
        String currentTokenText = termAtt.toString();

        if (!this.endToken.equals("") && currentTokenText.equals(this.endToken)) {
            //we hit our end token

            if (this.endTokenHandling == TokenHandling.include) {
                this.concat_endOffset = this.offsetAtt.endOffset();
                this.restoreState(this.currentTokenBeingConcatenated);

                if (!this.isFirstSubToken) {
                    this.termAtt.append(this.separator);
                }
                this.termAtt.append(currentTokenText);
                this.currentTokenBeingConcatenated = this.captureState();
            } else if (this.endTokenHandling == TokenHandling.exclude) {
                nextTokenAfterConcatenation = this.captureState(); //remember the next token for next call to incrementToken
            }

            emitConcatenatedToken();
            return true; //Emit the current token
        } else { //not the end token... keep concatenating
            concat_endOffset = offsetAtt.endOffset();
            if (isFirstSubToken) {
                concat_startOffset = offsetAtt.startOffset();
                concat_posIncrement = posIncrAtt.getPositionIncrement();
                isFirstSubToken = false;
            } else { //previous tokens have been concatenated
                this.restoreState(this.currentTokenBeingConcatenated);
                this.termAtt.append(this.separator + currentTokenText);
            }

            currentTokenBeingConcatenated = this.captureState();
            haveTokenToEmit = false; //keep concatenating
        }
        return haveTokenToEmit;
    }

    protected void emitConcatenatedToken() {
        this.restoreState(currentTokenBeingConcatenated);
        typeAtt.setType(ShingleFilter.DEFAULT_TOKEN_TYPE); //shingle
        this.posIncrAtt.setPositionIncrement(concat_posIncrement);
        this.offsetAtt.setOffset(concat_startOffset, concat_endOffset);
        this.posLenAtt.setPositionLength(1);
        clearConcatAttributes();
        isInConcatenatingMode = false;
    }

    protected void clearConcatAttributes() {
        concat_posIncrement = 1;
        concat_startOffset = 0;
        concat_endOffset = 0;
        isFirstSubToken = true;
        currentTokenBeingConcatenated = null;
    }

    public enum TokenHandling {
        include,
        exclude,
        drop
    }
}
