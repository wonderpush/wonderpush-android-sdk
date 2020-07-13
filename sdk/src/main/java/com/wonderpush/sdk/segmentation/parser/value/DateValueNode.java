package com.wonderpush.sdk.segmentation.parser.value;

import com.wonderpush.sdk.segmentation.parser.ASTValueNode;
import com.wonderpush.sdk.segmentation.parser.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

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
