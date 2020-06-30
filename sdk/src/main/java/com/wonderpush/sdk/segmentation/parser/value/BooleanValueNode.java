package com.wonderpush.sdk.segmentation.parser.value;

import com.wonderpush.sdk.segmentation.parser.ASTValueNode;
import com.wonderpush.sdk.segmentation.parser.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

public class BooleanValueNode extends ASTValueNode<Boolean> {

    public BooleanValueNode(ParsingContext context, Boolean value) {
        super(context, value);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitBooleanValueNode(this);
    }

}
