package com.wonderpush.sdk.segmentation;

abstract class ASTValueNode<T> {

    public final ParsingContext context;
    protected final T value;

    public ASTValueNode(ParsingContext context, T value) {
        this.context = context;
        this.value = value;
    }

    abstract <U> U accept(ASTValueVisitor<U> visitor);

    public T getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public static ASTValueNode<Object> castToObject(ASTValueNode<?> node) {
        if (node == null) return null;
        return (ASTValueNode<Object>) node;
    }

}
