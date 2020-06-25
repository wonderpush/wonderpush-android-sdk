package com.wonderpush.sdk.segmentation.criteria;

import com.wonderpush.sdk.segmentation.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class GeoCriterionNode extends ASTCriterionNode {

    public final ASTCriterionNode locationComparison;
    public final ASTCriterionNode dateComparison;

    public GeoCriterionNode(ParsingContext context, ASTCriterionNode locationComparison, ASTCriterionNode dateComparison) {
        super(context);
        this.locationComparison = locationComparison;
        this.dateComparison = dateComparison;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitGeoCriterionNode(this);
    }

}
