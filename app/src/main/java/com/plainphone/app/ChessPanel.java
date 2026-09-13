package com.plainphone.app;

import android.content.Context;
import android.view.View;

/** A local study board that can be freely arranged alongside terminal and web panels. */
final class ChessPanel implements PanelContent {
    private final String initial; private ChessStudyView study; private Runnable titleChanged;
    ChessPanel(String initial) { this.initial=initial == null ? "" : initial; }
    @Override public String kind() { return "chess"; }
    @Override public String title() { return "Chess"; }
    @Override public String saveExtra() { return study == null ? initial : study.gameId()+":"+study.ply(); }
    @Override public void setTitleListener(Runnable changed) { titleChanged=changed; }
    @Override public View onCreate(Context ctx) {
        android.app.Activity a=(android.app.Activity)ctx;
        study=new ChessStudyView(a,(id,ply,title)->{if(titleChanged!=null)titleChanged.run();});
        View view=study.create(); if(!initial.isEmpty()) { int at=initial.lastIndexOf(':'); try { study.load(at<0?initial:initial.substring(0,at),at<0?0:Integer.parseInt(initial.substring(at+1))); } catch(Exception ignored) {} }
        return view;
    }
}
