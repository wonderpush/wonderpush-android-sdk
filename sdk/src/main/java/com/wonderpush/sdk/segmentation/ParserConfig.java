package com.wonderpush.sdk.segmentation;

public class ParserConfig {

    public final ASTValueNodeParser<?> valueParser;
    public final ASTCriterionNodeParser criterionParser;
    public final boolean throwOnUnknownValue;
    public final boolean throwOnUnknownCriterion;

    public ParserConfig(ASTValueNodeParser<?> valueParser, ASTCriterionNodeParser criterionParser) {
        this(valueParser, criterionParser, false, false);
    }

    public ParserConfig(ASTValueNodeParser<?> valueParser, ASTCriterionNodeParser criterionParser, boolean throwOnUnknownValue, boolean throwOnUnknownCriterion) {
        this.valueParser = valueParser;
        this.criterionParser = criterionParser;
        this.throwOnUnknownValue = throwOnUnknownValue;
        this.throwOnUnknownCriterion = throwOnUnknownCriterion;
    }

}
