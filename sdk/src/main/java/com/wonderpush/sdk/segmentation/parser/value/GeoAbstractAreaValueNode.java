package com.wonderpush.sdk.segmentation.parser.value;

import com.wonderpush.sdk.segmentation.parser.ASTValueNode;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

public abstract class GeoAbstractAreaValueNode<T> extends ASTValueNode<T> {

    public GeoAbstractAreaValueNode(ParsingContext context, T value) {
        super(context, value);
    }

}
