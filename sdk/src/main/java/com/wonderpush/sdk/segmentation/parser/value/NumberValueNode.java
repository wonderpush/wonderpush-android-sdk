package com.wonderpush.sdk.segmentation.parser.value;

import com.wonderpush.sdk.segmentation.parser.ASTValueNode;
import com.wonderpush.sdk.segmentation.parser.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

public class NumberValueNode extends ASTValueNode<Number> {

    public NumberValueNode(ParsingContext context, Number value) {
        super(context, value);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitNumberValueNode(this);
    }

}
