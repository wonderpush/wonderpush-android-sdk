package com.wonderpush.sdk.segmentation;

import android.util.Log;

import com.wonderpush.sdk.JSONUtil;
import com.wonderpush.sdk.TimeSync;
import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.segmentation.parser.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.parser.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.parser.ASTValueNode;
import com.wonderpush.sdk.segmentation.parser.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.parser.DataSourceVisitor;
import com.wonderpush.sdk.segmentation.parser.FieldPath;
import com.wonderpush.sdk.segmentation.parser.criteria.ASTUnknownCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.AllCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.AndCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.AnyCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.ComparisonCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.EqualityCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.GeoCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.InsideCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.JoinCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.LastActivityDateCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.MatchAllCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.NotCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.OrCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.PrefixCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.PresenceCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.SubscriptionStatusCriterionNode;
import com.wonderpush.sdk.segmentation.parser.datasource.EventSource;
import com.wonderpush.sdk.segmentation.parser.datasource.FieldSource;
import com.wonderpush.sdk.segmentation.parser.datasource.GeoDateSource;
import com.wonderpush.sdk.segmentation.parser.datasource.GeoLocationSource;
import com.wonderpush.sdk.segmentation.parser.datasource.InstallationSource;
import com.wonderpush.sdk.segmentation.parser.datasource.LastActivityDateSource;
import com.wonderpush.sdk.segmentation.parser.datasource.PresenceElapsedTimeSource;
import com.wonderpush.sdk.segmentation.parser.datasource.PresenceSinceDateSource;
import com.wonderpush.sdk.segmentation.parser.datasource.UserSource;
import com.wonderpush.sdk.segmentation.parser.value.ASTUnknownValueNode;
import com.wonderpush.sdk.segmentation.parser.value.BooleanValueNode;
import com.wonderpush.sdk.segmentation.parser.value.DateValueNode;
import com.wonderpush.sdk.segmentation.parser.value.DurationValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoBoxValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoCircleValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoLocationValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoPolygonValueNode;
import com.wonderpush.sdk.segmentation.parser.value.NullValueNode;
import com.wonderpush.sdk.segmentation.parser.value.NumberValueNode;
import com.wonderpush.sdk.segmentation.parser.value.RelativeDateValueNode;
import com.wonderpush.sdk.segmentation.parser.value.StringValueNode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

abstract class BaseCriterionVisitor implements ASTValueVisitor<Object>, ASTCriterionVisitor<Boolean>, DataSourceVisitor<List<Object>> {

    public static final String TAG = "WonderPush.Segm.Visitor";

    protected final boolean debug;
    protected final Segmenter.Data data;

    public BaseCriterionVisitor(Segmenter.Data data) {
        this.debug = WonderPush.getLogging();
        this.data = data;
    }

    ///
    /// ASTValueVisitor
    ///

    @Override
    public Object visitASTUnknownValueNode(ASTUnknownValueNode node) {
        Log.w(TAG, "Unsupported unknown value of type " + node.key + " with value " + node.getValue());
        return null;
    }

    @Override
    public Object visitDateValueNode(DateValueNode node) {
        return node.getValue();
    }

    @Override
    public Object visitDurationValueNode(DurationValueNode node) {
        return node.getValue();
    }

    @Override
    public Object visitRelativeDateValueNode(RelativeDateValueNode node) {
        return node.duration.applyTo(TimeSync.getTime());
    }

    @Override
    public Object visitGeoLocationValueNode(GeoLocationValueNode node) {
        Log.w(TAG, "Unsupported " + node.getClass().getSimpleName());
        return null;
    }

    @Override
    public Object visitGeoBoxValueNode(GeoBoxValueNode node) {
        Log.w(TAG, "Unsupported " + node.getClass().getSimpleName());
        return null;
    }

    @Override
    public Object visitGeoCircleValueNode(GeoCircleValueNode node) {
        Log.w(TAG, "Unsupported " + node.getClass().getSimpleName());
        return null;
    }

    @Override
    public Object visitGeoPolygonValueNode(GeoPolygonValueNode node) {
        Log.w(TAG, "Unsupported " + node.getClass().getSimpleName());
        return null;
    }

    @Override
    public Object visitBooleanValueNode(BooleanValueNode node) {
        return node.getValue();
    }

    @Override
    public Object visitNullValueNode(NullValueNode node) {
        return node.getValue();
    }

