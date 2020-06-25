package com.wonderpush.sdk.segmentation.value;

import com.wonderpush.sdk.segmentation.ASTValueNode;
import com.wonderpush.sdk.segmentation.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class DateValueNode extends ASTValueNode<Number> {

    // value is a unix timestamp in milliseconds
    public DateValueNode(ParsingContext context, Number value) {
        super(context, value);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitDateValueNode(this);
    }

}
