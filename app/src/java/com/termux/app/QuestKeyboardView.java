package com.termux.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.termux.terminal.TerminalSession;

import java.nio.charset.StandardCharsets;

/**
 * Quest-friendly, fully in-app terminal keyboard.
 * It never asks Android's InputMethodManager for a keyboard.
 */
public final class QuestKeyboardView extends LinearLayout {
    private boolean shift;
    private boolean ctrl;
    private boolean symbols;

    private TermuxActivity activity;
    private LinearLayout keyContainer;

    public QuestKeyboardView(Context context) {
        super(context);
        init(context);
    }

    // Required so the Android layout inflater can instantiate this view from XML
    // (<com.termux.app.QuestKeyboardView .../>). Without this constructor the app
    // crashes with InflateException as soon as activity_termux.xml is inflated.
    public QuestKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // This view is only ever inflated as part of TermuxActivity's layout, so the
        // context it receives is always the activity itself.
        activity = (TermuxActivity) context;
        setOrientation(VERTICAL);
        setPadding(4, 4, 4, 4);
        setBackgroundColor(Color.rgb(24, 24, 24));

        keyContainer = new LinearLayout(context);
        keyContainer.setOrientation(VERTICAL);
        addView(keyContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        rebuild();
    }

    private Button key(@NonNull String label, final String value, float weight) {
        Button b = new Button(getContext());
        b.setText(label);
        b.setTextSize(13f);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(0, 0, 0, 0);
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(v -> send(value));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), weight);
        p.setMargins(2, 2, 2, 2);
        b.setLayoutParams(p);
        return b;
    }

    private Button actionKey(@NonNull String label, final Runnable action, float weight) {
        Button b = new Button(getContext());
        b.setText(label);
        b.setTextSize(12f);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(0, 0, 0, 0);
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), weight);
        p.setMargins(2, 2, 2, 2);
        b.setLayoutParams(p);
        return b;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        keyContainer.addView(row, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return row;
    }

    private void rebuild() {
        keyContainer.removeAllViews();

        LinearLayout row = row();
        row.addView(actionKey("ESC", () -> send("\u001b"), 1f));
        row.addView(actionKey("TAB", () -> send("\t"), 1f));
        row.addView(actionKey(ctrl ? "CTRL*" : "CTRL", () -> { ctrl = !ctrl; rebuild(); }, 1f));
        row.addView(actionKey(symbols ? "ABC" : "123", () -> { symbols = !symbols; rebuild(); }, 1f));
        row.addView(actionKey(shift ? "SHIFT*" : "SHIFT", () -> { shift = !shift; rebuild(); }, 1f));
        row.addView(actionKey("\u232b", () -> send("\b"), 1f));
        row.addView(actionKey("ENTER", () -> send("\r"), 1.4f));

        if (symbols) {
            addRow(new String[]{"!", "@", "#", "$", "%", "^", "&", "*", "(", ")"}, 1f);
            addRow(new String[]{"-", "_", "=", "+", "[", "]", "{", "}", "\\", "|"}, 1f);
            addRow(new String[]{";", ":", "'", "\"", "`", "~", ",", ".", "<", ">"}, 1f);
            addRow(new String[]{"/", "?"}, 1f);
        } else {
            addRow(new String[]{"1","2","3","4","5","6","7","8","9","0"}, 1f);
            addRow(new String[]{"q","w","e","r","t","y","u","i","o","p"}, 1f);
            addRow(new String[]{"a","s","d","f","g","h","j","k","l"}, 1.11f);
            addRow(new String[]{"z","x","c","v","b","n","m",",",".","/"}, 1.0f);
        }

        LinearLayout nav = row();
        nav.addView(actionKey("HOME", () -> send("\u001b[H"), 1f));
        nav.addView(actionKey("END", () -> send("\u001b[F"), 1f));
        nav.addView(actionKey("PGUP", () -> send("\u001b[5~"), 1f));
        nav.addView(actionKey("PGDN", () -> send("\u001b[6~"), 1f));
        nav.addView(actionKey("SPACE", () -> send(" "), 3.5f));
        nav.addView(actionKey("\u2190", () -> send("\u001b[D"), 1f));
        nav.addView(actionKey("\u2191", () -> send("\u001b[A"), 1f));
        nav.addView(actionKey("\u2193", () -> send("\u001b[B"), 1f));
        nav.addView(actionKey("\u2192", () -> send("\u001b[C"), 1f));
    }

    private void addRow(String[] keys, float weight) {
        LinearLayout r = row();
        for (String k : keys) {
            String label = (shift && k.length() == 1 && Character.isLetter(k.charAt(0)))
                    ? k.toUpperCase()
                    : k;
            r.addView(key(label, shiftedValue(k), weight));
        }
    }

    private String shiftedValue(String value) {
        if (!shift) return value;
        if (value.length() != 1) return value;
        switch (value.charAt(0)) {
            case '1': return "!";
            case '2': return "@";
            case '3': return "#";
            case '4': return "$";
            case '5': return "%";
            case '6': return "^";
            case '7': return "&";
            case '8': return "*";
            case '9': return "(";
            case '0': return ")";
            case ',': return "<";
            case '.': return ">";
            case '/': return "?";
            case '-': return "_";
            case '=': return "+";
            default: return value.toUpperCase();
        }
    }

    private void send(String value) {
        TerminalSession session = activity.getCurrentSession();
        if (session == null || !session.isRunning()) return;

        if (ctrl && value.length() == 1 && Character.isLetter(value.charAt(0))) {
            int code = Character.toLowerCase(value.charAt(0)) - 'a' + 1;
            session.writeCodePoint(false, code);
            ctrl = false;
            rebuild();
            return;
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        session.write(bytes, 0, bytes.length);
        if (shift && value.length() == 1 && Character.isLetter(value.charAt(0))) {
            shift = false;
            rebuild();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
