package com.wonderpush.sdk.segmentation;

class NumberValueNode extends ASTValueNode<Number> {

    public NumberValueNode(ParsingContext context, Number value) {
        super(context, value);
    }

    @Override
    <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitNumberValueNode(this);
    }

}
