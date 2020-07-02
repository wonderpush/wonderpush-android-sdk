package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.JSONUtil;
import com.wonderpush.sdk.segmentation.parser.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.parser.BadInputError;
import com.wonderpush.sdk.segmentation.parser.UnknownValueError;
import com.wonderpush.sdk.segmentation.parser.criteria.UnknownCriterionError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SegmenterTest {

    public static Segmenter.Data dataEmpty;

    static {
        try {
            dataEmpty = new Segmenter.Data(
                    new JSONObject("{}"),
                    Collections.emptyList(),
                    new Segmenter.PresenceInfo(0L, 0L, 0),
                    0L
            );
        } catch (JSONException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public static Segmenter.Data dataWithInstallation(Segmenter.Data data, JSONObject installation) {
        return new Segmenter.Data(
                installation,
                data.allEvents,
                data.presenceInfo,
                data.lastAppOpenDate
        );
    }

    public static Segmenter.Data dataWithAllEvents(Segmenter.Data data, List<JSONObject> allEvents) {
        return new Segmenter.Data(
                data.installation,
                allEvents,
                data.presenceInfo,
                data.lastAppOpenDate
        );
    }

    public static Segmenter.Data dataWithPresenceInfo(Segmenter.Data data, Segmenter.PresenceInfo presenceInfo) {
        return new Segmenter.Data(
                data.installation,
                data.allEvents,
                presenceInfo,
                data.lastAppOpenDate
        );
    }

    public static Segmenter.Data dataWithLastAppOpenDate(Segmenter.Data data, long lastAppOpenDate) {
        return new Segmenter.Data(
                data.installation,
                data.allEvents,
                data.presenceInfo,
                lastAppOpenDate
        );
    }

    public static Segmenter.Data dataWithInstallationDiff(Segmenter.Data data, JSONObject installationDiff, boolean nullFieldRemoves) {
        try {
            JSONObject installation = new JSONObject(data.installation.toString());
            JSONUtil.merge(installation, installationDiff, nullFieldRemoves);
            return dataWithInstallation(data, installation);
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Segmenter.Data dataWithNewerEvent(Segmenter.Data data, JSONObject newerEvent) {
        List<JSONObject> allEvents = new ArrayList<>(data.allEvents.size() + 1);
        String newerEventType = JSONUtil.getString(newerEvent, "type");
        if (newerEventType == null) throw new RuntimeException("missing event type");
        for (JSONObject event : data.allEvents) {
            String eventType = JSONUtil.getString(event, "type");
            if (!newerEventType.equals(eventType)) {
                allEvents.add(event);
            }
        }
        allEvents.add(newerEvent);
        return dataWithAllEvents(data, allEvents);
    }

    public static Segmenter.Data dataWithTags(Segmenter.Data data, Set<String> tags) {
        try {
            JSONObject installation = new JSONObject(data.installation.toString());
            JSONObject custom = installation.optJSONObject("custom");
            if (custom == null) {
                custom = new JSONObject();
                installation.put("custom", custom);
            }
            custom.put("tags", new JSONArray(tags));
            return new Segmenter.Data(
                    installation,
                    data.allEvents,
                    data.presenceInfo,
                    data.lastAppOpenDate
            );
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testItShouldMatchAll() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        Segmenter s = new Segmenter(dataEmpty);
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{}"));
        assertThat(s.matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchFieldFooEqNull() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":null}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[null,null]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",null]}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchFieldFooEqFalse() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":false}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":true}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",false]}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchFieldFooEqTrue() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":true}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":true}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",false]}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchFieldFooEq0() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":0}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0.0}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[1,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchFieldFooEq00() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":0.0}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0.0}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[1,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchFieldFooEq1() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":1}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0.0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.0}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[1,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchFieldFooEq10() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":1.0}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0.0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.0}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[1,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchFieldFooEqDecimal() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":1.5}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0.0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.2}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.5}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.7}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":2.0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":2}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[1.5,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchFieldFooEqLong() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":9223372036854775807}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":9223372036854775806}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":9223372036854775807}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":9223372036854775808}"))).matchesInstallation(parsedSegment), is(false));
        // When comparing a long with a double, we loose some precision, it's OK
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":9.223372036854775806e18}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":9.223372036854775807e18}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":9.223372036854775808e18}"))).matchesInstallation(parsedSegment), is(true));
        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":7.000000000000000512e18}}"));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":6999999999999999487}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":6999999999999999488}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":7000000000000000001}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":7000000000000000512}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":7000000000000000513}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchFieldFooEqBigDecimal() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":1e300}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0.0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1e300}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.0e300}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[1e300,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchFieldFooEqBar() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":\"bar\"}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchFieldFooEqEmptystring() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":\"\"}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"bar\",true]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[0,\"\",true]}"))).matchesInstallation(parsedSegment), is(true));
    }

    // TODO Test string dates inside event/installation

}
