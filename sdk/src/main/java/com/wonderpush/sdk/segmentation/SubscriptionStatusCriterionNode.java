package com.wonderpush.sdk.segmentation;

class SubscriptionStatusCriterionNode extends ASTCriterionNode {

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
