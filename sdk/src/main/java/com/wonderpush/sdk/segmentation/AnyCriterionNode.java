package com.wonderpush.sdk.segmentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class AnyCriterionNode extends ASTCriterionNode {

    public final List<ASTValueNode<Object>> values;

    public AnyCriterionNode(ParsingContext context, List<ASTValueNode<Object>> values) {
        super(context);
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitAnyCriterionNode(this);
    }

}
