package com.wonderpush.sdk.segmentation.parser;

public abstract class ASTValueNode<T> {

    public final ParsingContext context;
    private final T value;

    public ASTValueNode(ParsingContext context, T value) {
        this.context = context;
        this.value = value;
    }

    public abstract <U> U accept(ASTValueVisitor<U> visitor);

    public T getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public static ASTValueNode<Object> castToObject(ASTValueNode<?> node) {
        if (node == null) return null;
        return (ASTValueNode<Object>) node;
    }

}
