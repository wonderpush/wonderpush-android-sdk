package com.wonderpush.sdk.segmentation;

import java.util.HashMap;

class Geohash {

    private static final char[] BASE32_CODES = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e', 'f', 'g',
            'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

    private final static HashMap<Character, Integer> BASE32_CODES_DICT = new HashMap<>();

    static {
        int sz = BASE32_CODES.length;
        for (int i = 0; i < sz; i++) {
            BASE32_CODES_DICT.put(BASE32_CODES[i], i);
        }
    }

    public final String geohash;
    public final double top;
    public final double right;
    public final double bottom;
    public final double left;

    protected Geohash(String geohash, double top, double right, double bottom, double left) {
        this.geohash = geohash;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }
    
    public static Geohash parse(String geohash) throws BadInputError {
        if (geohash == null) return null;
        // See: https://github.com/sunng87/node-geohash/blob/87ca0f9d6213a13b3335a6889659cad59e83d286/main.js#L170-L204
        geohash = geohash.toLowerCase();
        boolean isLon = true;
        double maxLat = +90;
        double minLat = -90;
        double maxLon = +180;
        double minLon = -180;
        double mid;

        for (int i = 0, l = geohash.length(); i < l; i++) {
            char c = geohash.charAt(i);
            Integer hashValue = BASE32_CODES_DICT.get(c);
            if (hashValue == null) {
                throw new BadInputError("character \"" + c + "\" is not valid in a geohash");
            }
            for (int bits = 4; bits >= 0; bits--) {
                int bit = (hashValue >> bits) & 1;
                if (isLon) {
                    mid = (maxLon + minLon) / 2;
                    if (bit == 1) {
                        minLon = mid;
                    } else {
                        maxLon = mid;
                    }
                } else {
                    mid = (maxLat + minLat) / 2;
                    if (bit == 1) {
                        minLat = mid;
                    } else {
                        maxLat = mid;
                    }
                }
                isLon = !isLon;
            }
        }

        return new Geohash(geohash, maxLat, maxLon, minLat, minLon);
    }
    
    public double getCenterLat() {
        return (this.top + this.bottom) / 2;
    }

    public double getCenterLon() {
        return (this.left + this.right) / 2;
    }

    public GeoLocation getTopLeft() {
        return new GeoLocation(this.top, this.left);
    }

    public GeoLocation getTopRight() {
        return new GeoLocation(this.top, this.right);
    }

    public GeoLocation getBottomLeft() {
        return new GeoLocation(this.bottom, this.left);
    }

    public GeoLocation getBottomRight() {
        return new GeoLocation(this.bottom, this.right);
    }

    public GeoLocation toGeoLocation() {
        return new GeoLocation(this.getCenterLat(), this.getCenterLon());
    }

    public GeoBox toGeoBox() {
        return GeoBox.fromTopRightBottomLeft(this.top, this.right, this.bottom, this.left);
    }

}
