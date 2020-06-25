package com.wonderpush.sdk.segmentation;

class DateValueNode extends ASTValueNode<Number> {

    // value is a unix timestamp in milliseconds
    public DateValueNode(ParsingContext context, Number value) {
        super(context, value);
    }

    @Override
    <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitDateValueNode(this);
    }

}
