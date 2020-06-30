package com.wonderpush.sdk.segmentation.parser.value;

import com.wonderpush.sdk.TimeSync;
import com.wonderpush.sdk.segmentation.parser.ASTValueNode;
import com.wonderpush.sdk.segmentation.parser.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.parser.ISO8601Duration;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

public class DurationValueNode extends ASTValueNode<Number> {

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
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitDurationValueNode(this);
    }

}
