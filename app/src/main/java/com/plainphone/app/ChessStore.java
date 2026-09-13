package com.plainphone.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Private local library shared by the home Chess plugin and workspace panels. */
final class ChessStore {
    static final class Entry {
        final String id, pgn; final long updated;
        Entry(String id, String pgn, long updated) { this.id = id; this.pgn = pgn; this.updated = updated; }
        ChessGame game() { return ChessGame.fromPgn(pgn); }
    }
    private static final String PREF = "chess_games";
    private ChessStore() {}
    static List<Entry> all(Context c) {
        List<Entry> out = new ArrayList<>();
        try { JSONArray a = new JSONArray(c.getSharedPreferences("chess", 0).getString(PREF, "[]"));
            for (int i = 0; i < a.length(); i++) { JSONObject o = a.getJSONObject(i); out.add(new Entry(o.getString("id"), o.getString("pgn"), o.optLong("updated"))); }
        } catch (Exception ignored) {}
        Collections.sort(out, (x, y) -> Long.compare(y.updated, x.updated)); return out;
    }
    static Entry find(Context c, String id) { for (Entry e : all(c)) if (e.id.equals(id)) return e; return null; }
    static Entry save(Context c, String id, String pgn) {
        List<Entry> all = all(c); if (id == null) id = UUID.randomUUID().toString();
        List<Entry> next = new ArrayList<>(); for (Entry e : all) if (!e.id.equals(id)) next.add(e);
        Entry made = new Entry(id, pgn, System.currentTimeMillis()); next.add(made);
        JSONArray a = new JSONArray(); try { for (Entry e : next) a.put(new JSONObject().put("id", e.id).put("pgn", e.pgn).put("updated", e.updated)); } catch (Exception ignored) {}
        c.getSharedPreferences("chess", 0).edit().putString(PREF, a.toString()).apply(); return made;
    }
    static void delete(Context c, String id) { List<Entry> all = all(c); JSONArray a = new JSONArray(); try { for (Entry e : all) if (!e.id.equals(id)) a.put(new JSONObject().put("id",e.id).put("pgn",e.pgn).put("updated",e.updated)); } catch (Exception ignored) {} c.getSharedPreferences("chess",0).edit().putString(PREF,a.toString()).apply(); }
}
