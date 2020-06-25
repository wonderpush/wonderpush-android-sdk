package com.wonderpush.sdk.segmentation;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SegmentationDSLParser {

    public final String TAG = "WonderPush." + SegmentationDSLParser.class.getSimpleName();

    public final ParserConfig parserConfig;

    public SegmentationDSLParser(ParserConfig parserConfig) {
        this.parserConfig = parserConfig;
    }

    public ASTCriterionNode parse(JSONObject input, DataSource dataSource) throws BadInputError {
        ParsingContext ctx = new ParsingContext(this, null, dataSource);
        return this.parseCriterion(ctx, input);
    }

    public ASTCriterionNode parseCriterion(ParsingContext context, JSONObject input) throws BadInputError {
        if (input == null) {
            throw new BadInputError("Expects an object");
        }
        if (input.length() == 0) {
            if (context.dataSource.getRootDataSource() != context.dataSource) {
                throw new BadInputError("Missing data criterion");
            }
            return new MatchAllCriterionNode(context);
        }
        if (input.length() > 1) {
            List<ASTCriterionNode> children = new ArrayList<>(input.length());
            Iterator<String> keysIt = input.keys();
            JSONObject childInput = new JSONObject();
            while (keysIt.hasNext()) {
                String key = keysIt.next();
                Object value = input.opt(key);
                try {
                    childInput.putOpt(key, value);
                    children.add(this.parseCriterion(context, childInput));
                    childInput.remove(key);
                } catch (JSONException ex) {
                    Log.e(TAG, "Unexpected error while dissecting an implicit and criterion object");
                }
            }
            return new AndCriterionNode(context, children);
        }
        String inputKey = input.keys().next();
        if (inputKey.length() == 0) {
            throw new BadInputError("Bad key \"\"");
        }
        Object inputValue = input.opt(inputKey);
        ASTCriterionNode parsed = this.parserConfig.criterionParser.parseCriterion(context, inputKey, inputValue);
        if (parsed == null) {
            parsed = new ASTUnknownCriterionNode(context, inputKey, inputValue);
            // NOTE: I removed the possibility to optionally throw an UnknownCriterionError here
        }
        return parsed;
    }

    public ASTValueNode<Object> parseValue(ParsingContext context, Object input) throws BadInputError {
        if (input == null || input == JSONObject.NULL) {
            return new NullValueNode(context);
        }
        if (input instanceof Boolean) {
            return ASTValueNode.castToObject(new BooleanValueNode(context, (boolean) input));
        }
        if (input instanceof Number) {
            return ASTValueNode.castToObject(new NumberValueNode(context, (Number) input));
        }
        if (input instanceof String) {
            return ASTValueNode.castToObject(new StringValueNode(context, (String) input));
        }
        if (input instanceof JSONArray) {
            throw new BadInputError("array values are not accepted");
        }
        if (!(input instanceof JSONObject)) {
            throw new BadInputError(input.getClass().getCanonicalName() + " values are not accepted");
        }
        JSONObject inputObject = (JSONObject) input;
        if (inputObject.length() != 1) {
            throw new BadInputError("object values can only have 1 key defining their type");
        }
        String inputKey = inputObject.keys().next();
        if (inputKey.length() == 0) {
            throw new BadInputError("Bad key \"\"");
        }
        Object inputValue = inputObject.opt(inputKey);
        ASTValueNode<?> parsed = this.parserConfig.valueParser.parseValue(context, inputKey, inputValue);
        if (parsed == null) {
            parsed = new ASTUnknownValueNode(context, inputKey, inputValue);
            // NOTE: I removed the possibility to optionally throw an UnknownValueError here
        }
        return ASTValueNode.castToObject(parsed);
    }

}
