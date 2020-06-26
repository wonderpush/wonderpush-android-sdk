package com.wonderpush.sdk.segmentation;

import android.support.annotation.Nullable;

import com.wonderpush.sdk.segmentation.criteria.UnknownCriterionError;

public interface ASTCriterionNodeParser {

    @Nullable
    ASTCriterionNode parseCriterion(ParsingContext context, String key, Object input) throws BadInputError, UnknownCriterionError, UnknownValueError;

}
