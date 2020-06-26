package com.wonderpush.sdk.segmentation;

public class ParserConfig {

    public final ASTValueNodeParser<?> valueParser;
    public final ASTCriterionNodeParser criterionParser;
    public final boolean throwOnUnknownCriterion;
    public final boolean throwOnUnknownValue;

    public ParserConfig(ASTValueNodeParser<?> valueParser, ASTCriterionNodeParser criterionParser) {
        this(valueParser, criterionParser, false, false);
    }

    public ParserConfig(ASTValueNodeParser<?> valueParser, ASTCriterionNodeParser criterionParser, boolean throwOnUnknownCriterion, boolean throwOnUnknownValue) {
        this.valueParser = valueParser;
        this.criterionParser = criterionParser;
        this.throwOnUnknownCriterion = throwOnUnknownCriterion;
        this.throwOnUnknownValue = throwOnUnknownValue;
    }

}
