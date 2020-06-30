package com.wonderpush.sdk.segmentation.parser.criteria;

import com.wonderpush.sdk.segmentation.parser.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.parser.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

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
