package com.wonderpush.sdk.segmentation.parser.value;

import com.wonderpush.sdk.segmentation.parser.ASTValueNode;
import com.wonderpush.sdk.segmentation.parser.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

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
