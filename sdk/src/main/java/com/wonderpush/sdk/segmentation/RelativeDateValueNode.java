package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.TimeSync;

class RelativeDateValueNode extends ASTValueNode<Number> {

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
    <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitRelativeDateValueNode(this);
    }

}
