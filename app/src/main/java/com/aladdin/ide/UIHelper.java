package com.aladdin.ide;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public class UIHelper {

    public static TextView tv(Activity a, String s, int size) {
        TextView t = new TextView(a);
        t.setText(s); t.setTextSize(size); t.setTextColor(AppState.TEXT);
        t.setPadding(18, 12, 18, 12);
        return t;
    }

    public static TextView iconBtn(Activity a, String s, int size) {
        TextView t = new TextView(a);
        t.setText(s); t.setTextSize(size); t.setTextColor(AppState.TEXT_DIM);
        t.setGravity(Gravity.CENTER);
        t.setPadding(10, 8, 10, 8);
        return t;
    }

    public static TextView tipIcon(Activity a, String icon, int size, final String tip) {
        final TextView t = iconBtn(a, icon, size);
        t.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                Toast.makeText(v.getContext(), tip, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        return t;
    }

    public static Button btn(Activity a, String s) {
        Button b = new Button(a);
        b.setText(s); b.setTextColor(AppState.TEXT); b.setTextSize(12);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(12, 8, 12, 8);
        b.setMinWidth(0); b.setMinimumWidth(0);
        return b;
    }

    public static TextView createFloatingButton(Activity a, String icon, int color) {
        TextView t = new TextView(a);
        t.setText(icon); t.setTextSize(18); t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        bg.setStroke(2, AppState.SIDEBAR_BG);
        t.setBackground(bg);
        t.setWidth(100); t.setHeight(100); t.setElevation(8);
        return t;
    }

    public static void toast(Activity a, String m) {
        Toast.makeText(a, m, Toast.LENGTH_SHORT).show();
    }

    public static int dp(Activity a, int dp) {
        return (int)(dp * a.getResources().getDisplayMetrics().density);
    }
}
