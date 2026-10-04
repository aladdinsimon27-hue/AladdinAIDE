package com.aladdin.ide;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

public class FileManager {
    private Activity a;
    private LinearLayout sidebarContent, tabContainer;
    private EditorManager editor;

    public FileManager(Activity activity, LinearLayout sidebarContent,
                       LinearLayout tabContainer, EditorManager editor) {
        this.a = activity;
        this.sidebarContent = sidebarContent;
        this.tabContainer = tabContainer;
        this.editor = editor;
    }

    public String readFile(File f) throws Exception {
        FileInputStream in = new FileInputStream(f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[4096]; int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
        in.close();
        return out.toString("UTF-8");
    }

    public void writeFile(File f, String d) throws Exception {
        File p = f.getParentFile();
        if (p != null && !p.exists()) p.mkdirs();
        FileOutputStream o = new FileOutputStream(f);
        o.write(d.getBytes("UTF-8"));
        o.close();
    }

    public void deleteRec(File f) {
        if (f.isDirectory()) { File[] c = f.listFiles(); if (c != null) for (File x : c) deleteRec(x); }
        f.delete();
    }

    public String fileIcon(String n) {
        if (n.endsWith(".java")) return "☕";
        if (n.endsWith(".xml")) return "📄";
        if (n.endsWith(".gradle")) return "🐘";
        if (n.endsWith(".html")) return "🌐";
        if (n.endsWith(".css")) return "🎨";
        if (n.endsWith(".js")) return "🟨";
        if (n.endsWith(".json")) return "📋";
        if (n.endsWith(".md")) return "📖";
        if (n.endsWith(".png")||n.endsWith(".jpg")) return "🖼";
        return "📃";
    }

    public String formatSize(long s) {
        if (s < 1024) return s + " B";
        if (s < 1024 * 1024) return (s / 1024) + " KB";
        return (s / (1024 * 1024)) + " MB";
    }

    public void openFile(File f) {
        if (AppState.currentFile != null && !AppState.currentFile.equals(f))
            AppState.unsavedChanges.put(AppState.currentFile.getAbsolutePath(),
                editor.getEditor().getText().toString());
        if (!AppState.recentFiles.contains(f)) {
            AppState.recentFiles.add(0, f);
            if (AppState.recentFiles.size() > 10) AppState.recentFiles.remove(10);
        } else {
            AppState.recentFiles.remove(f);
            AppState.recentFiles.add(0, f);
        }
        addTab(f);
        loadFile(f);
    }

    public void loadFile(File f) {
        try {
            AppState.currentFile = f;
            String content = AppState.unsavedChanges.containsKey(f.getAbsolutePath())
                ? AppState.unsavedChanges.get(f.getAbsolutePath()) : readFile(f);
            AppState.recordingUndo = false;
            editor.getEditor().setText(content);
            AppState.lastText = content;
            AppState.undoStack.clear(); AppState.redoStack.clear();
            AppState.recordingUndo = true;
            addTab(f);
            editor.updateCursor();
            editor.updateCount(content);
            if (AppState.minimapOn) editor.updateMinimap(content);
            editor.getEditor().setSelection(0);
        } catch (Exception e) { UIHelper.toast(a, "Cannot open"); }
    }

    public void saveCurrentFile() {
        if (AppState.currentFile == null) { UIHelper.toast(a, "No file open"); return; }
        try {
            writeFile(AppState.currentFile, editor.getEditor().getText().toString());
            AppState.unsavedChanges.remove(AppState.currentFile.getAbsolutePath());
            AppState.dirtyFiles.remove(AppState.currentFile);
            addTab(AppState.currentFile);
            UIHelper.toast(a, "💾 Saved");
        } catch (Exception e) { UIHelper.toast(a, "Save error"); }
    }

    public void saveAll() {
        for (File f : new ArrayList<File>(AppState.dirtyFiles)) {
            try {
                if (f.equals(AppState.currentFile)) writeFile(f, editor.getEditor().getText().toString());
                else if (AppState.unsavedChanges.containsKey(f.getAbsolutePath()))
                    writeFile(f, AppState.unsavedChanges.get(f.getAbsolutePath()));
            } catch (Exception e) {}
        }
        AppState.dirtyFiles.clear();
        if (AppState.currentFile != null) addTab(AppState.currentFile);
        UIHelper.toast(a, "All saved");
    }

    public void markDirty() {
        if (AppState.currentFile == null) return;
        if (!AppState.dirtyFiles.contains(AppState.currentFile)) {
            AppState.dirtyFiles.add(AppState.currentFile);
            addTab(AppState.currentFile);
        }
    }

    public void addTab(final File file) {
        if (file != null && !AppState.openFiles.contains(file)) AppState.openFiles.add(file);
        tabContainer.removeAllViews();
        for (final File f : new ArrayList<File>(AppState.openFiles)) {
            final boolean act = f.equals(AppState.currentFile);
            final boolean dir = AppState.dirtyFiles.contains(f);
            LinearLayout tl = new LinearLayout(a);
            tl.setOrientation(LinearLayout.HORIZONTAL);
            tl.setBackgroundColor(act ? AppState.BG : AppState.TAB_INACTIVE);
            View acc = new View(a);
            acc.setBackgroundColor(act ? AppState.ACCENT : 0);
            tl.addView(acc, new LinearLayout.LayoutParams(3, LinearLayout.LayoutParams.MATCH_PARENT));
            TextView name = new TextView(a);
            name.setText((dir ? "● " : "") + f.getName());
            name.setTextColor(act ? AppState.TEXT_BRIGHT : AppState.TEXT_DIM);
            name.setTextSize(10);
            name.setPadding(12, 10, 8, 10);
            TextView close = new TextView(a);
            close.setText("✕");
            close.setTextColor(AppState.TEXT_DIM);
            close.setTextSize(10);
            close.setPadding(4, 10, 12, 10);
            name.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { loadFile(f); }
            });
            close.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    AppState.openFiles.remove(f);
                    AppState.dirtyFiles.remove(f);
                    AppState.unsavedChanges.remove(f.getAbsolutePath());
                    if (f.equals(AppState.currentFile)) {
                        AppState.currentFile = null;
                        editor.getEditor().setText("");
                    }
                    addTab(null);
                }
            });
            tl.addView(name); tl.addView(close);
            tabContainer.addView(tl);
        }
    }

    public void showFileInfo(File f) {
        try {
            String content = readFile(f);
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date(f.lastModified()));
            new AlertDialog.Builder(a).setTitle("File Info")
                .setMessage("📄  " + f.getName() + "\n\n📏  " + formatSize(f.length())
                    + "\n📝  " + content.split("\n").length + " lines"
                    + "\n🔤  " + content.length() + " chars"
                    + "\n📅  " + date)
                .setPositiveButton("Close", null).show();
        } catch (Exception e) { UIHelper.toast(a, "Cannot read"); }
    }

    public int[] countProject(File dir) {
        int[] s = new int[4];
        File[] files = dir.listFiles();
        if (files == null) return s;
        for (File f : files) {
            if (f.isDirectory()) {
                s[1]++;
                int[] sub = countProject(f);
                s[0] += sub[0]; s[1] += sub[1]; s[2] += sub[2]; s[3] += sub[3];
            } else {
                s[0]++;
                s[3] += f.length();
                try { s[2] += readFile(f).split("\n").length; } catch (Exception e) {}
            }
        }
        return s;
    }

    public void exportProjectZip() {
        if (AppState.currentProject == null) { UIHelper.toast(a, "Open a project first"); return; }
        try {
            File outDir = new File(android.os.Environment.getExternalStorageDirectory(), "AladdinExports");
            if (!outDir.exists()) outDir.mkdirs();
            File out = new File(outDir, AppState.currentProject.getName() + ".zip");
            FileOutputStream fos = new FileOutputStream(out);
            ZipOutputStream zos = new ZipOutputStream(fos);
            zipDir("", AppState.currentProject, zos);
            zos.close(); fos.close();
            UIHelper.toast(a, "Exported to " + out.getAbsolutePath());
        } catch (Exception e) { UIHelper.toast(a, "Export failed"); }
    }

    private void zipDir(String base, File dir, ZipOutputStream zos) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) zipDir(base + f.getName() + "/", f, zos);
            else {
                FileInputStream fis = new FileInputStream(f);
                ZipEntry ze = new ZipEntry(base + f.getName());
                zos.putNextEntry(ze);
                byte[] buf = new byte[4096]; int n;
                while ((n = fis.read(buf)) != -1) zos.write(buf, 0, n);
                fis.close();
                zos.closeEntry();
            }
        }
    }
}
