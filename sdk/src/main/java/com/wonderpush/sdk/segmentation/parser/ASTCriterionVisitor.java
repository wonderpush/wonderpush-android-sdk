package com.wonderpush.sdk.segmentation.parser;

import com.wonderpush.sdk.segmentation.parser.criteria.ASTUnknownCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.AllCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.AndCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.AnyCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.ComparisonCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.EqualityCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.GeoCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.InsideCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.JoinCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.LastActivityDateCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.MatchAllCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.NotCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.OrCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.PrefixCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.PresenceCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.SubscriptionStatusCriterionNode;

public interface ASTCriterionVisitor<T> {

    public T visitMatchAllCriterionNode(MatchAllCriterionNode node);

    public T visitAndCriterionNode(AndCriterionNode node);

    public T visitOrCriterionNode(OrCriterionNode node);

    public T visitNotCriterionNode(NotCriterionNode node);

    public T visitGeoCriterionNode(GeoCriterionNode node);

    public T visitSubscriptionStatusCriterionNode(SubscriptionStatusCriterionNode node);

    public T visitLastActivityDateCriterionNode(LastActivityDateCriterionNode node);

    public T visitPresenceCriterionNode(PresenceCriterionNode node);

    public T visitJoinCriterionNode(JoinCriterionNode node);

    public T visitEqualityCriterionNode(EqualityCriterionNode node);

    public T visitAnyCriterionNode(AnyCriterionNode node);

    public T visitAllCriterionNode(AllCriterionNode node);

    public T visitComparisonCriterionNode(ComparisonCriterionNode node);

    public T visitPrefixCriterionNode(PrefixCriterionNode node);

    public T visitInsideCriterionNode(InsideCriterionNode node);

    public T visitASTUnknownCriterionNode(ASTUnknownCriterionNode node);

}