    @Override
    public Object visitNumberValueNode(NumberValueNode node) {
        return node.getValue();
    }

    @Override
    public Object visitStringValueNode(StringValueNode node) {
        return node.getValue();
    }

    ///
    /// ASTCriterionVisitor
    ///

    @Override
    public Boolean visitMatchAllCriterionNode(MatchAllCriterionNode node) {
        if (debug) Log.d(TAG, "[visitMatchAllCriterionNode] return true");
        return true;
    }

    @Override
    public Boolean visitAndCriterionNode(AndCriterionNode node) {
        for (ASTCriterionNode child : node.children) {
            if (!child.accept(this)) {
                if (debug) Log.d(TAG, "[visitAndCriterionNode] return false because " + child + " is false.");
                return false;
            }
        }
        if (debug) Log.d(TAG, "[visitAndCriterionNode] return true");
        return true;
    }

    @Override
    public Boolean visitOrCriterionNode(OrCriterionNode node) {
        for (ASTCriterionNode child : node.children) {
            if (!child.accept(this)) {
                if (debug) Log.d(TAG, "[visitOrCriterionNode] return true because " + child + " is true.");
                return true;
            }
        }
        if (debug) Log.d(TAG, "[visitOrCriterionNode] return false");
        return false;
    }

    @Override
    public Boolean visitNotCriterionNode(NotCriterionNode node) {
        Boolean rtn = !node.accept(this);
        if (debug) Log.d(TAG, "[visitNotCriterionNode] return " + rtn);
        return rtn;
    }

    @Override
    public Boolean visitGeoCriterionNode(GeoCriterionNode node) {
        Log.w(TAG, "Unsupported " + node.getClass().getSimpleName());
        return false;
    }

    @Override
    public Boolean visitSubscriptionStatusCriterionNode(SubscriptionStatusCriterionNode node) {
        JSONObject pushToken = this.data.installation.optJSONObject("pushToken");
        boolean hasPushToken = pushToken != null && JSONUtil.getString(pushToken, "data") != null;
        JSONObject preferences = this.data.installation.optJSONObject("preferences");
        String preferencesSubscriptionStatus = preferences != null ? JSONUtil.getString(preferences, "subscriptionStatus") : null;
        SubscriptionStatusCriterionNode.SubscriptionStatus status;
        if (!hasPushToken) {
            status = SubscriptionStatusCriterionNode.SubscriptionStatus.optOut;
        } else if ("optOut".equals(preferencesSubscriptionStatus)) {
            status = SubscriptionStatusCriterionNode.SubscriptionStatus.softOptOut;
        } else {
            status = SubscriptionStatusCriterionNode.SubscriptionStatus.optIn;
        }
        return node.subscriptionStatus == status;
    }

    @Override
    public Boolean visitLastActivityDateCriterionNode(LastActivityDateCriterionNode node) {
        if (node.dateComparison == null) {
            return this.data.lastAppOpenDate > 0;
        }
        return node.dateComparison.accept(this);
    }

    @Override
    public Boolean visitPresenceCriterionNode(PresenceCriterionNode node) {
        // Are we present right now?
        boolean present = this.data.presenceInfo == null || this.data.presenceInfo.untilDate >= TimeSync.getTime();
        if (present != node.present) {
            if (debug) Log.d(TAG, "[visitPresenceCriterionNode] return false because presence mismatch, expected " + node.present);
            return false;
        }

        if (node.elapsedTimeComparison != null && !node.elapsedTimeComparison.accept(this)) {
            if (debug) Log.d(TAG, "[visitPresenceCriterionNode] return false because elapsedTime mismatch");
            return false;
        }
        if (node.sinceDateComparison != null && !node.sinceDateComparison.accept(this)) {
            if (debug) Log.d(TAG, "[visitPresenceCriterionNode] return false because sinceDate mismatch");
            return false;
        }
        if (debug) Log.d(TAG, "[visitPresenceCriterionNode] return true");
        return true;
    }

    @Override
    public Boolean visitJoinCriterionNode(JoinCriterionNode node) {
        if (node.context.dataSource instanceof EventSource) {
            for (JSONObject event : this.data.allEvents) {
                EventsVisitor eventsVisitor = new EventsVisitor(data, event);
                if (node.child.accept(eventsVisitor)) {
                    return true;
                }
            }
            return false;
        }
        if (node.context.dataSource instanceof InstallationSource) {
            InstallationVisitor installationVisitor = new InstallationVisitor(data);
            return node.child.accept(installationVisitor);
        }
        Log.w(TAG, "[visitJoinCriterionNode] return false for unsupported " + node.context.dataSource.getClass().getSimpleName());
        return false;
    }

