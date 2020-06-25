package com.wonderpush.sdk.segmentation;

import org.json.JSONObject;

class NullValueNode extends ASTValueNode<Object> {

    public NullValueNode(ParsingContext context) {
        super(context, JSONObject.NULL);
    }

    @Override
    <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitNullValueNode(this);
    }

}
