package com.wonderpush.sdk.segmentation.criteria;

import com.wonderpush.sdk.segmentation.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.ASTValueNode;
import com.wonderpush.sdk.segmentation.ParsingContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllCriterionNode extends ASTCriterionNode {

    public final List<ASTValueNode<Object>> values;

    public AllCriterionNode(ParsingContext context, List<ASTValueNode<Object>> values) {
        super(context);
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitAllCriterionNode(this);
    }

}
