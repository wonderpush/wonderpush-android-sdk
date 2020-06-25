package com.wonderpush.sdk.segmentation;

interface ASTCriterionVisitor<T> {

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
