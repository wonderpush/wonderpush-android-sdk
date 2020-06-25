package com.wonderpush.sdk.segmentation.criteria;

import com.wonderpush.sdk.segmentation.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AndCriterionNode extends ASTCriterionNode {

    public final List<ASTCriterionNode> children;

    public AndCriterionNode(ParsingContext context, List<ASTCriterionNode> children) {
        super(context);
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitAndCriterionNode(this);
    }

}