    @Override
    public Boolean visitEqualityCriterionNode(EqualityCriterionNode node) {
        List<Object> dataSourceValues = node.context.dataSource.accept(this);
        Object actualValue = node.value.accept(this);
        boolean result = false;
        for (Object dataSourceValue : dataSourceValues) {
            result = actualValue == null ? dataSourceValue == null : actualValue.equals(dataSourceValue);
            if (result) break;
        }
        if (debug) Log.d(TAG, "[visitEqualityCriterionNode] return " + result + " because " + dataSourceValues + " " + (result ? "==" : "!=") + " " + actualValue);
        return result;
    }

    @Override
    public Boolean visitAnyCriterionNode(AnyCriterionNode node) {
        List<Object> dataSourceValues = node.context.dataSource.accept(this);
        for (ASTValueNode<Object> value : node.values) {
            boolean found = false;
            Object actualValue = value.accept(this);
            for (Object dataSourceValue : dataSourceValues) {
                if (dataSourceValue == null ? actualValue == null : dataSourceValue.equals(actualValue)) {
                    if (debug) Log.d(TAG, "[visitAnyCriterionNode] return true for " + dataSourceValues);
                    return true;
                }
            }
        }
        if (debug) Log.d(TAG, "[visitAnyCriterionNode] return false for " + dataSourceValues);
        return false;
    }

