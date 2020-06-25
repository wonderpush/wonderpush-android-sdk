package com.wonderpush.sdk.segmentation.value;

import com.wonderpush.sdk.segmentation.ASTValueNode;
import com.wonderpush.sdk.segmentation.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class NumberValueNode extends ASTValueNode<Number> {

    public NumberValueNode(ParsingContext context, Number value) {
        super(context, value);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitNumberValueNode(this);
    }

}
