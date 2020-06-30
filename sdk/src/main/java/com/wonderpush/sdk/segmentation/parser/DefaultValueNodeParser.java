package com.wonderpush.sdk.segmentation.parser;

import com.wonderpush.sdk.segmentation.parser.value.DateValueNode;
import com.wonderpush.sdk.segmentation.parser.value.DurationValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoBoxValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoCircleValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoLocationValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoPolygonValueNode;
import com.wonderpush.sdk.segmentation.parser.value.RelativeDateValueNode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultValueNodeParser extends ConfigurableValueNodeParser {

    private static final Pattern RELATIVE_DATE_PATTERN = Pattern.compile("^[+-]?P");
    private static final Pattern ABSOLUTE_DATE_PATTERN = Pattern.compile("^(\\d\\d\\d\\d(?:-\\d\\d(?:-\\d\\d)?)?)(?:T(\\d\\d(?::\\d\\d(?::\\d\\d(?:.\\d\\d\\d)?)?)?))?(Z|[+-]\\d\\d(?::\\d\\d(?::\\d\\d(?:.\\d\\d\\d)?)?)?)?$");
    private static final Pattern HUMAN_READABLE_DURATION = Pattern.compile("^\\s*([+-]?[0-9.]+(?:[eE][+-]?[0-9]+)?)\\s*([a-zA-Z]*)?\\s*$");

    private static final double NS_TO_MS = 1 / 1e6;
    private static final double US_TO_MS = 1 / 1e3;
    private static final double MS_TO_MS = 1;
    private static final double SECONDS_TO_MS = 1000;
    private static final double MINUTES_TO_MS = 1000 * 60;
    private static final double HOURS_TO_MS = 1000 * 60 * 60;
    private static final double DAYS_TO_MS = 1000 * 60 * 60 * 24;
    private static final double WEEKS_TO_MS = 1000 * 60 * 60 * 24 * 7;
    private enum HumanReadableUnit {
        nanoseconds(NS_TO_MS),
        nanosecond(NS_TO_MS),
        nanos(NS_TO_MS),
        ns(NS_TO_MS),
        microseconds(US_TO_MS),
        microsecond(US_TO_MS),
        micros(US_TO_MS),
        us(US_TO_MS),
        milliseconds(MS_TO_MS),
        millisecond(MS_TO_MS),
        millis(MS_TO_MS),
        ms(MS_TO_MS),
        seconds(SECONDS_TO_MS),
        second(SECONDS_TO_MS),
        secs(SECONDS_TO_MS),
        sec(SECONDS_TO_MS),
        s(SECONDS_TO_MS),
        minutes(MINUTES_TO_MS),
        minute(MINUTES_TO_MS),
        min(MINUTES_TO_MS),
        m(MINUTES_TO_MS),
        hours(HOURS_TO_MS),
        hour(HOURS_TO_MS),
        hr(HOURS_TO_MS),
        h(HOURS_TO_MS),
        days(DAYS_TO_MS),
        day(DAYS_TO_MS),
        d(DAYS_TO_MS),
        weeks(WEEKS_TO_MS),
        week(WEEKS_TO_MS),
        w(WEEKS_TO_MS);

        private final double scaleTowardsMs;

        HumanReadableUnit(double scaleTowardsMs) {
            this.scaleTowardsMs = scaleTowardsMs;
        }

        public double toMilliseconds(double value) {
            return value * this.scaleTowardsMs;
        }
    }

    public DefaultValueNodeParser() throws ValueParserAlreadyExistsForKey {
        super();
        this.registerExactNameParser("date", ungenerify(DefaultValueNodeParser::parseDate));
        this.registerExactNameParser("duration", ungenerify(DefaultValueNodeParser::parseDuration));
        this.registerExactNameParser("geolocation", ungenerify(DefaultValueNodeParser::parseGeolocation));
        this.registerExactNameParser("geobox", ungenerify(DefaultValueNodeParser::parseGeobox));
        this.registerExactNameParser("geocircle", ungenerify(DefaultValueNodeParser::parseGeocircle));
        this.registerExactNameParser("geopolygon", ungenerify(DefaultValueNodeParser::parseGeopolygon));
    }

    private static <T> ASTValueNodeParser<Object> ungenerify(ASTValueNodeParser<T> parser) {
        return (context, key, input) -> ASTValueNode.castToObject(parser.parseValue(context, key, input));
    }

    public static ASTValueNode<Number> parseDate(ParsingContext context, String key, Object input) throws BadInputError {
        if (input instanceof Number) {
            Number numberValue = (Number) input;
            return new DateValueNode(context, numberValue);
        }
        if (input instanceof String) {
            String stringValue = (String) input;
            // Detect relative date
            if (RELATIVE_DATE_PATTERN.matcher(stringValue).lookingAt()) {
                ISO8601Duration duration = ISO8601Duration.parse(stringValue);
                return new RelativeDateValueNode(context, duration);
            }
            // Detect absolute dates
            Matcher matcher = ABSOLUTE_DATE_PATTERN.matcher(stringValue);
            if (matcher.matches()) {
                String date = matcher.group(1);
                String time = matcher.group(2);
                if (time == null) time = "";
                String offset = matcher.group(3);
                if (offset == null) offset = "";
                // Fill parts to default unspecified items
                date = date + "1970-01-01".substring(date.length());
                time = time + "00:00:00.000".substring(time.length());
                if ("Z".equals(offset)) offset = "";
                offset = offset + "+00:00.000".substring(offset.length());
                // Create fully specified date
                String str = date + "T" + time + offset;
                // Parse the date
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ROOT);
                try {
                    Date parsed = sdf.parse(str);
                    return new DateValueNode(context, parsed.getTime());
                } catch (ParseException ex) {
                    throw new BadInputError(key + " value \"" + input + "\" does not parse: " + ex.getMessage());
                }
            }
        }
        throw new BadInputError("\"" + key + "\" values expect a number or a string value");
    }

    public static DurationValueNode parseDuration(ParsingContext context, String key, Object input) throws BadInputError {
        if (input instanceof Number) {
            Number numberValue = (Number) input;
            return new DurationValueNode(context, numberValue);
        }
        if (input instanceof String) {
            String stringValue = (String) input;
            // Detect an ISO8601 duration
            if (RELATIVE_DATE_PATTERN.matcher(stringValue).lookingAt()) {
                ISO8601Duration duration = ISO8601Duration.parse(stringValue);
                return new DurationValueNode(context, duration);
            }
            // Parse a human readable duration
            Matcher matcher = HUMAN_READABLE_DURATION.matcher(stringValue);
            if (matcher.matches()) {
                double value = Double.parseDouble(matcher.group(1));
                String label = matcher.group(2);
                try {
                    HumanReadableUnit unit = HumanReadableUnit.valueOf(label);
                    return new DurationValueNode(context, unit.toMilliseconds(value));
                } catch (IllegalArgumentException ex) {
                    throw new BadInputError("\"" + key + "\" string values expect a valid unit");
                }
            }
        }
        throw new BadInputError("\"" + key + "\" values expect a number or a valid string value");
    }

    public static GeoLocationValueNode parseGeolocation(ParsingContext context, String key, Object input) throws BadInputError {
        if (input instanceof String) {
            String stringInput = (String) input;
            return new GeoLocationValueNode(context, Geohash.parse(stringInput).toGeoLocation());
        }
        if (input instanceof JSONObject) {
            JSONObject objectInput = (JSONObject) input;
            Object latObj = objectInput.opt("lat");
            Object lonObj = objectInput.opt("lon");
            if (latObj instanceof Number && lonObj instanceof Number) {
                Number lat = (Number) latObj;
                Number lon = (Number) lonObj;
                return new GeoLocationValueNode(context, new GeoLocation(lat.doubleValue(), lon.doubleValue()));
            }
        }
        throw new BadInputError("\"" + key + "\" values expect an object with a \"lat\" and \"lon\" numeric fields");
    }

    public static GeoBoxValueNode parseGeobox(ParsingContext context, String key, Object input) throws BadInputError {
        if (input instanceof String) {
            String stringInput = (String) input;
            return new GeoBoxValueNode(context, Geohash.parse(stringInput).toGeoBox());
        }
        if (!(input instanceof JSONObject)) {
            throw new BadInputError("\"" + key + "\" values expect an object");
        }
        JSONObject objectInput = (JSONObject) input;
        if (objectInput.has("topLeft") && objectInput.has("bottomRight")) {
            return new GeoBoxValueNode(context, GeoBox.fromTopLeftAndBottomRight(
                    parseGeolocation(context, "geolocation", objectInput.opt("topLeft")).getValue(),
                    parseGeolocation(context, "geolocation", objectInput.opt("bottomRight")).getValue()
            ));
        }
        if (objectInput.has("topRight") && objectInput.has("bottomLeft")) {
            return new GeoBoxValueNode(context, GeoBox.fromTopRightAndBottomLeft(
                    parseGeolocation(context, "geolocation", objectInput.opt("topRight")).getValue(),
                    parseGeolocation(context, "geolocation", objectInput.opt("bottomLeft")).getValue()
            ));
        }
        Object topObj = objectInput.opt("top");
        Object rightObj = objectInput.opt("right");
        Object bottomObj = objectInput.opt("bottom");
        Object leftObj = objectInput.opt("left");
        if (topObj instanceof Number && rightObj instanceof Number && bottomObj instanceof Number && leftObj instanceof Number) {
            Number top = (Number) topObj;
            Number right = (Number) rightObj;
            Number bottom = (Number) bottomObj;
            Number left = (Number) leftObj;
            return new GeoBoxValueNode(context, GeoBox.fromTopRightBottomLeft(top.doubleValue(), right.doubleValue(), bottom.doubleValue(), left.doubleValue()));
        }
        throw new BadInputError("\"" + key + "\" did not receive an object with a handled format");
    }

    public static GeoCircleValueNode parseGeocircle(ParsingContext context, String key, Object input) throws BadInputError {
        if (!(input instanceof JSONObject)) {
            throw new BadInputError("\"" + key + "\" values expect an object");
        }
        JSONObject objectInput = (JSONObject) input;
        Object radiusObj = objectInput.opt("radius");
        if (!(radiusObj instanceof Number)) {
            throw new BadInputError("\"" + key + "\" needs a radius numeric field");
        }
        Number radius = (Number) radiusObj;
        Object center = objectInput.opt("center");
        if (center == null) {
            throw new BadInputError("\"" + key + "\" did not receive an object with a handled format");
        }
        return new GeoCircleValueNode(context, new GeoCircle(parseGeolocation(context, "geolocation", center).getValue(), radius.doubleValue()));
    }

    public static GeoPolygonValueNode parseGeopolygon(ParsingContext context, String key, Object input) throws BadInputError {
        if (!(input instanceof JSONArray) || ((JSONArray) input).length() < 3) {
            throw new BadInputError("\"" + key + "\" values expect an array of at least 3 geolocations");
        }
        JSONArray arrayInput = (JSONArray) input;
        int length = arrayInput.length();
        List<GeoLocation> points = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            points.add(parseGeolocation(context, "geolocation", arrayInput.opt(i)).getValue());
        }
        return new GeoPolygonValueNode(context, new GeoPolygon(points));
    }

}
