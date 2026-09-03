package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

public class TimeBlocksActivity extends Activity {

    private ListView listView;
    private ArrayAdapter<TimeBlock> adapter;
    private List<TimeBlock> blocks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Typeface georgia = Fonts.current(this);

        listView = new ListView(this);
        listView.setBackgroundColor(Color.BLACK);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.addHeaderView(newBlockRow(georgia), null, false);
        UiKit.screen(this, "Time blocks", listView);

        blocks = Config.getTimeBlocks(this);
        adapter = new ArrayAdapter<TimeBlock>(this, android.R.layout.simple_list_item_1, blocks) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                TimeBlock block = blocks.get(position);
                StringBuilder text = new StringBuilder(block.name).append('\n').append(block.scheduleSummary());
                if (TimeBlockRules.isActiveNow(block, Calendar.getInstance())) {
                    text.append(" · Active");
                }
                if (!block.enabled) {
                    text.append(" · Disabled");
                }
                view.setText(text.toString());
                view.setBackground(rowBackground());
                view.setTextColor(Color.WHITE);
                view.setTextSize(18);
                view.setPadding(48, 32, 48, 32);
                view.setGravity(Gravity.START);
                view.setTypeface(georgia);
                return view;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, rawPosition, id) -> {
            int position = rawPosition - listView.getHeaderViewsCount();
            if (position < 0) return;
            Intent intent = new Intent(this, TimeBlockEditActivity.class);
            intent.putExtra("blockId", blocks.get(position).id);
            startActivity(intent);
        });

        listView.setOnItemLongClickListener((parent, view, rawPosition, id) -> {
            int position = rawPosition - listView.getHeaderViewsCount();
            if (position < 0) return false;
            Intent intent = new Intent(this, SessionLengthActivity.class);
            intent.putExtra("blockId", blocks.get(position).id);
            startActivity(intent);
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        blocks.clear();
        blocks.addAll(Config.getTimeBlocks(this));
        adapter.notifyDataSetChanged();
    }

    private TextView newBlockRow(Typeface georgia) {
        TextView row = new TextView(this);
        row.setText("+ New time block");
        row.setTextColor(Color.WHITE);
        row.setTextSize(20);
        row.setPadding(48, 32, 48, 32);
        row.setGravity(Gravity.START);
        row.setTypeface(georgia);
        row.setBackground(rowBackground());
        row.setOnClickListener(v -> {
            TimeBlock block = TimeBlock.create();
            List<TimeBlock> all = Config.getTimeBlocks(this);
            all.add(block);
            Config.setTimeBlocks(this, all);
            Intent intent = new Intent(this, TimeBlockEditActivity.class);
            intent.putExtra("blockId", block.id);
            startActivity(intent);
        });
        return row;
    }

    private Drawable rowBackground() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        drawable.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return drawable;
    }
}

