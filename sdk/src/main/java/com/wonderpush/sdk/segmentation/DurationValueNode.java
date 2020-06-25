package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.TimeSync;

class DurationValueNode extends ASTValueNode<Number> {

    public DurationValueNode(ParsingContext context, Number value) {
        super(context, value);
    }

    private static Number durationToNumber(ISO8601Duration duration) {
        long now = TimeSync.getTime();
        long then = duration.applyTo(now);
        return then - now;
    }

    public DurationValueNode(ParsingContext context, ISO8601Duration duration) {
        this(context, durationToNumber(duration));
    }

    @Override
    <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitDurationValueNode(this);
    }

}
