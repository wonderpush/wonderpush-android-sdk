package com.wonderpush.sdk.segmentation.parser.criteria;

import com.wonderpush.sdk.segmentation.parser.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.parser.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.parser.ASTValueNode;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

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