    @Override
    public Boolean visitAllCriterionNode(AllCriterionNode node) {
        List<Object> dataSourceValues = node.context.dataSource.accept(this);
        for (ASTValueNode<Object> value : node.values) {
            boolean found = false;
            Object actualValue = value.accept(this);
            for (Object dataSourceValue : dataSourceValues) {
                if (dataSourceValue == null ? actualValue == null : dataSourceValue.equals(actualValue)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                if (debug) Log.d(TAG, "[visitAllCriterionNode] return false because " + actualValue + " in not contained in " + dataSourceValues);
                return false;
            }
        }
        if (debug) Log.d(TAG, "[visitAllCriterionNode] return true for " + dataSourceValues);
        return true;
    }

    private static int compareObjectsOrThrow(Object a, Object b) throws IllegalArgumentException {
        if (a == JSONObject.NULL) a = null;
        if (b == JSONObject.NULL) b = null;
        if (a == null && b == null) return 0;
        if (a == null) return -compareObjectsOrThrow(b, null);
        // Now a is non null, we'll compare depending on its type, and take an appropriate zero-value for b if it is null
        if (a instanceof Boolean) {
            if (b == null) b = false;
            if (b instanceof Boolean) {
                return ((Boolean) a).compareTo((Boolean) b);
            }
        } else if (a instanceof Number) {
            if (b == null) b = 0;
            if (b instanceof Number) {
                return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
            }
        } else if (a instanceof String) {
            if (b == null) b = "";
            if (b instanceof String) {
                return ((String) a).compareTo((String) b);
            }
        }
        throw new IllegalArgumentException("Cannot compare " + a.getClass().getCanonicalName() + " and " + (b == null ? "null" : b.getClass().getCanonicalName()));
    }

    @Override
    public Boolean visitComparisonCriterionNode(ComparisonCriterionNode node) {
        List<Object> dataSourceValues = node.context.dataSource.accept(this);
        boolean result = false;
        Object actualValue = node.value.accept(this);
        for (Object dataSourceValue : dataSourceValues) {
            try {
                switch (node.comparator) {
                    case gt:
                        result = compareObjectsOrThrow(dataSourceValue, actualValue) > 0;
                        break;
                    case gte:
                        result = compareObjectsOrThrow(dataSourceValue, actualValue) >= 0;
                        break;
                    case lt:
                        result = compareObjectsOrThrow(dataSourceValue, actualValue) < 0;
                        break;
                    case lte:
                        result = compareObjectsOrThrow(dataSourceValue, actualValue) <= 0;
                        break;
                }
            } catch (IllegalArgumentException ignored) {}
            if (result) {
                break;
            }
        }
        if (debug) Log.d(TAG, "[visitComparisonCriterionNode] return " + result + " because " + dataSourceValues + " " + node.comparator.name() + " " + actualValue);
        return result;
    }

    @Override
    public Boolean visitPrefixCriterionNode(PrefixCriterionNode node) {
        List<Object> dataSourceValues = node.context.dataSource.accept(this);
        Object actualValue = node.value.accept(this);
        if (!(actualValue instanceof String)) {
            Log.w(TAG, "[visitPrefixCriterionNode] value " + actualValue + " is not a string");
            return false;
        }
        boolean result = false;
        for (Object dataSourceValue : dataSourceValues) {
            if (!(dataSourceValue instanceof String)) {
                Log.w(TAG, "[visitPrefixCriterionNode] value " + actualValue + " is not a string");
                continue;
            }
            result = ((String) dataSourceValue).startsWith((String) actualValue);
            if (result) break;
        }
        if (debug) Log.d(TAG, "[visitPrefixCriterionNode] return " + result + " because " + dataSourceValues + " " + (result ? "starts with" : "does not start with") + " " + actualValue);
        return result;
    }

    @Override
    public Boolean visitInsideCriterionNode(InsideCriterionNode node) {
        Log.w(TAG, "Unsupported " + node.getClass().getSimpleName());
        return false;
    }

    @Override
    public Boolean visitASTUnknownCriterionNode(ASTUnknownCriterionNode node) {
        Log.w(TAG, "Unsupported unknown criterion " + node.key + " with value " + node.value);
        return false;
    }

    ///
    /// DataSourceVisitor
    ///

    @Override
    public List<Object> visitUserSource(UserSource dataSource) {
        return Collections.emptyList();
    }

    @Override
    public List<Object> visitInstallationSource(InstallationSource dataSource) {
        return Collections.emptyList();
    }

    @Override
    public List<Object> visitEventSource(EventSource dataSource) {
        return Collections.emptyList();
    }

    // public List<Object> visitFieldSource(FieldSource dataSource) {
    //     This method is voluntarily left for subclasses,
    //     which should call visitFieldSourceWithObject with the right object.
    // }

    protected List<Object> visitFieldSourceWithObject(FieldSource dataSource, JSONObject source) {
        FieldPath fieldPath = dataSource.fullPath();
        Object curr = source;
        for (String part : fieldPath.parts) {
            if (curr instanceof JSONObject) {
                curr = ((JSONObject) curr).opt(part);
            } else if (curr instanceof JSONArray) {
                try {
                    int index = Integer.parseInt(part, 10);
                    curr = ((JSONArray) curr).opt(index);
                } catch (NumberFormatException ex) {
                    curr = null;
                }
            } else {
                curr = null;
            }
        }
        if (curr instanceof JSONArray) {
            JSONUtil.JSONArrayToList((JSONArray) curr, Object.class);
        }
        if (curr == null || curr == JSONObject.NULL) return Collections.emptyList();
        return Collections.singletonList(curr);
    }

    @Override
    public List<Object> visitLastActivityDateSource(LastActivityDateSource dataSource) {
        return Collections.singletonList(this.data.lastAppOpenDate);
    }

    @Override
    public List<Object> visitPresenceSinceDateSource(PresenceSinceDateSource dataSource) {
        // Note: with in-apps, if we're running this, we're present.
        if (dataSource.present) {
            // When presence info is missing, assume the user just got here.
            return Collections.singletonList(this.data.presenceInfo == null ? TimeSync.getTime() : this.data.presenceInfo.fromDate);
        }
        // When presence info is missing, assume the user will stay here indefinitely (yay!).
        return Collections.singletonList(this.data.presenceInfo == null ? Long.MAX_VALUE : this.data.presenceInfo.untilDate);
    }

    @Override
    public List<Object> visitPresenceElapsedTimeSource(PresenceElapsedTimeSource dataSource) {
        if (dataSource.present) {
            return Collections.singletonList(this.data.presenceInfo == null ? 0 : Math.max(0, TimeSync.getTime() - this.data.presenceInfo.fromDate));
        }
        return Collections.singletonList(this.data.presenceInfo == null ? 0 : this.data.presenceInfo.elapsedTime);
    }

    @Override
    public List<Object> visitGeoLocationSource(GeoLocationSource dataSource) {
        // TODO Implement geo
        return Collections.emptyList();
    }

    @Override
    public List<Object> visitGeoDateSource(GeoDateSource dataSource) {
        // TODO Implement geo
        return Collections.emptyList();
    }

}
