package com.wonderpush.sdk.segmentation.value;

import com.wonderpush.sdk.segmentation.ASTValueNode;
import com.wonderpush.sdk.segmentation.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

import org.json.JSONObject;

public class ASTUnknownValueNode extends ASTValueNode<Object> {

    public final String key;

    public ASTUnknownValueNode(ParsingContext context, String key, Object value) {
        super(context, value);
        this.key = key;
    }

    @Override
    public <T> T accept(ASTValueVisitor<T> visitor) {
        return visitor.visitASTUnknownValueNode(this);
    }

}
