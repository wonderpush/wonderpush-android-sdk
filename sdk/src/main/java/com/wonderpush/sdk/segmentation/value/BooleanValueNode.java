package com.wonderpush.sdk.segmentation.value;

import com.wonderpush.sdk.segmentation.ASTValueNode;
import com.wonderpush.sdk.segmentation.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class BooleanValueNode extends ASTValueNode<Boolean> {

    public BooleanValueNode(ParsingContext context, Boolean value) {
        super(context, value);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitBooleanValueNode(this);
    }

}
