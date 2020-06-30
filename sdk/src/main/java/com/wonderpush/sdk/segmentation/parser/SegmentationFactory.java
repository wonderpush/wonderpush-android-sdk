package com.wonderpush.sdk.segmentation.parser;

public class SegmentationFactory {

    private static SegmentationDSLParser defaultParser = null;
    private static SegmentationDSLParser defaultThrowingParser = null;

    public static SegmentationDSLParser getDefaultParser() {
        if (defaultParser == null) {
            synchronized (SegmentationFactory.class) {
                if (defaultParser == null) {
                    try {
                        defaultParser = new SegmentationDSLParser(new ParserConfig(new DefaultValueNodeParser(), new DefaultCriterionNodeParser()));
                    } catch (ValueParserAlreadyExistsForKey | CriterionParserAlreadyExistsForKey ex) {
                        throw new RuntimeException("Failed to construct the default SegmentationDSLParser", ex);
                    }
                }
            }
        }
        return defaultParser;
    }

    public static SegmentationDSLParser getDefaultThrowingParser() {
        if (defaultThrowingParser == null) {
            synchronized (SegmentationFactory.class) {
                if (defaultThrowingParser == null) {
                    try {
                        defaultThrowingParser = new SegmentationDSLParser(new ParserConfig(new DefaultValueNodeParser(), new DefaultCriterionNodeParser(), true, true));
                    } catch (ValueParserAlreadyExistsForKey | CriterionParserAlreadyExistsForKey ex) {
                        throw new RuntimeException("Failed to construct the default SegmentationDSLParser", ex);
                    }
                }
            }
        }
        return defaultThrowingParser;
    }

}
