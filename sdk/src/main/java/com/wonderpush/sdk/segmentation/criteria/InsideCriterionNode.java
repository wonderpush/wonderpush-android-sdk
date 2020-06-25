package com.wonderpush.sdk.segmentation.criteria;

import com.wonderpush.sdk.segmentation.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.value.GeoAbstractAreaValueNode;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class InsideCriterionNode extends ASTCriterionNode {

    public final GeoAbstractAreaValueNode value;

    public InsideCriterionNode(ParsingContext context, GeoAbstractAreaValueNode value) {
        super(context);
        this.value = value;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitInsideCriterionNode(this);
    }

}
