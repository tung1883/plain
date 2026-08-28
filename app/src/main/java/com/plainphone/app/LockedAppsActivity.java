package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LockedAppsActivity extends Activity {

    private PackageManager pm;
    private List<ResolveInfo> apps;
    private List<String> labels;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private EditText pinField;
    private EditText confirmPinField;
    private TextView pinStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();
        apps = loadLaunchableApps();

        labels = new ArrayList<>();
        for (ResolveInfo info : apps) {
            labels.add(info.activityInfo.applicationInfo.loadLabel(pm).toString());
        }

        listView = new ListView(this);
        listView.setBackgroundColor(Color.BLACK);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.addHeaderView(buildPinHeader());
        setContentView(listView);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, labels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout row;
                TextView label;
                CheckBox checkBox;
                if (convertView instanceof LinearLayout) {
                    row = (LinearLayout) convertView;
                    label = (TextView) row.getChildAt(0);
                    checkBox = (CheckBox) row.getChildAt(1);
                } else {
                    Typeface georgia = Fonts.current(LockedAppsActivity.this);

                    row = new LinearLayout(LockedAppsActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setBackground(rowBackground());
                    row.setPadding(48, 32, 48, 32);

                    label = new TextView(LockedAppsActivity.this);
                    label.setTextColor(Color.WHITE);
                    label.setTextSize(20);
                    label.setTypeface(georgia);
                    label.setGravity(Gravity.START);
                    row.addView(label, new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    checkBox = new CheckBox(LockedAppsActivity.this);
                    checkBox.setButtonTintList(ColorStateList.valueOf(Color.WHITE));
                    row.addView(checkBox, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                    row.setTag(checkBox);
                }

                String pkg = apps.get(position).activityInfo.packageName;
                String appLabel = labels.get(position);
                label.setText(appLabel);

                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(Config.getLockedPackages(LockedAppsActivity.this).contains(pkg));
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {

                            Set<String> locked = Config.getLockedPackages(LockedAppsActivity.this);
                            locked.add(pkg);
                            Config.setLockedPackages(LockedAppsActivity.this, locked);
                            if (!Config.isPinSet(LockedAppsActivity.this)) {
                                promptSetPin();
                            }
                        } else {

                            buttonView.setOnCheckedChangeListener(null);
                            buttonView.setChecked(true);
                            buttonView.setOnCheckedChangeListener(this);

                            Intent intent = new Intent(LockedAppsActivity.this, LockedAppChangeActivity.class);
                            intent.putExtra("package", pkg);
                            intent.putExtra("label", appLabel);
                            startActivity(intent);
                        }
                    }
                });

                return row;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Object tag = view.getTag();
            if (tag instanceof CheckBox) {
                ((CheckBox) tag).performClick();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    private View buildPinHeader() {
        Typeface georgia = Fonts.current(this);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.BLACK);
        header.setPadding(48, 48, 48, 32);

        TextView title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(georgia);
        title.setText("App-lock PIN");
        header.addView(title);

        header.setGravity(Gravity.CENTER_HORIZONTAL);

        pinField = pinField(georgia, "PIN");
        LinearLayout.LayoutParams pinParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pinParams.topMargin = 24;
        header.addView(pinField, pinParams);

        confirmPinField = pinField(georgia, "Confirm");
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        confirmParams.topMargin = 16;
        header.addView(confirmPinField, confirmParams);

        Button save = new Button(this);
        save.setText("Save PIN");
        UiKit.style(this, save);
        save.setOnClickListener(v -> trySavePin());
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        saveParams.topMargin = 16;
        header.addView(save, saveParams);

        pinStatusText = new TextView(this);
        pinStatusText.setTextColor(Color.WHITE);
        pinStatusText.setTextSize(14);
        pinStatusText.setTypeface(georgia);
        if (!Config.isPinSet(this)) {
            pinStatusText.setText("Set a PIN to protect locked apps");
        }
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = 16;
        header.addView(pinStatusText, statusParams);

        TextView listTitle = new TextView(this);
        listTitle.setTextColor(Color.WHITE);
        listTitle.setTextSize(20);
        listTitle.setTypeface(georgia);
        listTitle.setText("Locked apps");
        LinearLayout.LayoutParams listTitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        listTitleParams.topMargin = 32;
        header.addView(listTitle, listTitleParams);

        return header;
    }

    private EditText pinField(Typeface georgia, String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(Color.GRAY);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        input.setGravity(Gravity.CENTER);
        input.setTypeface(georgia);
        UiKit.style(this, input);
        return input;
    }

    private void trySavePin() {
        String pin = pinField.getText().toString();
        String confirm = confirmPinField.getText().toString();

        if (pin.length() < 4 || pin.length() > 6) {
            pinStatusText.setText("PIN must be 4-6 digits");
            return;
        }
        if (!pin.equals(confirm)) {
            pinStatusText.setText("PINs don't match — try again");
            confirmPinField.setText("");
            return;
        }

        Config.setLockPin(this, pin);
        pinField.setText("");
        confirmPinField.setText("");
        pinStatusText.setText("PIN saved");
    }

    private void promptSetPin() {
        pinStatusText.setText("Set a PIN above to protect locked apps");
        pinField.requestFocus();
    }

    private Drawable rowBackground() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        drawable.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return drawable;
    }

    private List<ResolveInfo> loadLaunchableApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);

        List<ResolveInfo> deduped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo info : resolved) {
            if (info.activityInfo.packageName.equals(getPackageName())) continue;
            if (seen.add(info.activityInfo.packageName)) {
                deduped.add(info);
            }
        }

        Collections.sort(deduped, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                return a.activityInfo.applicationInfo.loadLabel(pm).toString()
                        .compareToIgnoreCase(b.activityInfo.applicationInfo.loadLabel(pm).toString());
            }
        });
        return deduped;
    }
}

