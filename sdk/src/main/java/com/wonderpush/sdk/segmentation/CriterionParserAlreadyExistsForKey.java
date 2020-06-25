package com.wonderpush.sdk.segmentation;

class CriterionParserAlreadyExistsForKey extends Exception {

    public final String key;
    public final ASTCriterionNodeParser oldParser;
    public final ASTCriterionNodeParser newParser;

    public CriterionParserAlreadyExistsForKey(String key, ASTCriterionNodeParser oldParser, ASTCriterionNodeParser newParser) {
        super("Parser already exists for key " + key);
        this.key = key;
        this.oldParser = oldParser;
        this.newParser = newParser;
    }
}
