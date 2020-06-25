package com.wonderpush.sdk.segmentation.criteria;

import com.wonderpush.sdk.segmentation.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class PresenceCriterionNode extends ASTCriterionNode {

    public final boolean present;
    public final ASTCriterionNode sinceDateComparison;
    public final ASTCriterionNode elapsedTimeComparison;

    public PresenceCriterionNode(ParsingContext context, boolean present, ASTCriterionNode sinceDateComparison, ASTCriterionNode elapsedTimeComparison) {
        super(context);
        this.present = present;
        this.sinceDateComparison = sinceDateComparison;
        this.elapsedTimeComparison = elapsedTimeComparison;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitPresenceCriterionNode(this);
    }

}
