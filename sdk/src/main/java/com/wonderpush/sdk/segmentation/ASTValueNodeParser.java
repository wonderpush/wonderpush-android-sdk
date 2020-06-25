package com.wonderpush.sdk.segmentation;

import android.support.annotation.Nullable;

public interface ASTValueNodeParser<T> {

    @Nullable
    ASTValueNode<T> parseValue(ParsingContext context, String key, Object input) throws BadInputError;

}
