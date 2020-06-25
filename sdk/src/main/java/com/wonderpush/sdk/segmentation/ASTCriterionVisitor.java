package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.criteria.ASTUnknownCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.AllCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.AndCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.AnyCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.ComparisonCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.EqualityCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.GeoCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.InsideCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.JoinCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.LastActivityDateCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.MatchAllCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.NotCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.OrCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.PrefixCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.PresenceCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.SubscriptionStatusCriterionNode;

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
