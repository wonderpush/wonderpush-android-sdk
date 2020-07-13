package com.wonderpush.sdk.segmentation.parser.value;

import com.wonderpush.sdk.TimeSync;
import com.wonderpush.sdk.segmentation.parser.ASTValueNode;
import com.wonderpush.sdk.segmentation.parser.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.parser.ISO8601Duration;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

public class RelativeDateValueNode extends ASTValueNode<Number> {

    public final ISO8601Duration duration;

    public RelativeDateValueNode(ParsingContext context, ISO8601Duration duration) {
        super(context, Float.NaN);
        this.duration = duration;
    }

    @Override
    public Number getValue() {
        return this.duration.applyTo(TimeSync.getTime());
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitRelativeDateValueNode(this);
    }

}
