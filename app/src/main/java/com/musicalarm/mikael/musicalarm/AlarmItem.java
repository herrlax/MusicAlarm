package com.musicalarm.mikael.musicalarm;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mikael on 2017-06-05.
 */

public class AlarmItem implements Cloneable {
    private String trackUri;
    private String imageUrl;
    private String name;
    private String artist;
    private int hour;
    private int minute;
    private boolean active;
    private int alarmID;

    private String json;

    public  AlarmItem() {

    }

    public AlarmItem(String trackUri, String imageUrl, String name, String artist,
                     int hour, int minute, boolean active, int alarmID) {
        this.trackUri = trackUri;
        this.imageUrl = imageUrl;
        this.name = name;
        this.artist = artist;
        this.hour = hour;
        this.minute = minute;
        this.active = active;
        this.alarmID = alarmID;

        try {
            jsonify();
        } catch (JSONException e) {

        }
    }

    // generates json from all class attributes
    public void jsonify() throws JSONException{

        JSONObject jsonObj = new JSONObject();
        jsonObj.accumulate("trackUri", trackUri);
        jsonObj.accumulate("imageUrl", imageUrl);
        jsonObj.accumulate("name", name);
        jsonObj.accumulate("artist", artist);
        jsonObj.accumulate("hour", hour);
        jsonObj.accumulate("minute", minute);
        jsonObj.accumulate("active", active);
        jsonObj.accumulate("alarmID", alarmID);

        this.json = jsonObj.toString();
    }

    public void buildFromJson(JSONObject json) {

        try {
            trackUri = json.getString("trackUri");
            imageUrl = json.getString("imageUrl");
            name = json.getString("name");
            artist = json.getString("artist");
            hour = Integer.parseInt(json.getString("hour"));
            minute = Integer.parseInt(json.getString("minute"));
            active = json.getBoolean("active");
            alarmID = Integer.parseInt(json.getString("alarmID"));

            this.json = json.toString();

        } catch (JSONException e) {

        }
    }

    public void setTrackUri(String trackUri) {
        this.trackUri = trackUri;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public boolean isActive() {
        return active;
    }

    public int getMinute() {
        return minute;
    }

    public int getHour() {
        return hour;
    }

    public String getArtist() {
        return artist;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTrackUri() {
        return trackUri;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public int getAlarmID() {
        return alarmID;
    }

    public void setAlarmID(int alarmID) {
        this.alarmID = alarmID;
    }

    // clones this instance
    @Override
    public AlarmItem clone() {

        AlarmItem alarmItem = new AlarmItem(trackUri, imageUrl,
                name, artist, hour, minute, active, alarmID);
        alarmItem.setJson(this.json);

        return alarmItem;
    }

}
