package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Full-screen, standalone entrance to the local Chess library. */
public class ChessActivity extends Activity {
    static final String EXTRA_GAME_ID = "gameId";
    static final String EXTRA_OPEN_FILE = "openFile";
    private static final int PICK_PGN = 0x5C20;
    private static final int SAVE_PGN = 0x5C21;
    private ChessStudyView study;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        study = new ChessStudyView(this, (id, ply, title) -> setTitle(title), this::savePgn);
        View content = study.create(); UiKit.screen(this, "Chess", content);
        String id = getIntent().getStringExtra(EXTRA_GAME_ID); if (id != null) study.load(id, 0);
        if (getIntent().getBooleanExtra(EXTRA_OPEN_FILE, false)) content.post(this::openFile);
    }
    void openFile() { Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/x-chess-pgn"); try { startActivityForResult(i,PICK_PGN); } catch(Exception e) { i.setType("text/*"); startActivityForResult(i,PICK_PGN); } }
    private void savePgn() { Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/x-chess-pgn").putExtra(Intent.EXTRA_TITLE,"game.pgn"); startActivityForResult(i,SAVE_PGN); }
    @Override protected void onActivityResult(int request, int result, Intent data) { super.onActivityResult(request,result,data); if(result!=RESULT_OK||data==null) return; if(request==PICK_PGN){String p=read(data.getData()); if(p==null) android.widget.Toast.makeText(this,"Couldn't read that PGN",android.widget.Toast.LENGTH_SHORT).show(); else study.loadPgn(p);} else if(request==SAVE_PGN) write(data.getData(),study.pgn()); }
    private String read(Uri u) { try(InputStream in=getContentResolver().openInputStream(u); ByteArrayOutputStream out=new ByteArrayOutputStream()){if(in==null)return null;byte[] b=new byte[4096];for(int n;(n=in.read(b))>=0;)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}catch(Exception e){return null;} }
    private void write(Uri u, String text) { try(java.io.OutputStream out=getContentResolver().openOutputStream(u,"wt")){if(out==null)throw new java.io.IOException();out.write(text.getBytes(StandardCharsets.UTF_8));android.widget.Toast.makeText(this,"PGN saved",android.widget.Toast.LENGTH_SHORT).show();}catch(Exception e){android.widget.Toast.makeText(this,"Couldn't save that PGN",android.widget.Toast.LENGTH_SHORT).show();} }
}
