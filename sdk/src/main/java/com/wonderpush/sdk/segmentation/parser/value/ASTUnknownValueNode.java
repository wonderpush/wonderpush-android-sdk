package com.wonderpush.sdk.segmentation.parser.value;

import com.wonderpush.sdk.segmentation.parser.ASTValueNode;
import com.wonderpush.sdk.segmentation.parser.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

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
