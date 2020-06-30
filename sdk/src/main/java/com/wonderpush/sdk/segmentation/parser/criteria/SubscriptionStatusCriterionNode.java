package com.wonderpush.sdk.segmentation.parser.criteria;

import com.wonderpush.sdk.segmentation.parser.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.parser.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

public class SubscriptionStatusCriterionNode extends ASTCriterionNode {

    public enum SubscriptionStatus {
        optIn,
        optOut,
        softOptOut,
    }

    public final SubscriptionStatus subscriptionStatus;

    public SubscriptionStatusCriterionNode(ParsingContext context, SubscriptionStatus subscriptionStatus) {
        super(context);
        this.subscriptionStatus = subscriptionStatus;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitSubscriptionStatusCriterionNode(this);
    }

}
