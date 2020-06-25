package com.wonderpush.sdk.segmentation;

import android.support.annotation.Nullable;

interface ASTCriterionNodeParser {

    @Nullable ASTCriterionNode parseCriterion(ParsingContext context, String key, Object input) throws BadInputError;

}
