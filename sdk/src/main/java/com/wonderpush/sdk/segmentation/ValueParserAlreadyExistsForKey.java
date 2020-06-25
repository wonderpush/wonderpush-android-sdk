package com.wonderpush.sdk.segmentation;

class ValueParserAlreadyExistsForKey extends Exception {

    public final String key;
    public final ASTValueNodeParser<?> oldParser;
    public final ASTValueNodeParser<?> newParser;

    public ValueParserAlreadyExistsForKey(String key, ASTValueNodeParser<?> oldParser, ASTValueNodeParser<?> newParser) {
        super("Parser already exists for key " + key);
        this.key = key;
        this.oldParser = oldParser;
        this.newParser = newParser;
    }

}
