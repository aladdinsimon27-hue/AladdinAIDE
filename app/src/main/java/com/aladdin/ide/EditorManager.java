package com.aladdin.ide;

import android.app.Activity;
import android.text.*;
import android.text.style.*;
import android.widget.*;
import java.util.regex.*;

public class EditorManager {
    private Activity a;
    private EditText editor;
    private TextView lineNumbers, minimap, statusRight, statusCount;

    public EditorManager(Activity activity, EditText editor, TextView lineNumbers,
                         TextView minimap, TextView statusRight, TextView statusCount) {
        this.a = activity;
        this.editor = editor;
        this.lineNumbers = lineNumbers;
        this.minimap = minimap;
        this.statusRight = statusRight;
        this.statusCount = statusCount;
    }

    public EditText getEditor() { return editor; }

    public void updateLineNumbers(String text) {
        int lines = 1;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') lines++;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            if (AppState.bookmarks.contains(i)) sb.append("🔖");
            sb.append(i).append("\n");
        }
        lineNumbers.setText(sb.toString());
    }

    public void updateCount(String text) {
        int words = 0; boolean inW = false;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) inW = false;
            else if (!inW) { words++; inW = true; }
        }
        statusCount.setText(words + "w " + text.length() + "c ");
    }

    public void updateCursor() {
        int cur = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        String t = editor.getText().toString();
        int line = 1, col = 1;
        for (int i = 0; i < cur && i < t.length(); i++) {
            if (t.charAt(i) == '\n') { line++; col = 1; } else col++;
        }
        int sel = Math.abs(end - cur);
        String selInfo = sel > 0 ? "  (" + sel + ")" : "";
        statusRight.setText("Ln " + line + ", Col " + col + selInfo);
    }

    public void updateMinimap(String text) {
        if (!AppState.minimapOn) return;
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        int max = Math.min(lines.length, 80);
        for (int i = 0; i < max; i++) {
            int len = Math.min(lines[i].length() / 3, 20);
            for (int j = 0; j < len; j++) sb.append("█");
            sb.append("\n");
        }
        minimap.setText(sb.toString());
    }

    public void highlight(Editable e) {
        int cur = editor.getSelectionStart();
        ForegroundColorSpan[] old = e.getSpans(0, e.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan s : old) e.removeSpan(s);
        BackgroundColorSpan[] oldB = e.getSpans(0, e.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan s : oldB) e.removeSpan(s);
        String t = e.toString();

        int ls = t.lastIndexOf('\n', cur - 1) + 1;
        int le = t.indexOf('\n', cur);
        if (le < 0) le = t.length();
        if (ls < le) e.setSpan(new BackgroundColorSpan(AppState.LINE_HL), ls, le, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (cur > 0 && cur <= t.length()) {
            char b = cur > 0 ? t.charAt(cur - 1) : 0;
            int mi = -1;
            if (b == '{' || b == '}' || b == '(' || b == ')' || b == '[' || b == ']')
                mi = findMatch(t, cur - 1, b);
            if (b != 0 && mi >= 0) {
                int x = cur - 1, y = mi;
                if (y < x) { int tmp = x; x = y; y = tmp; }
                e.setSpan(new BackgroundColorSpan(AppState.ACCENT_LIGHT), x, x+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                e.setSpan(new BackgroundColorSpan(AppState.ACCENT_LIGHT), y, y+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        Matcher m = Pattern.compile("//.*|<!--[^-]*-->").matcher(t);
        while (m.find()) e.setSpan(new ForegroundColorSpan(AppState.COMMENT), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        m = Pattern.compile("\"[^\"]*\"|'[^']*'").matcher(t);
        while (m.find()) e.setSpan(new ForegroundColorSpan(AppState.ORANGE), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        String[] kw = {"public","private","protected","class","void","int","String","boolean",
            "if","else","for","while","return","new","import","package","extends",
            "implements","final","static","abstract","try","catch","throw","throws",
            "interface","enum","this","super","true","false","null","function","const",
            "let","var","document","window","div","span","body","html"};
        for (String k : kw) {
            m = Pattern.compile("\\b" + k + "\\b").matcher(t);
            while (m.find()) e.setSpan(new ForegroundColorSpan(AppState.BLUE), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        m = Pattern.compile("<[^>]*>").matcher(t);
        while (m.find()) e.setSpan(new ForegroundColorSpan(AppState.GREEN), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        m = Pattern.compile("\\b\\d+\\b").matcher(t);
        while (m.find()) e.setSpan(new ForegroundColorSpan(AppState.NUMBER_CLR), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        m = Pattern.compile("@\\w+|#\\w+|\\.\\w+").matcher(t);
        while (m.find()) e.setSpan(new ForegroundColorSpan(AppState.YELLOW), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        editor.setSelection(cur);
    }

    public int findMatch(String t, int pos, char open) {
        char close = open == '{' ? '}' : open == '}' ? '{' : open == '(' ? ')' : open == ')' ? '(' : open == '[' ? ']' : '[';
        boolean fwd = (open == '{' || open == '(' || open == '[');
        int depth = 0;
        if (fwd) {
            for (int i = pos + 1; i < t.length(); i++) {
                char c = t.charAt(i);
                if (c == open) depth++;
                else if (c == close) { if (depth == 0) return i; depth--; }
            }
        } else {
            for (int i = pos - 1; i >= 0; i--) {
                char c = t.charAt(i);
                if (c == open) depth++;
                else if (c == close) { if (depth == 0) return i; depth--; }
            }
        }
        return -1;
    }

    public void performUndo() {
        if (AppState.undoStack.isEmpty()) { UIHelper.toast(a, "Nothing to undo"); return; }
        AppState.redoStack.add(editor.getText().toString());
        AppState.redoPos.add(editor.getSelectionStart());
        String prev = AppState.undoStack.remove(AppState.undoStack.size() - 1);
        int pos = AppState.undoPos.remove(AppState.undoPos.size() - 1);
        AppState.recordingUndo = false;
        editor.setText(prev);
        try { editor.setSelection(Math.min(pos, prev.length())); } catch (Exception e) {}
        AppState.recordingUndo = true;
        AppState.lastText = prev;
        UIHelper.toast(a, "↶ Undo");
    }

    public void performRedo() {
        if (AppState.redoStack.isEmpty()) { UIHelper.toast(a, "Nothing to redo"); return; }
        AppState.undoStack.add(editor.getText().toString());
        AppState.undoPos.add(editor.getSelectionStart());
        String next = AppState.redoStack.remove(AppState.redoStack.size() - 1);
        int pos = AppState.redoPos.remove(AppState.redoPos.size() - 1);
        AppState.recordingUndo = false;
        editor.setText(next);
        try { editor.setSelection(Math.min(pos, next.length())); } catch (Exception e) {}
        AppState.recordingUndo = true;
        AppState.lastText = next;
        UIHelper.toast(a, "↷ Redo");
    }

    public void formatDocument() {
        if (AppState.currentFile == null) { UIHelper.toast(a, "No file"); return; }
        String text = editor.getText().toString();
        StringBuilder out = new StringBuilder();
        int indent = 0;
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("}")) indent = Math.max(0, indent - 1);
            StringBuilder pad = new StringBuilder();
            for (int i = 0; i < indent; i++) pad.append("    ");
            out.append(pad).append(trimmed).append("\n");
            int opens = countChar(trimmed, '{');
            int closes = countChar(trimmed, '}');
            indent += opens - closes;
            if (indent < 0) indent = 0;
        }
        editor.setText(out.toString());
        UIHelper.toast(a, "≡ Formatted");
    }

    public int countChar(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }
}
