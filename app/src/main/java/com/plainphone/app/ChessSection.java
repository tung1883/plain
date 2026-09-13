package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import java.util.List;

/** Home-library face of Chess; games themselves open in the dedicated study activity. */
final class ChessSection {
    private ChessSection() {}
    static void render(Activity a, List<Object> rows) {
        SearchResult.Kind k=SearchResult.Kind.CHESS;
        rows.add(new SearchResult(k,"+ Paste PGN","Create a local study",-1,()->a.startActivity(new Intent(a,ChessActivity.class))));
        rows.add(new SearchResult(k,"+ Open PGN file","Import from device",-1,()->a.startActivity(new Intent(a,ChessActivity.class).putExtra(ChessActivity.EXTRA_OPEN_FILE,true))));
        for(ChessStore.Entry e:ChessStore.all(a)){ ChessGame g=e.game(); rows.add(new SearchResult(k,g.title(),g.subtitle(),-1,()->a.startActivity(new Intent(a,ChessActivity.class).putExtra(ChessActivity.EXTRA_GAME_ID,e.id)),e)); }
    }
    static List<SearchResult> search(Activity a, String needle) { java.util.ArrayList<SearchResult> out=new java.util.ArrayList<>(); String n=needle.toLowerCase(); for(ChessStore.Entry e:ChessStore.all(a)){ChessGame g=e.game(); if(g.title().toLowerCase().contains(n)||g.subtitle().toLowerCase().contains(n))out.add(new SearchResult(SearchResult.Kind.CHESS,g.title(),g.subtitle(),0,()->a.startActivity(new Intent(a,ChessActivity.class).putExtra(ChessActivity.EXTRA_GAME_ID,e.id))));}return out; }
}
