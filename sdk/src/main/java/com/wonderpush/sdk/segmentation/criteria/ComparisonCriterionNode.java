package com.wonderpush.sdk.segmentation.criteria;

import com.wonderpush.sdk.segmentation.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.ASTValueNode;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class ComparisonCriterionNode extends ASTCriterionNode {

    public enum Comparator {
        gt,
        gte,
        lt,
        lte,
    }

    public final Comparator comparator;
    public final ASTValueNode<Object> value;

    public ComparisonCriterionNode(ParsingContext context, Comparator comparator, ASTValueNode<Object> value) {
        super(context);
        this.comparator = comparator;
        this.value = value;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitComparisonCriterionNode(this);
    }

}
