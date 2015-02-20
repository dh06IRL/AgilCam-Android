package com.david.autodash;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

/**
 * Created by davidhodge on 11/16/14.
 */
public class SettingsActivity extends Activity {

    Context mContext;
    ActionBar actionBar;
    CheckBox showTraffic;
    CheckBox showSpeed;
    TextView mapType;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_settings);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        editor = sharedPreferences.edit();

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        showTraffic = (CheckBox) findViewById(R.id.show_traffic);
        showSpeed = (CheckBox) findViewById(R.id.show_speed);
        mapType = (TextView) findViewById(R.id.map_type);

        showTraffic.setChecked(sharedPreferences.getBoolean("traffic", false));
        showSpeed.setChecked(sharedPreferences.getBoolean("speed", false));

        showTraffic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editor.putBoolean("traffic", true);
                    editor.apply();
                } else {
                    editor.putBoolean("traffic", false);
                    editor.apply();
                }
            }
        });

        showSpeed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editor.putBoolean("speed", true);
                    editor.apply();
                } else {
                    editor.putBoolean("speed", false);
                    editor.apply();
                }
            }
        });

        mapType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMapLayerPicker();
            }
        });

    }

    public void showMapLayerPicker() {
        final Dialog dialog = new Dialog(mContext);
        dialog.setContentView(R.layout.dialog_map_layers);
        dialog.setTitle("Picker Layer Type");

        Button normalBtn = (Button) dialog.findViewById(R.id.normal_btn);
        normalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("layer", "normal");
                editor.apply();
                dialog.dismiss();
            }
        });

        Button hybridBtn = (Button) dialog.findViewById(R.id.hybrid_btn);
        hybridBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("layer", "hybrid");
                editor.apply();
                dialog.dismiss();
            }
        });

        Button satBtn = (Button) dialog.findViewById(R.id.sat_btn);
        satBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("layer", "sat");
                editor.apply();
                dialog.dismiss();
            }
        });

        Button terBtn = (Button) dialog.findViewById(R.id.ter_btn);
        terBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("layer", "ter");
                editor.apply();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(
                    getBaseContext().getPackageName() );
            intent .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
