package com.wonderpush.sdk.segmentation;

class BooleanValueNode extends ASTValueNode<Boolean> {

    public BooleanValueNode(ParsingContext context, Boolean value) {
        super(context, value);
    }

    @Override
    <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitBooleanValueNode(this);
    }

}
