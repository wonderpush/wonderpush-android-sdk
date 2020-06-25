package com.wonderpush.sdk.segmentation;

class ParserConfig {

    public final ASTValueNodeParser<?> valueParser;
    public final ASTCriterionNodeParser criterionParser;

    public ParserConfig(ASTValueNodeParser<?> valueParser, ASTCriterionNodeParser criterionParser) {
        this.valueParser = valueParser;
        this.criterionParser = criterionParser;
    }

}
