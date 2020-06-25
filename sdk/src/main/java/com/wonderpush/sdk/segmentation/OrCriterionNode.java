package com.wonderpush.sdk.segmentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class OrCriterionNode extends ASTCriterionNode {

    public final List<ASTCriterionNode> children;

    public OrCriterionNode(ParsingContext context, List<ASTCriterionNode> children) {
        super(context);
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitOrCriterionNode(this);
    }

}
