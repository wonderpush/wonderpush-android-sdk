package com.wonderpush.sdk.segmentation;

import org.json.JSONObject;

class ASTUnknownValueNode extends ASTValueNode<Object> {

    public final String key;
    public final Object value;

    public ASTUnknownValueNode(ParsingContext context, String key, Object value) {
        super(context, JSONObject.NULL);
        this.key = key;
        this.value = value;
    }

    @Override
    public <T> T accept(ASTValueVisitor<T> visitor) {
        return visitor.visitASTUnknownValueNode(this);
    }

}
