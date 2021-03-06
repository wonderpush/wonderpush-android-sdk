package com.wonderpush.sdk.segmentation.parser;

import androidx.annotation.Nullable;

import com.wonderpush.sdk.segmentation.parser.criteria.UnknownCriterionError;

public interface ASTCriterionNodeParser {

    @Nullable
    ASTCriterionNode parseCriterion(ParsingContext context, String key, Object input) throws BadInputError, UnknownCriterionError, UnknownValueError;

}
