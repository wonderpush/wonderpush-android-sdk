package com.wonderpush.sdk.segmentation.value;

import com.wonderpush.sdk.segmentation.ASTValueNode;
import com.wonderpush.sdk.segmentation.ParsingContext;

public abstract class GeoAbstractAreaValueNode<T> extends ASTValueNode<T> {

    public GeoAbstractAreaValueNode(ParsingContext context, T value) {
        super(context, value);
    }

}
