package com.plainphone.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

class TimeBlock {

    enum Mode { BLACKOUT, ALLOW_ONLY }

    final String id;
    String name;
    int daysMask;
    int startMinute;
    int endMinute;
    Mode mode;
    Set<String> packages;
    boolean enabled;

    private TimeBlock(String id) {
        this.id = id;
        this.name = "New block";
        this.daysMask = 0b1111111;
        this.startMinute = 9 * 60;
        this.endMinute = 17 * 60;
        this.mode = Mode.BLACKOUT;
        this.packages = new HashSet<>();
        this.enabled = true;
    }

    static TimeBlock create() {
        return new TimeBlock(UUID.randomUUID().toString());
    }

    static TimeBlock findById(List<TimeBlock> blocks, String id) {
        if (id == null) return null;
        for (TimeBlock block : blocks) {
            if (block.id.equals(id)) return block;
        }
        return null;
    }

    boolean runsOnDay(int calendarDayOfWeek) {
        return (daysMask & (1 << (calendarDayOfWeek - 1))) != 0;
    }

    String scheduleSummary() {
        return formatTime(startMinute) + "–" + formatTime(endMinute)
                + " · " + (mode == Mode.BLACKOUT ? "Blackout" : "Allow-only");
    }

    static String formatTime(int minuteOfDay) {
        int hour24 = (minuteOfDay / 60) % 24;
        int minute = minuteOfDay % 60;
        int hour12 = hour24 % 12 == 0 ? 12 : hour24 % 12;
        String ampm = hour24 < 12 ? "AM" : "PM";
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, ampm);
    }

    JSONObject toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("name", name);
            obj.put("daysMask", daysMask);
            obj.put("startMinute", startMinute);
            obj.put("endMinute", endMinute);
            obj.put("mode", mode.name());
            obj.put("enabled", enabled);
            obj.put("packages", new JSONArray(packages));
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    static TimeBlock fromJson(JSONObject obj) throws JSONException {
        TimeBlock block = new TimeBlock(obj.getString("id"));
        block.name = obj.getString("name");
        block.daysMask = obj.getInt("daysMask");
        block.startMinute = obj.getInt("startMinute");
        block.endMinute = obj.getInt("endMinute");
        block.mode = Mode.valueOf(obj.getString("mode"));
        block.enabled = obj.getBoolean("enabled");
        Set<String> packages = new HashSet<>();
        JSONArray arr = obj.getJSONArray("packages");
        for (int i = 0; i < arr.length(); i++) {
            packages.add(arr.getString(i));
        }
        block.packages = packages;
        return block;
    }
}

