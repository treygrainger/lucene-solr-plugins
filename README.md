<h1>lucene-solr-plugins</h1>
Where I put useful plugins that haven't been committed to Lucene/Solr yet. Included plugins are listed below.

<h3>ConcatenateBetweenFilter:</h3>
<p>A Token Filter used to concatenate one or more tokens into a single token within a token stream. You can specify a token separator, a token at which to begin concatenation, and token at which to end concatenation, whether you want those marker tokens to be included, excluded, or dropped from concatenation. By default, it concatenates all tokens in the token stream with a space.</p>

<p>With default settings:</p>
       ['the', 'quick', 'brown', fox'] => ['the quick brown fox']

<p>With startToken='&lt;concat&gt;', endToken='&lt;/concat&gt;'</p>
       ['the', '<concat>', 'quick', 'brown', '</concat>', fox'] => ['the', 'quick brown', 'fox']

Configurable parameters:       
 * **separator**: the text to insert between each concatenated token. Defaults to space.
 * **startToken**: if set, only tokens after the startToken and prior to the next endToken will be
                   concatenated.  If unset, concatenation starts at the beginning of the token stream.
 * **endToken**: if set, stops concatenating tokens after the immediately preceding token.
 * **startTokenHandling**: Supported options: 'separate', 'combine', 'drop' (the default). If set to separate,
                            the start token will not be included in the subsequent concatenated token.
                            If set to combine, the start token will be included in the subsequent concatenated token.
                            If set to drop, the start token will be removed from the token stream.
 * **endTokenHandling**: Supported options: 'separate', 'combine', 'drop' (the default). If set to separate,
                            the end token will not be included in the preceding concatenated token.
                            If set to combine, the end token will be included in the preceding concatenated token.
                            If set to drop, the start token will be removed from the token stream.
