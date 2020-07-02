package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.parser.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.parser.BadInputError;
import com.wonderpush.sdk.segmentation.parser.SegmentationFactory;
import com.wonderpush.sdk.segmentation.parser.UnknownValueError;
import com.wonderpush.sdk.segmentation.parser.criteria.UnknownCriterionError;
import com.wonderpush.sdk.segmentation.parser.datasource.InstallationSource;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Segmenter {

    public static class PresenceInfo {
        public final long fromDate;
        public final long untilDate;
        public final long elapsedTime;

        public PresenceInfo(long fromDate, long untilDate, long elapsedTime) {
            this.fromDate = fromDate;
            this.untilDate = untilDate;
            this.elapsedTime = elapsedTime;
        }
    }

    public static class Data {
        public final JSONObject installation;
        public final List<JSONObject> allEvents;
        public final PresenceInfo presenceInfo;
        public final long lastAppOpenDate;

        public Data(JSONObject installation, List<JSONObject> allEvents, PresenceInfo presenceInfo, long lastAppOpenDate) {
            this.installation = installation;
            this.allEvents = Collections.unmodifiableList(new ArrayList<>(allEvents));
            this.presenceInfo = presenceInfo;
            this.lastAppOpenDate = lastAppOpenDate;
        }
    }

    protected Data data;

    public Segmenter(Data data) {
        this.data = data;
    }

    public static ASTCriterionNode parseInstallationSegment(JSONObject segmentInput) throws BadInputError, UnknownValueError, UnknownCriterionError {
        return SegmentationFactory.getDefaultParser().parse(segmentInput, new InstallationSource());
    }

    public boolean matchesInstallation(ASTCriterionNode parsedInstallationSegment) {
        return parsedInstallationSegment.accept(new InstallationVisitor(data));
    }

}
