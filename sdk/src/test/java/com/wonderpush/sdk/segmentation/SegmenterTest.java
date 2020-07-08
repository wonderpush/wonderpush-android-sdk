package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.JSONUtil;
import com.wonderpush.sdk.TimeSync;
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
                    null,
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
    public void testItShouldMatchMatchAll() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        Segmenter s = new Segmenter(dataEmpty);
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{}"));
        assertThat(s.matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchFieldFooEqNull() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":null}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[]}"))).matchesInstallation(parsedSegment), is(true));
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

    @Test
    public void testItShouldMatchFieldCustomDateFooEqNumber() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        // 1577836800000 is 2020-01-01T00:00:00.000Z
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".custom.date_foo\":{\"eq\":1577836800000}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":null}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":1}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":false}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"foo\"}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":1577836800000}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2029-09-09T09:09:09.009+09:09\"}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01T01:00:00.000+01:00\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01T00:00:00.000Z\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01T00:00:00.000\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01T00:00:00\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01T00:00\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01T00\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020Z\"}}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchFieldCustomDateFooEqDateString() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".custom.date_foo\":{\"eq\":{\"date\":\"2020-01-01T00:00:00.000Z\"}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":null}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":1}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":false}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"foo\"}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":1577836800000}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2029-09-09T09:09:09.009+09:09\"}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01T01:00:00.000+01:00\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01T00:00:00.000Z\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01T00:00:00.000\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01T00:00:00\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01T00:00\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01T00\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01-01\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020-01\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"custom\":{\"date_foo\":\"2020Z\"}}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchFieldFooComparisonLong() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"gt\":9223372036854775806}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":9223372036854775805}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":9223372036854775806}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":9223372036854775807}"))).matchesInstallation(parsedSegment), is(true));
        // When comparing a long with a double, we loose some precision, it's OK
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":9.223372036854775808e18}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":9.223372036854777000e18}"))).matchesInstallation(parsedSegment), is(true));
        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"lt\":9.223372036854775808e18}}"));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":9223372036854775805}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchFieldFooComparisonIntegers() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment;

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"lt\":0}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":-1}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"lte\":0}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":-1}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"gt\":0}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":-1}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"gte\":0}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":-1}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchFieldFooComparisonFloats() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment;

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"lt\":1.5}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.2}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.5}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.7}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"lte\":1.5}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.2}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.5}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.7}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"gt\":1.5}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.2}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.5}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.7}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"gte\":1.5}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.2}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.5}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1.7}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchFieldFooComparisonStrings() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment;

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"lt\":\"mm\"}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"m\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"ma\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"MM\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"mm\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"mma\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"mz\"}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"lte\":\"mm\"}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"m\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"ma\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"MM\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"mm\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"mma\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"mz\"}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"gt\":\"mm\"}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"m\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"ma\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"MM\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"mm\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"mma\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"mz\"}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"gte\":\"mm\"}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"m\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"ma\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"MM\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"mm\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"mma\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"mz\"}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchFieldFooComparisonMixed() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment;

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"lt\":0}}"));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"lte\":0}}"));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"gt\":0}}"));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"gte\":0}}"));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchFieldFooComparisonBooleans() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment;

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"lt\":true}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":true}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"lte\":true}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":true}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"gt\":true}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":true}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"gte\":true}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":true}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"lt\":false}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":true}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"lte\":false}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":true}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"gt\":false}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":true}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"gte\":false}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":true}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchEventTypeTest() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"event\":{\".type\":{\"eq\":\"test\"}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithNewerEvent(dataEmpty, new JSONObject("{\"type\":\"@APP_OPEN\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithNewerEvent(dataEmpty, new JSONObject("{\"type\":\"test\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithNewerEvent(dataWithNewerEvent(dataEmpty, new JSONObject("{\"type\":\"@APP_OPEN\"}")), new JSONObject("{\"type\":\"test\"}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchInstallation() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".bar\":{\".sub\":{\"eq\":\"sub\"},\"installation\":{\".foo\":{\"eq\":\"foo\"}}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"bar\":{\"sub\":\"sub\"}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\",\"bar\":{\"sub\":\"sub\"}}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchEventInstallation() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"event\":{\".type\":{\"eq\":\"test\"},\"installation\":{\".foo\":{\"eq\":\"foo\"}}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithNewerEvent(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}")), new JSONObject("{\"type\":\"nope\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithNewerEvent(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}")), new JSONObject("{\"type\":\"test\"}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchUser() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"user\":{}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchAnd() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment;

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"eq\":\"foo\"},\".bar\":{\"eq\":\"bar\"}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"bar\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\",\"bar\":\"bar\"}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"and\":[{\".foo\":{\"eq\":\"foo\"}},{\".bar\":{\"eq\":\"bar\"}}]}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"bar\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\",\"bar\":\"bar\"}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchOr() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"or\":[{\".foo\":{\"eq\":\"foo\"}},{\".bar\":{\"eq\":\"bar\"}}]}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"to\":\"to\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"bar\":\"bar\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\",\"bar\":\"bar\"}"))).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchNot() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"not\":{\".foo\":{\"eq\":\"foo\"}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"bar\":\"bar\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\",\"bar\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldNotMatchUnknownCriterion() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"unknown criterion\":{}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchSubscriptionStatus() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment;

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"subscriptionStatus\":\"optOut\"}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":null}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":null}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":\"FAKE\"}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":\"FAKE\"},\"preferences\":{\"subscriptionStatus\":null}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":\"FAKE\"},\"preferences\":{\"subscriptionStatus\":\"optIn\"}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":\"FAKE\"},\"preferences\":{\"subscriptionStatus\":\"optOut\"}}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"subscriptionStatus\":\"softOptOut\"}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":null}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":\"FAKE\"}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":\"FAKE\"},\"preferences\":{\"subscriptionStatus\":null}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":\"FAKE\"},\"preferences\":{\"subscriptionStatus\":\"optIn\"}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":\"FAKE\"},\"preferences\":{\"subscriptionStatus\":\"optOut\"}}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"subscriptionStatus\":\"optIn\"}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":null}}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":\"FAKE\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":\"FAKE\"},\"preferences\":{\"subscriptionStatus\":null}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":\"FAKE\"},\"preferences\":{\"subscriptionStatus\":\"optIn\"}}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"pushToken\":{\"data\":\"FAKE\"},\"preferences\":{\"subscriptionStatus\":\"optOut\"}}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchLastActivityDate() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment;

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"lastActivityDate\":{\"gt\":1000000000000}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithLastAppOpenDate(dataEmpty, 999999999999L)).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithLastAppOpenDate(dataEmpty, 1000000000000L)).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithLastAppOpenDate(dataEmpty, 1000000000001L)).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"lastActivityDate\":{\"gt\":{\"date\":\"-PT1M\"}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithLastAppOpenDate(dataEmpty, TimeSync.getTime())).matchesInstallation(parsedSegment), is(true));
    }

    @Test
    public void testItShouldMatchPresence() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        long now = TimeSync.getTime();
        ASTCriterionNode parsedSegment;

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"presence\":{\"present\":false}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false)); // no info is considered present since just about now
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 60000, now + 60000, 120000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 120000, now - 60000, 60000))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now + 60000, now + 120000, 60000))).matchesInstallation(parsedSegment), is(true)); // not present yet, so not present

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"presence\":{\"present\":true}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(true)); // no info is considered present since just about now
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 60000, now + 60000, 120000))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 120000, now - 60000, 60000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now + 60000, now + 120000, 60000))).matchesInstallation(parsedSegment), is(false)); // not present yet, so not present

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"presence\":{\"present\":false,\"elapsedTime\":{\"gt\":1000}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false)); // no info is considered present since just about now
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 60000, now + 60000, 120000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 120000, now - 60000, 60000))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now + 60000, now + 120000, 60000))).matchesInstallation(parsedSegment), is(true)); // not present yet, so not present, and it will last 60s, so we pass

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"presence\":{\"present\":true,\"elapsedTime\":{\"gt\":1000}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false)); // no info is considered present since just about now
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 60000, now + 60000, 120000))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 120000, now - 60000, 60000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now + 60000, now + 120000, 60000))).matchesInstallation(parsedSegment), is(false)); // not present yet, so not present

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"presence\":{\"present\":false,\"elapsedTime\":{\"lt\":1000}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false)); // no info is considered present since just about now
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 60000, now + 60000, 120000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 120000, now - 60000, 60000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now + 60000, now + 120000, 60000))).matchesInstallation(parsedSegment), is(false)); // not present yet, so not present

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"presence\":{\"present\":true,\"elapsedTime\":{\"lt\":1000}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(true)); // no info is considered present since just about now
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 60000, now + 60000, 120000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 120000, now - 60000, 60000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now + 60000, now + 120000, 60000))).matchesInstallation(parsedSegment), is(false)); // not present yet, so not present

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"presence\":{\"present\":false,\"sinceDate\":{\"lte\":{\"date\":\"-PT1M\"}}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false)); // no info is considered present since just about now
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 120000, now - 100000, 20000))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 60000, now - 30000, 30000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 60000, now + 60000, 120000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now + 60000, now + 120000, 60000))).matchesInstallation(parsedSegment), is(false)); // not present yet, but leave date is not lte -PT1M

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"presence\":{\"present\":true,\"sinceDate\":{\"lte\":{\"date\":\"-PT1M\"}}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false)); // no info is considered present since just about now
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 120000, now - 100000, 20000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 60000, now - 30000, 30000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 30000, now + 60000, 90000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 120000, now + 60000, 180000))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now + 60000, now + 120000, 60000))).matchesInstallation(parsedSegment), is(false)); // not present yet, so not present

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"presence\":{\"present\":false,\"sinceDate\":{\"gte\":{\"date\":\"-PT1M\"}}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false)); // no info is considered present since just about now
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 120000, now - 100000, 20000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 60000, now - 30000, 30000))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 60000, now + 60000, 120000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now + 120000, now + 180000, 60000))).matchesInstallation(parsedSegment), is(true)); // not present yet, and leave date is gte -PT1M

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\"presence\":{\"present\":true,\"sinceDate\":{\"gte\":{\"date\":\"-PT1M\"}}}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(true)); // no info is considered present since just about now
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 120000, now - 100000, 20000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 60000, now - 30000, 30000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 30000, now + 60000, 90000))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now - 120000, now + 60000, 180000))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithPresenceInfo(dataEmpty, new Segmenter.PresenceInfo(now + 60000, now + 120000, 60000))).matchesInstallation(parsedSegment), is(false)); // not present yet, but leave date is gte -PT1M
    }

    @Test
    public void testItShouldMatchPrefix() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment;

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"prefix\":\"fo\"}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"fo\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"f\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"FOO\"}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"prefix\":\"fo\"}}"));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchAny() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment;

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"any\":[]}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"any\":[1]}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,\"foo\"]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1,\"foo\"]}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"any\":[1,\"foo\"]}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,\"foo\"]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1,\"foo\"]}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"any\":[1,null]}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,null]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1,null]}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"any\":[null]}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,null]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1,null]}"))).matchesInstallation(parsedSegment), is(false));
    }

    @Test
    public void testItShouldMatchAll() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsedSegment;

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"all\":[]}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"all\":[1]}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,\"foo\"]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1,\"foo\"]}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"all\":[1,\"foo\"]}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,\"foo\"]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1,\"foo\"]}"))).matchesInstallation(parsedSegment), is(true));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"all\":[1,null]}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,null]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1,null]}"))).matchesInstallation(parsedSegment), is(false));

        parsedSegment = Segmenter.parseInstallationSegment(new JSONObject("{\".foo\":{\"all\":[null]}}"));
        assertThat(new Segmenter(dataEmpty).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"bar\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":\"foo\"}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":null}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":false}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":0}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":1}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[]}"))).matchesInstallation(parsedSegment), is(true));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,null]}"))).matchesInstallation(parsedSegment), is(false));
        assertThat(new Segmenter(dataWithInstallation(dataEmpty, new JSONObject("{\"foo\":[\"bar\",false,1,null]}"))).matchesInstallation(parsedSegment), is(false));
    }

}
