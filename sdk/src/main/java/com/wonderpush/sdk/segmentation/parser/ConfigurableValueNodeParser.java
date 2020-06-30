package com.wonderpush.sdk.segmentation.parser;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurableValueNodeParser implements ASTValueNodeParser<Object> {

    public final Map<String, ASTValueNodeParser<?>> exactNameParsers = new HashMap<>();
    public final List<ASTValueNodeParser<?>> dynamicNameParsers = new ArrayList<>();

    public void registerExactNameParser(String key, ASTValueNodeParser<?> parser) throws ValueParserAlreadyExistsForKey {
        ASTValueNodeParser<?> oldParser = this.exactNameParsers.get(key);
        if (oldParser != null) {
            throw new ValueParserAlreadyExistsForKey(key, oldParser, parser);
        }
        this.exactNameParsers.put(key, parser);
    }

    public void registerDynamicNameParser(ASTValueNodeParser<?> parser) {
        this.dynamicNameParsers.add(parser);
    }

    @Nullable
    @Override
    public ASTValueNode<Object> parseValue(ParsingContext context, String key, Object input) throws BadInputError {
        ASTValueNodeParser<?> exactNameParser = this.exactNameParsers.get(key);
        if (exactNameParser != null) {
            return ASTValueNode.castToObject(exactNameParser.parseValue(context, key, input));
        }
        for (ASTValueNodeParser<?> parser : this.dynamicNameParsers) {
            ASTValueNode<?> parsed = parser.parseValue(context, key, input);
            if (parsed != null) {
                return ASTValueNode.castToObject(parsed);
            }
        }
        return null;
    }

}
