package com.aladdin.ide;

import android.app.*;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.io.File;

public class MenuManager {

    public static void showCustomMenu(final Activity a, final FileManager fm,
                                      final EditorManager em, final BuildManager bm,
                                      final LinearLayout sidebarContent, final LinearLayout tabContainer) {
        final Dialog dialog = new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout layout = new LinearLayout(a);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(AppState.PANEL_BG);
        layout.setPadding(20, 20, 20, 20);

        LinearLayout header = new LinearLayout(a);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, 15);
        TextView title = UIHelper.tv(a, "☰  Menu", 18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(AppState.TEXT_BRIGHT);
        title.setPadding(0, 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView close = UIHelper.iconBtn(a, "✕", 22);
        close.setTextColor(AppState.TEXT_BRIGHT);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        header.addView(close);
        layout.addView(header);

        ScrollView scroll = new ScrollView(a);
        LinearLayout items = new LinearLayout(a);
        items.setOrientation(LinearLayout.VERTICAL);

        String[][] menuItems = {
            {"📁","New Project"},{"📄","New File"},{"📂","New Folder"},
            {"💾","Save File"},{"💾","Save All"},{"✕","Close File"},
            {"↶","Undo"},{"↷","Redo"},{"≡","Format Document"},
            {"//","Toggle Comment"},{"⎘","Duplicate Line"},{"🗑","Delete Line"},
            {"📋","Clipboard"},{"🔍","Find & Replace"},{"→","Go to Line"},
            {"🎨","Change Theme"},{"🎯","Change Accent"},{"🗺","Minimap"},
            {"↩","Word Wrap"},{"🖥","Zen Mode"},{"📜","Recent Files"},
            {"📦","Export ZIP"},{"🌐","Web Preview"},{"⚙","Settings"},
            {"ℹ","About"}
        };

        for (final String[] item : menuItems) {
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(10, 11, 10, 11);
            row.setBackgroundColor(AppState.PANEL_BG);
            TextView icon = UIHelper.tv(a, item[0], 15);
            icon.setPadding(0, 0, 15, 0);
            row.addView(icon);
            TextView label = UIHelper.tv(a, item[1], 13);
            label.setPadding(0, 0, 0, 0);
            row.addView(label);
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    dialog.dismiss();
                    execute(a, item[1], fm, em, bm, sidebarContent, tabContainer);
                }
            });
            items.addView(row);
            View divider = new View(a);
            divider.setBackgroundColor(AppState.SIDEBAR_BG);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
            items.addView(divider);
        }
        scroll.addView(items);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        layout.addView(scroll);
        dialog.setContentView(layout);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(AppState.PANEL_BG));
        }
        dialog.show();
    }

    public static void showCommandPalette(final Activity a, final FileManager fm,
                                          final EditorManager em, final BuildManager bm,
                                          final LinearLayout sidebarContent, final LinearLayout tabContainer) {
        final String[] cmds = {
            "📁  New Project", "📄  New File", "📂  New Folder",
            "💾  Save File", "💾  Save All", "✕  Close File",
            "↶  Undo", "↷  Redo", "≡  Format Document",
            "//  Toggle Comment", "⎘  Duplicate Line", "🗑  Delete Line",
            "📋  Clipboard", "🔍  Find & Replace", "→  Go to Line",
            "🎨  Change Theme", "🎯  Change Accent", "🗺  Toggle Minimap",
            "↩  Toggle Word Wrap", "🖥  Zen Mode", "📜  Recent Files",
            "📦  Export ZIP", "⚙  Settings", "ℹ  About"
        };
        new AlertDialog.Builder(a).setTitle("⌘  Command Palette")
            .setItems(cmds, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    String c = cmds[w].substring(3).trim();
                    AppState.commandHistory.add(c);
                    execute(a, c, fm, em, bm, sidebarContent, tabContainer);
                }
            }).show();
    }

    public static void execute(Activity a, String cmd, FileManager fm, EditorManager em,
                               BuildManager bm, LinearLayout sidebarContent, LinearLayout tabContainer) {
        if (cmd.equals("New Project")) createProjectDialog(a, fm, em, sidebarContent, tabContainer);
        else if (cmd.equals("New File") || cmd.equals("New Folder")) {
            if (AppState.currentProject == null) { UIHelper.toast(a, "Open a project first"); return; }
            if (cmd.equals("New File")) PanelManager.createNewFile(a, sidebarContent, fm, AppState.currentProject, tabContainer);
            else PanelManager.createNewFolder(a, sidebarContent, AppState.currentProject, fm, tabContainer);
        }
        else if (cmd.equals("Save File")) fm.saveCurrentFile();
        else if (cmd.equals("Save All")) fm.saveAll();
        else if (cmd.equals("Close File")) {
            if (AppState.currentFile != null) {
                AppState.openFiles.remove(AppState.currentFile);
                AppState.dirtyFiles.remove(AppState.currentFile);
                AppState.unsavedChanges.remove(AppState.currentFile.getAbsolutePath());
                AppState.currentFile = null;
                em.getEditor().setText("");
                fm.addTab(null);
            }
        }
        else if (cmd.equals("Undo")) em.performUndo();
        else if (cmd.equals("Redo")) em.performRedo();
        else if (cmd.equals("Format Document")) em.formatDocument();
        else if (cmd.equals("Toggle Comment")) toggleComment(em);
        else if (cmd.equals("Duplicate Line")) duplicateLine(em);
        else if (cmd.equals("Delete Line")) deleteLine(em);
        else if (cmd.equals("Clipboard")) showClipboard(a, em);
        else if (cmd.equals("Find & Replace")) showFindReplace(a, em);
        else if (cmd.equals("Go to Line")) showGotoLine(a, em);
        else if (cmd.equals("Change Theme")) showThemePicker(a);
        else if (cmd.equals("Change Accent")) showAccentPicker(a);
        else if (cmd.equals("Minimap") || cmd.equals("Toggle Minimap")) {
            AppState.minimapOn = !AppState.minimapOn;
            UIHelper.toast(a, "Minimap " + (AppState.minimapOn ? "on" : "off"));
        }
        else if (cmd.equals("Word Wrap") || cmd.equals("Toggle Word Wrap")) {
            AppState.wordWrap = !AppState.wordWrap;
            em.getEditor().setHorizontallyScrolling(!AppState.wordWrap);
            UIHelper.toast(a, "Word wrap " + (AppState.wordWrap ? "on" : "off"));
        }
        else if (cmd.equals("Zen Mode")) {
            AppState.zenMode = !AppState.zenMode;
            UIHelper.toast(a, AppState.zenMode ? "Zen on" : "Zen off");
        }
        else if (cmd.equals("Recent Files")) showRecentFiles(a, fm);
        else if (cmd.equals("Export ZIP")) fm.exportProjectZip();
        else if (cmd.equals("Web Preview")) UIHelper.toast(a, "Open an HTML file first");
        else if (cmd.equals("Settings")) PanelManager.buildPanel(a, sidebarContent, fm, em, bm, tabContainer, 7);
        else if (cmd.equals("About")) showAbout(a);
    }

    public static void createProjectDialog(final Activity a, final FileManager fm, final EditorManager em,
                                           final LinearLayout sidebarContent, final LinearLayout tabContainer) {
        final String[] templates = {
            "📱  Android App (Java)",
            "🌐  Website (HTML/CSS/JS)",
            "🎮  libGDX Game",
            "☕  Java Console App",
            "📄  Empty Project"
        };
        new AlertDialog.Builder(a).setTitle("Create New Project")
            .setItems(templates, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    showNameDialog(a, fm, em, sidebarContent, tabContainer, w);
                }
            }).show();
    }

    static void showNameDialog(final Activity a, final FileManager fm, final EditorManager em,
                               final LinearLayout sidebarContent, final LinearLayout tabContainer,
                               final int templateId) {
        final EditText in = new EditText(a);
        in.setHint("MyProject");
        new AlertDialog.Builder(a).setTitle("Project Name").setView(in)
            .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    String n = in.getText().toString().trim();
                    if (n.isEmpty()) { UIHelper.toast(a, "Enter a name"); return; }
                    try {
                        genProject(n, templateId);
                        AppState.currentProject = new File(AppState.projectsDir, n.replaceAll("[^A-Za-z0-9_]", ""));
                        PanelManager.buildPanel(a, sidebarContent, fm, em, null, tabContainer, 0);
                        UIHelper.toast(a, "✓ Project created");
                    } catch (Exception e) { UIHelper.toast(a, e.getMessage()); }
                }
            }).setNegativeButton("Cancel", null).show();
    }

    static void genProject(String name, int templateId) throws Exception {
        String s = name.replaceAll("[^A-Za-z0-9_]", "");
        if (s.isEmpty()) s = "MyProject";
        File proj = new File(AppState.projectsDir, s);
        if (proj.exists()) throw new Exception("Project already exists");
        if (templateId == 0) {
            File app = new File(proj, "app");
            File src = new File(app, "src/main/java/com/aladdin/app");
            File lay = new File(app, "src/main/res/layout");
            File val = new File(app, "src/main/res/values");
            src.mkdirs(); lay.mkdirs(); val.mkdirs();
            write(new File(proj, "settings.gradle"), "rootProject.name = \"" + s + "\"\ninclude ':app'\n");
            write(new File(proj, "build.gradle"), "plugins {\n    id 'com.android.application' version '8.2.2' apply false\n}\n");
            write(new File(app, "build.gradle"), "plugins {\n    id 'com.android.application'\n}\n\nandroid {\n    namespace 'com.aladdin.app'\n    compileSdk 34\n    defaultConfig {\n        applicationId 'com.aladdin.app'\n        minSdk 24\n        targetSdk 34\n    }\n}\n");
            write(new File(app, "src/main/AndroidManifest.xml"), "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n    <application android:label=\"" + s + "\">\n        <activity android:name=\".MainActivity\" android:exported=\"true\">\n            <intent-filter>\n                <action android:name=\"android.intent.action.MAIN\" />\n                <category android:name=\"android.intent.category.LAUNCHER\" />\n            </intent-filter>\n        </activity>\n    </application>\n</manifest>\n");
            write(new File(src, "MainActivity.java"), "package com.aladdin.app;\n\nimport android.app.Activity;\nimport android.os.Bundle;\n\npublic class MainActivity extends Activity {\n    @Override\n    protected void onCreate(Bundle b) {\n        super.onCreate(b);\n        setContentView(R.layout.activity_main);\n    }\n}\n");
            write(new File(lay, "activity_main.xml"), "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n    android:layout_width=\"match_parent\"\n    android:layout_height=\"match_parent\"\n    android:gravity=\"center\">\n    <TextView android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"\n        android:text=\"Hello!\" />\n</LinearLayout>\n");
            write(new File(val, "strings.xml"), "<resources>\n    <string name=\"app_name\">" + s + "</string>\n</resources>\n");
            write(new File(val, "styles.xml"), "<resources>\n    <style name=\"AppTheme\" parent=\"android:style/Theme.Material.Light.NoActionBar\"></style>\n</resources>\n");
        } else if (templateId == 1) {
            write(new File(proj, "index.html"), AppState.fileTemplates.get(".html"));
            write(new File(proj, "style.css"), AppState.fileTemplates.get(".css"));
            write(new File(proj, "script.js"), AppState.fileTemplates.get(".js"));
            write(new File(proj, "README.md"), "# " + s + "\n\nWeb project.\n");
        } else if (templateId == 2) {
            File core = new File(proj, "core/src");
            File assets = new File(proj, "assets");
            core.mkdirs(); assets.mkdirs();
            write(new File(core, "Game.java"), "package com.game;\n\nimport com.badlogic.gdx.ApplicationAdapter;\nimport com.badlogic.gdx.Gdx;\nimport com.badlogic.gdx.graphics.GL20;\nimport com.badlogic.gdx.graphics.g2d.SpriteBatch;\n\npublic class Game extends ApplicationAdapter {\n    SpriteBatch batch;\n    @Override\n    public void create() { batch = new SpriteBatch(); }\n    @Override\n    public void render() {\n        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);\n        batch.begin();\n        batch.end();\n    }\n    @Override\n    public void dispose() { batch.dispose(); }\n}\n");
            write(new File(proj, "README.md"), "# " + s + "\n\nlibGDX game project.\n");
        } else if (templateId == 3) {
            write(new File(proj, "Main.java"), "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello from " + s + "\");\n    }\n}\n");
            write(new File(proj, "README.md"), "# " + s + "\n\nJava console app.\n");
        } else {
            write(new File(proj, "README.md"), "# " + s + "\n\nProject created with Aladdin IDE v10.0\n");
        }
    }

    static void write(File f, String data) throws Exception {
        File p = f.getParentFile();
        if (p != null && !p.exists()) p.mkdirs();
        java.io.FileOutputStream o = new java.io.FileOutputStream(f);
        o.write(data.getBytes("UTF-8"));
        o.close();
    }

    static void toggleComment(EditorManager em) {
        EditText editor = em.getEditor();
        int s = editor.getSelectionStart();
        int e = editor.getSelectionEnd();
        String text = editor.getText().toString();
        int ls = text.lastIndexOf('\n', s) + 1;
        int le = text.indexOf('\n', Math.max(s, e));
        if (le < 0) le = text.length();
        String block = text.substring(ls, le);
        String[] lines = block.split("\n", -1);
        boolean all = true;
        for (String ln : lines) if (!ln.trim().startsWith("//")) { all = false; break; }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (all) {
                int idx = lines[i].indexOf("//");
                if (idx >= 0) sb.append(lines[i].substring(0, idx)).append(lines[i].substring(idx + 2));
                else sb.append(lines[i]);
            } else sb.append("// ").append(lines[i]);
            if (i < lines.length - 1) sb.append("\n");
        }
        editor.getText().replace(ls, le, sb.toString());
    }

    static void duplicateLine(EditorManager em) {
        EditText ed = em.getEditor();
        int pos = ed.getSelectionStart();
        String t = ed.getText().toString();
        int ls = t.lastIndexOf('\n', pos - 1) + 1;
        int le = t.indexOf('\n', pos);
        if (le < 0) le = t.length();
        String line = t.substring(ls, le);
        ed.getText().insert(le, "\n" + line);
    }

    static void deleteLine(EditorManager em) {
        EditText ed = em.getEditor();
        int pos = ed.getSelectionStart();
        String t = ed.getText().toString();
        int ls = t.lastIndexOf('\n', pos - 1) + 1;
        int le = t.indexOf('\n', pos);
        if (le < 0) le = t.length(); else le++;
        if (le > t.length()) le = t.length();
        ed.getText().delete(ls, le);
    }

    static void showClipboard(final Activity a, final EditorManager em) {
        final android.content.ClipboardManager cm = (android.content.ClipboardManager)
            a.getSystemService(Activity.CLIPBOARD_SERVICE);
        new AlertDialog.Builder(a).setTitle("Clipboard")
            .setItems(new String[]{"📋 Copy","✂ Cut","📥 Paste","☑ Select All"},
                new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        EditText ed = em.getEditor();
                        if (w == 0) {
                            int s = ed.getSelectionStart(), e = ed.getSelectionEnd();
                            if (s == e) return;
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("t", ed.getText().subSequence(s, e)));
                        } else if (w == 1) {
                            int s = ed.getSelectionStart(), e = ed.getSelectionEnd();
                            if (s == e) return;
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("t", ed.getText().subSequence(s, e)));
                            ed.getText().delete(s, e);
                        } else if (w == 2) {
                            if (cm.hasPrimaryClip()) {
                                ed.getText().insert(ed.getSelectionStart(),
                                    cm.getPrimaryClip().getItemAt(0).getText());
                            }
                        } else ed.setSelection(0, ed.getText().length());
                    }
                }).show();
    }

    static void showFindReplace(final Activity a, final EditorManager em) {
        LinearLayout ll = new LinearLayout(a);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(20, 20, 20, 20);
        final EditText f = new EditText(a); f.setHint("Find..."); ll.addView(f);
        final EditText r = new EditText(a); r.setHint("Replace..."); ll.addView(r);
        new AlertDialog.Builder(a).setTitle("🔍 Find & Replace").setView(ll)
            .setPositiveButton("Replace All", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    String find = f.getText().toString();
                    String rep = r.getText().toString();
                    if (find.isEmpty()) return;
                    String t = em.getEditor().getText().toString();
                    int c = 0;
                    while (t.contains(find) && c < 500) {
                        t = t.replaceFirst(java.util.regex.Pattern.quote(find),
                            java.util.regex.Matcher.quoteReplacement(rep));
                        c++;
                    }
                    em.getEditor().setText(t);
                    UIHelper.toast(a, "Replaced " + c);
                }
            })
            .setNegativeButton("Cancel", null).show();
    }

    static void showGotoLine(final Activity a, final EditorManager em) {
        final EditText in = new EditText(a);
        in.setHint("Line number");
        in.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(a).setTitle("Go to Line").setView(in)
            .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    try {
                        int line = Integer.parseInt(in.getText().toString());
                        String t = em.getEditor().getText().toString();
                        int idx = 0, cur = 1;
                        while (cur < line && idx < t.length()) {
                            if (t.charAt(idx) == '\n') cur++;
                            idx++;
                        }
                        em.getEditor().setSelection(Math.min(idx, t.length()));
                    } catch (Exception e) {}
                }
            }).setNegativeButton("Cancel", null).show();
    }

    static void showThemePicker(final Activity a) {
        new AlertDialog.Builder(a).setTitle("Theme")
            .setItems(new String[]{"dark","dracula","monokai","light"}, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    AppState.themeId = w;
                    a.getSharedPreferences("AladdinPrefs", 0).edit().putInt("themeId", w).apply();
                    a.finish(); a.startActivity(a.getIntent());
                }
            }).show();
    }

    static void showAccentPicker(final Activity a) {
        new AlertDialog.Builder(a).setTitle("Accent")
            .setItems(new String[]{"Blue","Purple","Green","Orange","Red","Pink"}, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    AppState.accentId = w;
                    a.getSharedPreferences("AladdinPrefs", 0).edit().putInt("accentId", w).apply();
                    a.finish(); a.startActivity(a.getIntent());
                }
            }).show();
    }

    static void showRecentFiles(final Activity a, final FileManager fm) {
        if (AppState.recentFiles.isEmpty()) { UIHelper.toast(a, "No recent files"); return; }
        String[] names = new String[AppState.recentFiles.size()];
        for (int i = 0; i < AppState.recentFiles.size(); i++) names[i] = AppState.recentFiles.get(i).getName();
        new AlertDialog.Builder(a).setTitle("Recent Files")
            .setItems(names, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    File f = AppState.recentFiles.get(w);
                    if (f.exists()) fm.openFile(f);
                    else UIHelper.toast(a, "File missing");
                }
            }).show();
    }

    static void showAbout(final Activity a) {
        new AlertDialog.Builder(a).setTitle("About Aladdin IDE")
            .setMessage("⚡ Aladdin IDE v10.0\n\nModular Edition\n\n" +
                "8-File Architecture:\n" +
                "• MainActivity.java\n" +
                "• UIHelper.java\n" +
                "• AppState.java\n" +
                "• EditorManager.java\n" +
                "• FileManager.java\n" +
                "• BuildManager.java\n" +
                "• PanelManager.java\n" +
                "• MenuManager.java")
            .setPositiveButton("Close", null).show();
    }
}
