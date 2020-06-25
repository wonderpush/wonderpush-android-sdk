package com.wonderpush.sdk.segmentation;

public class SegmentationFactory {

    private static SegmentationDSLParser defaultParser = null;

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

}
