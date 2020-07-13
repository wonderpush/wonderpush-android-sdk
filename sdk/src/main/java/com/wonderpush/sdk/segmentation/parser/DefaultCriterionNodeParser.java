package com.wonderpush.sdk.segmentation.parser;

import com.wonderpush.sdk.segmentation.parser.criteria.AllCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.AndCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.AnyCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.ComparisonCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.EqualityCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.GeoCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.InsideCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.JoinCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.LastActivityDateCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.NotCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.OrCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.PrefixCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.PresenceCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.SubscriptionStatusCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.UnknownCriterionError;
import com.wonderpush.sdk.segmentation.parser.datasource.EventSource;
import com.wonderpush.sdk.segmentation.parser.datasource.FieldSource;
import com.wonderpush.sdk.segmentation.parser.datasource.GeoDateSource;
import com.wonderpush.sdk.segmentation.parser.datasource.GeoLocationSource;
import com.wonderpush.sdk.segmentation.parser.datasource.InstallationSource;
import com.wonderpush.sdk.segmentation.parser.datasource.LastActivityDateSource;
import com.wonderpush.sdk.segmentation.parser.datasource.PresenceElapsedTimeSource;
import com.wonderpush.sdk.segmentation.parser.datasource.PresenceSinceDateSource;
import com.wonderpush.sdk.segmentation.parser.datasource.UserSource;
import com.wonderpush.sdk.segmentation.parser.value.GeoAbstractAreaValueNode;
import com.wonderpush.sdk.segmentation.parser.value.StringValueNode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DefaultCriterionNodeParser extends ConfigurableCriterionNodeParser {

    public DefaultCriterionNodeParser() throws CriterionParserAlreadyExistsForKey {
        super();
        // Dynamic: Field access
        this.registerDynamicNameParser(DefaultCriterionNodeParser::parseDynamicDotField);
        // Generic combiners
        this.registerExactNameParser("and", DefaultCriterionNodeParser::parseAnd);
        this.registerExactNameParser("or", DefaultCriterionNodeParser::parseOr);
        this.registerExactNameParser("not", DefaultCriterionNodeParser::parseNot);
        // Available only on installation data sources
        this.registerExactNameParser("lastActivityDate", DefaultCriterionNodeParser::parseLastActivityDate);
        this.registerExactNameParser("presence", DefaultCriterionNodeParser::parsePresence);
        this.registerExactNameParser("geo", DefaultCriterionNodeParser::parseGeo);
        this.registerExactNameParser("subscriptionStatus", DefaultCriterionNodeParser::parseSubscriptionStatus);
        this.registerExactNameParser("user", DefaultCriterionNodeParser::parseUser);
        this.registerExactNameParser("installation", DefaultCriterionNodeParser::parseInstallation);
        this.registerExactNameParser("event", DefaultCriterionNodeParser::parseEvent);
        // Available only on non-root data source
        this.registerExactNameParser("eq", DefaultCriterionNodeParser::parseEq);
        this.registerExactNameParser("any", DefaultCriterionNodeParser::parseAny);
        this.registerExactNameParser("all", DefaultCriterionNodeParser::parseAll);
        this.registerExactNameParser("gt", DefaultCriterionNodeParser::parseGt);
        this.registerExactNameParser("gte", DefaultCriterionNodeParser::parseGte);
        this.registerExactNameParser("lt", DefaultCriterionNodeParser::parseLt);
        this.registerExactNameParser("lte", DefaultCriterionNodeParser::parseLte);
        this.registerExactNameParser("prefix", DefaultCriterionNodeParser::parsePrefix);
        this.registerExactNameParser("inside", DefaultCriterionNodeParser::parseInside);
    }

    public static ASTCriterionNode parseDynamicDotField(ParsingContext context, String key, Object input) throws BadInputError, UnknownCriterionError, UnknownValueError {
        if (key.charAt(0) == '.') {
            FieldSource fieldSource = new FieldSource(context.dataSource, FieldPath.parse(key.substring(1)));
            ParsingContext newContext = context.withDataSource(fieldSource);
            return context.parser.parseCriterion(newContext, ensureObject(key, input));
        }
        return null;
    }

    public static AndCriterionNode parseAnd(ParsingContext context, String key, Object input) throws BadInputError, UnknownCriterionError, UnknownValueError {
        List<JSONObject> checkedInputValue = ensureArrayOfObjects(key, input);
        List<ASTCriterionNode> parsed = new ArrayList<>(checkedInputValue.size());
        for (JSONObject inputItem : checkedInputValue) {
            parsed.add(context.parser.parseCriterion(context, inputItem));
        }
        return new AndCriterionNode(context, parsed);
    }

    public static OrCriterionNode parseOr(ParsingContext context, String key, Object input) throws BadInputError, UnknownCriterionError, UnknownValueError {
        List<JSONObject> checkedInputValue = ensureArrayOfObjects(key, input);
        List<ASTCriterionNode> parsed = new ArrayList<>(checkedInputValue.size());
        for (JSONObject inputItem : checkedInputValue) {
            parsed.add(context.parser.parseCriterion(context, inputItem));
        }
        return new OrCriterionNode(context, parsed);
    }

    public static NotCriterionNode parseNot(ParsingContext context, String key, Object input) throws BadInputError, UnknownCriterionError, UnknownValueError {
        JSONObject checkedInputValue = ensureObject(key, input);
        return new NotCriterionNode(context, context.parser.parseCriterion(context, checkedInputValue));
    }

    public static LastActivityDateCriterionNode parseLastActivityDate(ParsingContext context, String key, Object input) throws BadInputError, UnknownCriterionError, UnknownValueError {
        if (!(context.dataSource instanceof InstallationSource)) {
            throw new BadInputError("\"" + key + "\" is only supported in the context of \"installation\"");
        }
        InstallationSource contextDataSource = (InstallationSource) context.dataSource;
        JSONObject checkedInputValue = ensureObject(key, input);
        DataSource dataSource = new LastActivityDateSource(contextDataSource);
        ASTCriterionNode dateCriterion = context.parser.parseCriterion(context.withDataSource(dataSource), checkedInputValue);
        return new LastActivityDateCriterionNode(context, dateCriterion);
    }

    public static PresenceCriterionNode parsePresence(ParsingContext context, String key, Object input) throws BadInputError, UnknownCriterionError, UnknownValueError {
        if (!(context.dataSource instanceof InstallationSource)) {
            throw new BadInputError("\"" + key + "\" is only supported in the context of \"installation\"");
        }
        InstallationSource contextDataSource = (InstallationSource) context.dataSource;
        JSONObject checkedInputValue = ensureObject(key, input);
        Boolean present = ensureBoolean(key + ".present", checkedInputValue.opt("present"));
        PresenceSinceDateSource sinceDateSource = new PresenceSinceDateSource(contextDataSource, present);
        ASTCriterionNode sinceDateCriterion = checkedInputValue.has("sinceDate") ? context.parser.parseCriterion(context.withDataSource(sinceDateSource), ensureObject(key + ".sinceDate", checkedInputValue.opt("sinceDate"))) : null;
        PresenceElapsedTimeSource elapsedTimeSource = new PresenceElapsedTimeSource(contextDataSource, present);
        ASTCriterionNode elapsedTimeCriterion = checkedInputValue.has("elapsedTime") ? context.parser.parseCriterion(context.withDataSource(elapsedTimeSource), ensureObject(key + ".elapsedTime", checkedInputValue.opt("elapsedTime"))) : null;
        return new PresenceCriterionNode(context, present, sinceDateCriterion, elapsedTimeCriterion);
    }

    public static GeoCriterionNode parseGeo(ParsingContext context, String key, Object input) throws BadInputError, UnknownCriterionError, UnknownValueError {
        if (!(context.dataSource instanceof InstallationSource)) {
            throw new BadInputError("\"" + key + "\" is only supported in the context of \"installation\"");
        }
        InstallationSource contextDataSource = (InstallationSource) context.dataSource;
        JSONObject checkedInputValue = ensureObject(key, input);
        GeoLocationSource geoLocationSource = new GeoLocationSource(contextDataSource);
        ASTCriterionNode geoLocationCriterion = checkedInputValue.has("location") ? context.parser.parseCriterion(context.withDataSource(geoLocationSource), ensureObject(key + ".location", checkedInputValue.opt("location"))) : null;
        GeoDateSource dateSource = new GeoDateSource(contextDataSource);
        ASTCriterionNode dateCriterion = checkedInputValue.has("date") ? context.parser.parseCriterion(context.withDataSource(dateSource), ensureObject(key + ".date", checkedInputValue.opt("date"))) : null;
        return new GeoCriterionNode(context, geoLocationCriterion, dateCriterion);
    }

    public static SubscriptionStatusCriterionNode parseSubscriptionStatus(ParsingContext context, String key, Object input) throws BadInputError {
        if (!(context.dataSource instanceof InstallationSource)) {
            throw new BadInputError("\"" + key + "\" is only supported in the context of \"installation\"");
        }
        InstallationSource contextDataSource = (InstallationSource) context.dataSource;
        String checkedInputValue = ensureString(key, input);
        SubscriptionStatusCriterionNode.SubscriptionStatus subscriptionStatus;
        try {
            subscriptionStatus = SubscriptionStatusCriterionNode.SubscriptionStatus.valueOf(checkedInputValue);
        } catch (IllegalArgumentException ex) {
            throw new BadInputError("\"" + key + "\" must be one of \"optIn\", \"optOut\" or \"softOptOut\"");
        }
        return new SubscriptionStatusCriterionNode(context, subscriptionStatus);
    }

    public static ASTCriterionNode parseUser(ParsingContext context, String key, Object input) throws BadInputError, UnknownCriterionError, UnknownValueError {
        DataSource rootDataSource = context.dataSource.getRootDataSource();
        JSONObject checkedInputValue = ensureObject(key, input);
        ParsingContext newContext = context.withDataSource(new UserSource());
        if (rootDataSource instanceof UserSource) {
            return context.parser.parseCriterion(newContext, checkedInputValue);
        } else if (rootDataSource instanceof InstallationSource) {
            return new JoinCriterionNode(newContext, context.parser.parseCriterion(newContext, checkedInputValue));
        } else if (rootDataSource instanceof EventSource) {
            ParsingContext oneHopContext = context.withDataSource(new InstallationSource());
            ParsingContext twoHopsContext = oneHopContext.withDataSource(new UserSource());
            return new JoinCriterionNode(oneHopContext, new JoinCriterionNode(twoHopsContext, context.parser.parseCriterion(twoHopsContext, checkedInputValue)));
        }
        throw new BadInputError("\"" + key + "\" is not supported in this context");
    }

    public static ASTCriterionNode parseInstallation(ParsingContext context, String key, Object input) throws BadInputError, UnknownCriterionError, UnknownValueError {
        DataSource rootDataSource = context.dataSource.getRootDataSource();
        JSONObject checkedInputValue = ensureObject(key, input);
        ParsingContext newContext = context.withDataSource(new InstallationSource());
        if (rootDataSource instanceof UserSource || rootDataSource instanceof EventSource) {
            return new JoinCriterionNode(newContext, context.parser.parseCriterion(newContext, checkedInputValue));
        } else if (rootDataSource instanceof InstallationSource) {
            return context.parser.parseCriterion(newContext, checkedInputValue);
        }
        throw new BadInputError("\"" + key + "\" is not supported in this context");
    }

    public static ASTCriterionNode parseEvent(ParsingContext context, String key, Object input) throws BadInputError, UnknownCriterionError, UnknownValueError {
        DataSource rootDataSource = context.dataSource.getRootDataSource();
        JSONObject checkedInputValue = ensureObject(key, input);
        ParsingContext newContext = context.withDataSource(new EventSource());
        if (rootDataSource instanceof UserSource) {
            ParsingContext oneHopContext = context.withDataSource(new InstallationSource());
            ParsingContext twoHopsContext = oneHopContext.withDataSource(new EventSource());
            return new JoinCriterionNode(oneHopContext, new JoinCriterionNode(twoHopsContext, context.parser.parseCriterion(twoHopsContext, checkedInputValue)));
        } else if (rootDataSource instanceof InstallationSource) {
            return new JoinCriterionNode(newContext, context.parser.parseCriterion(newContext, checkedInputValue));
        } else if (rootDataSource instanceof EventSource) {
            return context.parser.parseCriterion(newContext, checkedInputValue);
        }
        throw new BadInputError("\"" + key + "\" is not supported in this context");
    }

    public static EqualityCriterionNode parseEq(ParsingContext context, String key, Object input) throws BadInputError, UnknownValueError {
        if (context.dataSource.getRootDataSource() == context.dataSource) {
            throw new BadInputError("\"" + key + "\" is only supported in the context of a field");
        }
        return new EqualityCriterionNode(context, context.parser.parseValue(context, input));
    }

    public static AnyCriterionNode parseAny(ParsingContext context, String key, Object input) throws BadInputError, UnknownValueError {
        if (context.dataSource.getRootDataSource() == context.dataSource) {
            throw new BadInputError("\"" + key + "\" is only supported in the context of a field");
        }
        JSONArray checkedInputValue = ensureArray(key, input);
        int length  = checkedInputValue.length();
        List<ASTValueNode<Object>> values = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            values.add(context.parser.parseValue(context, checkedInputValue.opt(i)));
        }
        return new AnyCriterionNode(context, values);
    }

    public static AllCriterionNode parseAll(ParsingContext context, String key, Object input) throws BadInputError, UnknownValueError {
        if (context.dataSource.getRootDataSource() == context.dataSource) {
            throw new BadInputError("\"" + key + "\" is only supported in the context of a field");
        }
        JSONArray checkedInputValue = ensureArray(key, input);
        int length  = checkedInputValue.length();
        List<ASTValueNode<Object>> values = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            values.add(context.parser.parseValue(context, checkedInputValue.opt(i)));
        }
        return new AllCriterionNode(context, values);
    }

    public static ComparisonCriterionNode parseGtGteLtLte(ParsingContext context, String key, Object input, ComparisonCriterionNode.Comparator comparator) throws BadInputError, UnknownValueError {
        if (context.dataSource.getRootDataSource() == context.dataSource) {
            throw new BadInputError("\"" + key + "\" is only supported in the context of a field");
        }
        return new ComparisonCriterionNode(context, comparator, context.parser.parseValue(context, input));
    }

    public static ComparisonCriterionNode parseGt(ParsingContext context, String key, Object input) throws BadInputError, UnknownValueError {
        return parseGtGteLtLte(context, key, input, ComparisonCriterionNode.Comparator.gt);
    }

    public static ComparisonCriterionNode parseGte(ParsingContext context, String key, Object input) throws BadInputError, UnknownValueError {
        return parseGtGteLtLte(context, key, input, ComparisonCriterionNode.Comparator.gte);
    }

    public static ComparisonCriterionNode parseLt(ParsingContext context, String key, Object input) throws BadInputError, UnknownValueError {
        return parseGtGteLtLte(context, key, input, ComparisonCriterionNode.Comparator.lt);
    }

    public static ComparisonCriterionNode parseLte(ParsingContext context, String key, Object input) throws BadInputError, UnknownValueError {
        return parseGtGteLtLte(context, key, input, ComparisonCriterionNode.Comparator.lte);
    }

    public static PrefixCriterionNode parsePrefix(ParsingContext context, String key, Object input) throws BadInputError, UnknownValueError {
        if (context.dataSource.getRootDataSource() == context.dataSource) {
            throw new BadInputError("\"" + key + "\" is only supported in the context of a field");
        }
        ASTValueNode<?> value = context.parser.parseValue(context, input);
        if (!(value instanceof StringValueNode)) {
            throw new BadInputError("\"" + key + "\" expects a string value");
        }
        return new PrefixCriterionNode(context, (StringValueNode) value);
    }

    public static InsideCriterionNode parseInside(ParsingContext context, String key, Object input) throws BadInputError, UnknownValueError {
        if (context.dataSource.getRootDataSource() == context.dataSource) {
            throw new BadInputError("\"" + key + "\" is only supported in the context of a field");
        }
        ASTValueNode<?> value = context.parser.parseValue(context, input);
        if (!(value instanceof GeoAbstractAreaValueNode<?>)) {
            throw new BadInputError("\"" + key + "\" expects a compatible geo value");
        }
        return new InsideCriterionNode(context, (GeoAbstractAreaValueNode<?>) value);
    }

    private static JSONArray ensureArray(String key, Object input) throws BadInputError {
        if (!(input instanceof JSONArray)) {
            throw new BadInputError("\"" + key + "\" expects an array");
        }
        return (JSONArray) input;
    }

    private static List<JSONObject> ensureArrayOfObjects(String key, Object input) throws BadInputError {
        JSONArray jsonArray = ensureArray(key, input);
        int length = jsonArray.length();
        List<JSONObject> rtn = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            Object item = jsonArray.opt(i);
            if (!(item instanceof JSONObject)) {
                throw new BadInputError("\"" + key + "\" expects an array of objects");
            }
            rtn.add((JSONObject) item);
        }
        return rtn;
    }

    private static JSONObject ensureObject(String key, Object input) throws BadInputError {
        if (!(input instanceof JSONObject)) {
            throw new BadInputError("\"" + key + "\" expects an object");
        }
        return (JSONObject) input;
    }

    private static String ensureString(String key, Object input) throws BadInputError {
        if (!(input instanceof String)) {
            throw new BadInputError("\"" + key + "\" expects a string");
        }
        return (String) input;
    }

    private static Boolean ensureBoolean(String key, Object input) throws BadInputError {
        if (!(input instanceof Boolean)) {
            throw new BadInputError("\"" + key + "\" expects a boolean");
        }
        return (Boolean) input;
    }

}
