package com.wonderpush.sdk.segmentation;

class StringValueNode extends ASTValueNode<String> {

    public StringValueNode(ParsingContext context, String value) {
        super(context, value);
    }

    @Override
    <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitStringValueNode(this);
    }

}
