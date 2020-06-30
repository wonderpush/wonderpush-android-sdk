package com.wonderpush.sdk.segmentation.parser.criteria;

import com.wonderpush.sdk.segmentation.parser.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.parser.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.parser.value.GeoAbstractAreaValueNode;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

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
