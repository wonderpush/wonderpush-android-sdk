package com.wonderpush.sdk;

import org.json.JSONObject;

class NotificationMapModel extends NotificationModel {

    private String message;
    private Map map;

    public NotificationMapModel(String inputJSONString) {
        super(inputJSONString);
    }

    @Override
    protected void readFromJSONObject(JSONObject wpData) {
        message = JSONUtil.getString(wpData, "message");
        map = Map.fromJSONObject(wpData.optJSONObject("map"));
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public static class Map {

        private Place place;

        public Map() {
        }

        public static Map fromJSONObject(JSONObject wpMap) {
            if (wpMap == null) {
                return null;
            }
            Map rtn = new Map();
            rtn.readFromJSONObject(wpMap);
            return rtn;
        }

        protected void readFromJSONObject(JSONObject wpMap) {
            place = Place.fromJSONObject(wpMap.optJSONObject("place"));
        }

        public Place getPlace() {
            return place;
        }

        public void setPlace(Place place) {
            this.place = place;
        }

    }

    public static class Place {

        private Point point;
        private String name;
        private String query;
        private Integer zoom;

        public Place() {
        }

        public static Place fromJSONObject(JSONObject wpPlace) {
            if (wpPlace == null) {
                return null;
            }
            Place rtn = new Place();
            rtn.readFromJSONObject(wpPlace);
            return rtn;
        }

        protected void readFromJSONObject(JSONObject wpPlace) {
            point = Point.fromJSONObject(wpPlace.optJSONObject("point"));
            name = JSONUtil.getString(wpPlace, "name");
            query = JSONUtil.getString(wpPlace, "query");
            if (wpPlace.has("zoom")) {
                zoom = wpPlace.optInt("zoom", 0);
            }
        }

        public Point getPoint() {
            return point;
        }

        public void setPoint(Point point) {
            this.point = point;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public Integer getZoom() {
            return zoom;
        }

        public void setZoom(Integer zoom) {
            this.zoom = zoom;
        }

    }

    public static class Point {

        private double lat;
        private double lon;

        public Point() {
        }

        public static Point fromJSONObject(JSONObject wpPoint) {
            if (wpPoint == null) {
                return null;
            }
            Point rtn = new Point();
            rtn.readFromJSONObject(wpPoint);
            return rtn;
        }

        protected void readFromJSONObject(JSONObject wpPoint) {
            lat = wpPoint.optDouble("lat", 0.0);
            lon = wpPoint.optDouble("lon", 0.0);
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLon() {
            return lon;
        }

        public void setLon(double lon) {
            this.lon = lon;
        }

    }

}
