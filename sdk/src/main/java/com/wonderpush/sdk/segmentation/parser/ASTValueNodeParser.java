package com.wonderpush.sdk.segmentation.parser;

import androidx.annotation.Nullable;

public interface ASTValueNodeParser<T> {

    @Nullable
    ASTValueNode<T> parseValue(ParsingContext context, String key, Object input) throws BadInputError;

}
