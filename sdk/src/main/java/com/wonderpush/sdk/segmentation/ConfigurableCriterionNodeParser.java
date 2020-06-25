package com.wonderpush.sdk.segmentation;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurableCriterionNodeParser implements ASTCriterionNodeParser {

    public final Map<String, ASTCriterionNodeParser> exactNameParsers = new HashMap<>();
    public final List<ASTCriterionNodeParser> dynamicNameParsers = new ArrayList<>();

    public void registerExactNameParser(String key, ASTCriterionNodeParser parser) throws CriterionParserAlreadyExistsForKey {
        ASTCriterionNodeParser oldParser = this.exactNameParsers.get(key);
        if (oldParser != null) {
            throw new CriterionParserAlreadyExistsForKey(key, oldParser, parser);
        }
        this.exactNameParsers.put(key, parser);
    }

    public void registerDynamicNameParser(ASTCriterionNodeParser parser) {
        this.dynamicNameParsers.add(parser);
    }

    @Nullable
    @Override
    public ASTCriterionNode parseCriterion(ParsingContext context, String key, Object input) throws BadInputError {
        ASTCriterionNodeParser exactNameParser = this.exactNameParsers.get(key);
        if (exactNameParser != null) {
            return exactNameParser.parseCriterion(context, key, input);
        }
        for (ASTCriterionNodeParser parser : this.dynamicNameParsers) {
            ASTCriterionNode parsed = parser.parseCriterion(context, key, input);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

}
