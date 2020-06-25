package com.wonderpush.sdk.segmentation.value;

import com.wonderpush.sdk.segmentation.ASTValueNode;
import com.wonderpush.sdk.segmentation.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

import org.json.JSONObject;

public class NullValueNode extends ASTValueNode<Object> {

    public NullValueNode(ParsingContext context) {
        super(context, JSONObject.NULL);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitNullValueNode(this);
    }

}
