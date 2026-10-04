package com.aladdin.ide;

import android.app.*;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class PanelManager {

    public static void buildPanel(Activity a, LinearLayout content,
                                  FileManager fm, EditorManager em,
                                  BuildManager bm, LinearLayout tabContainer,
                                  int activity) {
        content.removeAllViews();
        switch (activity) {
            case 0: buildExplorer(a, content, fm, em, tabContainer); break;
            case 1: buildSearch(a, content, fm, em); break;
            case 2: buildWebDev(a, content, fm, em); break;
            case 3: buildGameDev(a, content, fm, em); break;
            case 4: buildSnippets(a, content, em); break;
            case 5: buildOutline(a, content, em); break;
            case 6: buildDiagnostics(a, content, em); break;
            case 7: buildSettings(a, content, fm, em); break;
        }
    }

    static void buildExplorer(Activity a, LinearLayout content, FileManager fm, EditorManager em, LinearLayout tabContainer) {
        LinearLayout h = new LinearLayout(a);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(14, 14, 8, 10);
        TextView title = UIHelper.tv(a, "EXPLORER", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(AppState.TEXT_DIM);
        title.setPadding(0, 0, 0, 0);
        h.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView nf = UIHelper.tipIcon(a, "📄+", 12, "New file");
        nf.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (AppState.currentProject == null) { UIHelper.toast(a, "Open a project first"); return; }
                createNewFile(a, content, fm, AppState.currentProject, tabContainer);
            }
        });
        h.addView(nf);
        TextView nd = UIHelper.tipIcon(a, "📁+", 12, "New folder");
        nd.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (AppState.currentProject == null) { UIHelper.toast(a, "Open a project first"); return; }
                createNewFolder(a, content, AppState.currentProject, fm, tabContainer);
            }
        });
        h.addView(nd);
        TextView ex = UIHelper.tipIcon(a, "📦", 12, "Export/Import");
        ex.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(a).setTitle("📦 Export / Import")
                    .setItems(new String[]{"📦  Export as ZIP", "📥  Import from ZIP"}, new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int w) {
                            if (w == 0) fm.exportProjectZip();
                            else UIHelper.toast(a, "Place ZIP in /Downloads/AladdinExports");
                        }
                    }).show();
            }
        });
        h.addView(ex);
        TextView rf = UIHelper.tipIcon(a, "↻", 14, "Refresh");
        rf.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { buildPanel(a, content, fm, em, null, tabContainer, 0); }
        });
        h.addView(rf);
        content.addView(h);

        File[] list = AppState.projectsDir.listFiles();
        if (list == null || list.length == 0) {
            buildEmptyExplorer(a, content, fm, em, tabContainer);
            return;
        }
        Arrays.sort(list, new Comparator<File>() {
            @Override public int compare(File x, File y) { return x.getName().compareToIgnoreCase(y.getName()); }
        });
        for (final File p : list) {
            if (!p.isDirectory()) continue;
            boolean isOpen = p.equals(AppState.currentProject);
            TextView item = UIHelper.tv(a, (isOpen ? "▾ " : "▸ ") + p.getName(), 12);
            item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            item.setTextColor(isOpen ? AppState.TEXT_BRIGHT : AppState.TEXT);
            item.setPadding(14, 10, 10, 10);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (p.equals(AppState.currentProject)) AppState.currentProject = null;
                    else {
                        AppState.currentProject = p;
                        if (!AppState.recentProjects.contains(p)) AppState.recentProjects.add(0, p);
                    }
                    buildPanel(a, content, fm, em, null, tabContainer, 0);
                }
            });
            content.addView(item);
            if (isOpen) renderTree(a, content, p, 1, fm, em, tabContainer);
        }
    }

    static void renderTree(Activity a, LinearLayout content, File dir, int depth, FileManager fm, EditorManager em, LinearLayout tabContainer) {
        File[] list = dir.listFiles();
        if (list == null) return;
        Arrays.sort(list, new Comparator<File>() {
            @Override public int compare(File x, File y) {
                if (x.isDirectory() && !y.isDirectory()) return -1;
                if (!x.isDirectory() && y.isDirectory()) return 1;
                return x.getName().compareToIgnoreCase(y.getName());
            }
        });
        for (final File f : list) {
            if (!AppState.showHidden && f.getName().startsWith(".")) continue;
            boolean collapsed = AppState.collapsedFolders.contains(f);
            String icon = f.isDirectory() ? (collapsed ? "▸ " : "▾ ") : fm.fileIcon(f.getName());
            TextView item = UIHelper.tv(a, icon + f.getName(), 11);
            item.setPadding(14 + depth * 14, 8, 10, 8);
            item.setTextColor(f.isDirectory() ? AppState.TEXT : AppState.TEXT_DIM);
            if (f.isDirectory()) {
                item.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (AppState.collapsedFolders.contains(f)) AppState.collapsedFolders.remove(f);
                        else AppState.collapsedFolders.add(f);
                        buildPanel(a, content, fm, em, null, tabContainer, 0);
                    }
                });
            } else {
                if (f.equals(AppState.currentFile)) item.setTextColor(AppState.TEXT_BRIGHT);
                item.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { fm.openFile(f); }
                });
                item.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) {
                        new AlertDialog.Builder(a).setTitle(f.getName())
                            .setItems(new String[]{"Open","Info","Duplicate","Delete"}, new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface d, int w) {
                                    if (w == 0) fm.openFile(f);
                                    else if (w == 1) fm.showFileInfo(f);
                                    else if (w == 2) { try { fm.writeFile(new File(f.getParent(), "copy_" + f.getName()), fm.readFile(f)); buildPanel(a, content, fm, em, null, tabContainer, 0); } catch (Exception e) {} }
                                    else if (w == 3) { fm.deleteRec(f); buildPanel(a, content, fm, em, null, tabContainer, 0); }
                                }
                            }).show();
                        return true;
                    }
                });
            }
            content.addView(item);
            if (f.isDirectory() && !collapsed) renderTree(a, content, f, depth + 1, fm, em, tabContainer);
        }
    }

    static void buildEmptyExplorer(Activity a, LinearLayout content, FileManager fm, EditorManager em, LinearLayout tabContainer) {
        TextView empty = UIHelper.tv(a, "\n\n📭\n\nNo projects yet.\n\nCreate your first project.", 12);
        empty.setTextColor(AppState.TEXT_DIM);
        empty.setPadding(16, 20, 16, 20);
        empty.setGravity(Gravity.CENTER);
        content.addView(empty);
        Button b = UIHelper.btn(a, "+  Create Project");
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(AppState.ACCENT);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(14, 10, 14, 10);
        b.setLayoutParams(p);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { MenuManager.createProjectDialog(a, fm, em, content, tabContainer); }
        });
        content.addView(b);
    }

    static void createNewFile(final Activity a, final LinearLayout content, final FileManager fm, final File parent, final LinearLayout tabContainer) {
        final EditText in = new EditText(a);
        in.setHint("filename.ext");
        new AlertDialog.Builder(a).setTitle("New File").setView(in)
            .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    String name = in.getText().toString().trim();
                    if (name.isEmpty()) return;
                    String tmpl = "";
                    for (String ext : AppState.fileTemplates.keySet()) {
                        if (name.endsWith(ext)) { tmpl = AppState.fileTemplates.get(ext); break; }
                    }
                    try { fm.writeFile(new File(parent, name), tmpl); } catch (Exception e) {}
                    buildPanel(a, content, fm, null, null, tabContainer, 0);
                }
            }).setNegativeButton("Cancel", null).show();
    }

    static void createNewFolder(final Activity a, final LinearLayout content, final File parent, final FileManager fm, final LinearLayout tabContainer) {
        final EditText in = new EditText(a);
        in.setHint("folder");
        new AlertDialog.Builder(a).setTitle("New Folder").setView(in)
            .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    new File(parent, in.getText().toString()).mkdirs();
                    buildPanel(a, content, fm, null, null, tabContainer, 0);
                }
            }).setNegativeButton("Cancel", null).show();
    }

    static void buildSearch(Activity a, LinearLayout content, FileManager fm, EditorManager em) {
        TextView title = UIHelper.tv(a, "SEARCH", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(AppState.TEXT_DIM);
        title.setPadding(14, 14, 10, 10);
        content.addView(title);
        final EditText q = new EditText(a);
        q.setHint("Search in project...");
        q.setTextColor(AppState.TEXT);
        q.setHintTextColor(AppState.TEXT_DIM);
        q.setTextSize(12);
        q.setBackgroundColor(AppState.PANEL_BG);
        q.setPadding(12, 10, 12, 10);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        qp.setMargins(14, 4, 14, 6);
        q.setLayoutParams(qp);
        content.addView(q);
        Button s = UIHelper.btn(a, "🔍  Search");
        s.setTextColor(Color.WHITE);
        s.setBackgroundColor(AppState.ACCENT);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.setMargins(14, 4, 14, 12);
        s.setLayoutParams(sp);
        s.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String query = q.getText().toString().trim();
                if (query.isEmpty()) return;
                if (AppState.currentProject == null) { UIHelper.toast(a, "Open a project first"); return; }
                ArrayList<String> found = new ArrayList<String>();
                searchInDir(AppState.currentProject, query, found);
                content.removeAllViews();
                TextView title2 = UIHelper.tv(a, "SEARCH — " + found.size() + " results", 10);
                title2.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                title2.setTextColor(AppState.ACCENT_LIGHT);
                title2.setPadding(14, 14, 10, 10);
                content.addView(title2);
                for (String res : found) {
                    final String[] parts = res.split("\\|");
                    TextView r = UIHelper.tv(a, "  " + parts[0] + ":" + parts[1], 10);
                    r.setTextColor(AppState.TEXT_DIM);
                    r.setPadding(18, 6, 10, 6);
                    r.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            try {
                                File target = new File(AppState.currentProject, parts[0]);
                                if (target.exists()) fm.openFile(target);
                            } catch (Exception e) {}
                        }
                    });
                    content.addView(r);
                }
            }
        });
        content.addView(s);
    }

    static void searchInDir(File dir, String q, ArrayList<String> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) searchInDir(f, q, out);
            else if (f.length() < 500000 && !f.getName().startsWith(".")) {
                try {
                    FileInputStream in = new FileInputStream(f);
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[4096]; int n;
                    while ((n = in.read(buf)) != -1) bo.write(buf, 0, n);
                    in.close();
                    String content = bo.toString("UTF-8");
                    String[] lines = content.split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        if (lines[i].toLowerCase().contains(q.toLowerCase())) {
                            out.add(f.getName() + "|" + (i+1));
                            if (out.size() > 100) return;
                        }
                    }
                } catch (Exception e) {}
            }
        }
    }

    static void buildWebDev(Activity a, LinearLayout content, FileManager fm, EditorManager em) {
        TextView title = UIHelper.tv(a, "WEB DEVELOPMENT", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(AppState.TEXT_DIM);
        title.setPadding(14, 14, 10, 10);
        content.addView(title);
        addBtn(a, content, "🌐  New HTML Page", new Runnable() {
            @Override public void run() { createFileWithExt(a, fm, ".html"); }
        });
        addBtn(a, content, "🎨  New CSS File", new Runnable() {
            @Override public void run() { createFileWithExt(a, fm, ".css"); }
        });
        addBtn(a, content, "📜  New JS File", new Runnable() {
            @Override public void run() { createFileWithExt(a, fm, ".js"); }
        });
        addBtn(a, content, "📱  Responsive Preview", new Runnable() {
            @Override public void run() { UIHelper.toast(a, "Preview modes coming soon"); }
        });
        addBtn(a, content, "🎨  Color Picker", new Runnable() {
            @Override public void run() { showColorPicker(a, em); }
        });
        addBtn(a, content, "🔤  Font Selector", new Runnable() {
            @Override public void run() { showFontPicker(a, em); }
        });
        addBtn(a, content, "⚡  Emmet Expand", new Runnable() {
            @Override public void run() { showEmmet(a, em); }
        });
    }

    static void addBtn(Activity a, LinearLayout content, String label, final Runnable action) {
        Button b = UIHelper.btn(a, label);
        b.setTextColor(AppState.TEXT);
        b.setBackgroundColor(AppState.PANEL_BG);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(14, 3, 14, 3);
        b.setLayoutParams(p);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        content.addView(b);
    }

    static void createFileWithExt(final Activity a, final FileManager fm, final String ext) {
        if (AppState.currentProject == null) { UIHelper.toast(a, "Open a project first"); return; }
        final EditText in = new EditText(a);
        in.setHint("index" + ext);
        new AlertDialog.Builder(a).setTitle("New " + ext + " file").setView(in)
            .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    String name = in.getText().toString().trim();
                    if (name.isEmpty()) name = "index" + ext;
                    if (!name.endsWith(ext)) name += ext;
                    try {
                        File f = new File(AppState.currentProject, name);
                        fm.writeFile(f, AppState.fileTemplates.containsKey(ext) ? AppState.fileTemplates.get(ext) : "");
                        fm.openFile(f);
                    } catch (Exception e) {}
                }
            }).setNegativeButton("Cancel", null).show();
    }

    static void showColorPicker(Activity a, final EditorManager em) {
        final String[] colors = {"#FF6B6B","#FFA07A","#FFD93D","#6BCB77","#4D96FF",
            "#9B59B6","#E91E63","#00BCD4","#FF9800","#8BC34A"};
        new AlertDialog.Builder(a).setTitle("Pick Color")
            .setItems(colors, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    insertText(em, colors[w]);
                }
            }).show();
    }

    static void showFontPicker(Activity a, final EditorManager em) {
        final String[] fonts = {"Arial, sans-serif","Georgia, serif","'Courier New', monospace","Verdana, sans-serif"};
        new AlertDialog.Builder(a).setTitle("Font Family")
            .setItems(fonts, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    insertText(em, "font-family: " + fonts[w] + ";\n");
                }
            }).show();
    }

    static void showEmmet(Activity a, final EditorManager em) {
        final String[] emmets = {"div", "div>p", "ul>li*3", "input:text"};
        new AlertDialog.Builder(a).setTitle("Emmet")
            .setItems(emmets, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    String[] expansions = {"<div></div>", "<div>\n    <p></p>\n</div>",
                        "<ul>\n    <li></li>\n    <li></li>\n    <li></li>\n</ul>",
                        "<input type=\"text\">"};
                    insertText(em, expansions[w]);
                }
            }).show();
    }

    static void insertText(EditorManager em, String text) {
        EditText editor = em.getEditor();
        int start = editor.getSelectionStart();
        editor.getText().insert(start, text);
    }

    static void buildGameDev(Activity a, LinearLayout content, FileManager fm, EditorManager em) {
        TextView title = UIHelper.tv(a, "GAME DEVELOPMENT", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(AppState.TEXT_DIM);
        title.setPadding(14, 14, 10, 10);
        content.addView(title);
        addBtn(a, content, "🎮  Game Loop", new Runnable() {
            @Override public void run() { insertText(em, "public void render(float delta) {\n    update(delta);\n    draw();\n}\n"); }
        });
        addBtn(a, content, "🎯  Input Handler", new Runnable() {
            @Override public void run() { insertText(em, "Gdx.input.setInputProcessor(new InputAdapter() {\n    @Override\n    public boolean touchDown(int x, int y, int p, int b) { return true; }\n});\n"); }
        });
        addBtn(a, content, "💥  Collision Check", new Runnable() {
            @Override public void run() { insertText(em, "if (rect1.overlaps(rect2)) {\n    // collision\n}\n"); }
        });
        addBtn(a, content, "🎬  Sprite Batch", new Runnable() {
            @Override public void run() { insertText(em, "SpriteBatch batch = new SpriteBatch();\nSprite sprite = new Sprite(texture);\n\nbatch.begin();\nsprite.draw(batch);\nbatch.end();\n"); }
        });
        addBtn(a, content, "🔊  Sound Play", new Runnable() {
            @Override public void run() { insertText(em, "Sound sound = Gdx.audio.newSound(Gdx.files.internal(\"sound.wav\"));\nsound.play();\n"); }
        });
        addBtn(a, content, "⚙  Physics Body", new Runnable() {
            @Override public void run() { insertText(em, "BodyDef def = new BodyDef();\ndef.type = BodyDef.BodyType.DynamicBody;\nBody body = world.createBody(def);\n"); }
        });
    }

    static void buildSnippets(Activity a, LinearLayout content, EditorManager em) {
        TextView title = UIHelper.tv(a, "SNIPPETS", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(AppState.TEXT_DIM);
        title.setPadding(14, 14, 10, 10);
        content.addView(title);
        String[] snippets = {"Java Activity","Java Loop","Try/Catch","Toast",
            "XML Layout","XML TextView","XML Button","Gradle Config",
            "HTML Page","CSS Reset","JS Function","JSON Object"};
        for (final String s : snippets) {
            TextView item = UIHelper.tv(a, "  " + s, 11);
            item.setTextColor(AppState.TEXT);
            item.setPadding(14, 12, 10, 12);
            item.setBackgroundColor(AppState.PANEL_BG);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(14, 3, 14, 3);
            item.setLayoutParams(p);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    HashMap<String, String> m = new HashMap<String, String>();
                    m.put("Java Activity", "package com.example;\n\nimport android.app.Activity;\nimport android.os.Bundle;\n\npublic class MyActivity extends Activity {\n    @Override\n    protected void onCreate(Bundle b) {\n        super.onCreate(b);\n    }\n}\n");
                    m.put("Java Loop", "for (int i = 0; i < 10; i++) {\n    \n}\n");
                    m.put("Try/Catch", "try {\n    \n} catch (Exception e) {\n    e.printStackTrace();\n}\n");
                    m.put("Toast", "Toast.makeText(this, \"Hello\", Toast.LENGTH_SHORT).show();\n");
                    m.put("XML Layout", AppState.fileTemplates.get(".xml"));
                    m.put("XML TextView", "<TextView\n    android:layout_width=\"wrap_content\"\n    android:layout_height=\"wrap_content\"\n    android:text=\"Hello\" />\n");
                    m.put("XML Button", "<Button\n    android:layout_width=\"wrap_content\"\n    android:layout_height=\"wrap_content\"\n    android:text=\"Click\" />\n");
                    m.put("Gradle Config", "android {\n    namespace 'com.example.app'\n    compileSdk 34\n}\n");
                    m.put("HTML Page", AppState.fileTemplates.get(".html"));
                    m.put("CSS Reset", AppState.fileTemplates.get(".css"));
                    m.put("JS Function", "function myFunc() {\n    return true;\n}\n");
                    m.put("JSON Object", "{\n    \"key\": \"value\"\n}\n");
                    insertText(em, m.containsKey(s) ? m.get(s) : "");
                }
            });
            content.addView(item);
        }
    }

    static void buildOutline(Activity a, LinearLayout content, EditorManager em) {
        TextView title = UIHelper.tv(a, "OUTLINE", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(AppState.TEXT_DIM);
        title.setPadding(14, 14, 10, 10);
        content.addView(title);
        if (AppState.currentFile == null) {
            content.addView(UIHelper.tv(a, "Open a file to see symbols.", 11));
            return;
        }
        String text = em.getEditor().getText().toString();
        java.util.regex.Pattern cp = java.util.regex.Pattern.compile("(class|interface|enum|function)\\s+(\\w+)");
        java.util.regex.Pattern mp = java.util.regex.Pattern.compile("(public|private|void|int|String|function)\\s+[\\w<>\\[\\]]*\\s*(\\w+)\\s*[\\(=]");
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("//")) continue;
            java.util.regex.Matcher cm = cp.matcher(line);
            if (cm.find()) {
                TextView t = UIHelper.tv(a, "C " + cm.group(1) + " " + cm.group(2) + "  L" + (i+1), 11);
                t.setTextColor(AppState.ACCENT_LIGHT);
                t.setPadding(14, 8, 10, 8);
                content.addView(t);
            }
            java.util.regex.Matcher mm = mp.matcher(line);
            if (mm.find() && !line.contains("new ")) {
                TextView t = UIHelper.tv(a, "    ▸ " + mm.group(2) + "()  L" + (i+1), 11);
                t.setTextColor(AppState.TEXT);
                t.setPadding(14, 8, 10, 8);
                content.addView(t);
            }
        }
    }

    static void buildDiagnostics(Activity a, LinearLayout content, EditorManager em) {
        TextView title = UIHelper.tv(a, "DIAGNOSTICS", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(AppState.TEXT_DIM);
        title.setPadding(14, 14, 10, 10);
        content.addView(title);
        if (AppState.currentFile == null) {
            content.addView(UIHelper.tv(a, "Open a file for diagnostics.", 11));
            return;
        }
        String text = em.getEditor().getText().toString();
        int ob = 0, cb = 0;
        for (char c : text.toCharArray()) {
            if (c == '{') ob++; if (c == '}') cb++;
        }
        if (ob == cb) {
            TextView ok = UIHelper.tv(a, "✓  Braces balanced (" + ob + ")", 11);
            ok.setTextColor(AppState.GREEN);
            ok.setPadding(14, 12, 14, 12);
            content.addView(ok);
        } else {
            TextView bad = UIHelper.tv(a, "❌  Unbalanced braces: " + ob + " open, " + cb + " close", 11);
            bad.setTextColor(AppState.YELLOW);
            bad.setPadding(14, 12, 14, 12);
            content.addView(bad);
        }
        String[] lines = text.split("\n");
        TextView stats = UIHelper.tv(a, "PERFORMANCE\n\nLines: " + lines.length + "\nChars: " + text.length(), 11);
        stats.setTextColor(AppState.TEXT_DIM);
        stats.setPadding(14, 16, 14, 12);
        content.addView(stats);
    }

    static void buildSettings(Activity a, LinearLayout content, FileManager fm, EditorManager em) {
        TextView title = UIHelper.tv(a, "SETTINGS", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(AppState.TEXT_DIM);
        title.setPadding(14, 14, 10, 10);
        content.addView(title);
        section(a, content, "GITHUB");
        final EditText u = field(a, content, "Username", AppState.githubUser);
        final EditText r = field(a, content, "Repository", AppState.githubRepo);
        final EditText t = field(a, content, "Token", AppState.githubToken);
        section(a, content, "APPEARANCE");
        Button themeBtn = UIHelper.btn(a, "🎨  Theme: " + new String[]{"dark","dracula","monokai","light"}[AppState.themeId]);
        themeBtn.setTextColor(AppState.TEXT);
        themeBtn.setBackgroundColor(AppState.PANEL_BG);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(14, 4, 14, 4);
        themeBtn.setLayoutParams(tp);
        themeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(a).setTitle("Theme")
                    .setItems(new String[]{"dark","dracula","monokai","light"}, new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int w) {
                            AppState.themeId = w;
                            a.getSharedPreferences("AladdinPrefs", 0).edit().putInt("themeId", w).apply();
                            a.finish(); a.startActivity(a.getIntent());
                        }
                    }).show();
            }
        });
        content.addView(themeBtn);
        final EditText fs = field(a, content, "Font size", String.valueOf(AppState.fontSize));
        section(a, content, "EDITOR");
        final CheckBox ww = check(a, content, "Word wrap", AppState.wordWrap);
        final CheckBox ac = check(a, content, "Auto-close brackets", AppState.autoClose);
        final CheckBox ln = check(a, content, "Line numbers", AppState.lineNumbersOn);
        final CheckBox mm = check(a, content, "Minimap", AppState.minimapOn);
        Button save = UIHelper.btn(a, "💾  Save Settings");
        save.setTextColor(Color.WHITE);
        save.setBackgroundColor(AppState.ACCENT);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.setMargins(14, 12, 14, 12);
        save.setLayoutParams(sp);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AppState.githubUser = u.getText().toString().trim();
                AppState.githubRepo = r.getText().toString().trim();
                AppState.githubToken = t.getText().toString().trim();
                try { AppState.fontSize = Integer.parseInt(fs.getText().toString().trim()); } catch (Exception e) {}
                if (AppState.fontSize < 8) AppState.fontSize = 8;
                if (AppState.fontSize > 30) AppState.fontSize = 30;
                AppState.wordWrap = ww.isChecked();
                AppState.autoClose = ac.isChecked();
                AppState.lineNumbersOn = ln.isChecked();
                AppState.minimapOn = mm.isChecked();
                a.getSharedPreferences("AladdinPrefs", 0).edit()
                    .putString("user", AppState.githubUser)
                    .putString("repo", AppState.githubRepo)
                    .putString("token", AppState.githubToken)
                    .putInt("fontSize", AppState.fontSize)
                    .putBoolean("wordWrap", AppState.wordWrap)
                    .putBoolean("autoClose", AppState.autoClose)
                    .putBoolean("lineNumbers", AppState.lineNumbersOn)
                    .putBoolean("minimap", AppState.minimapOn)
                    .apply();
                UIHelper.toast(a, "✓ Saved");
            }
        });
        content.addView(save);
    }

    static void section(Activity a, LinearLayout content, String name) {
        TextView t = UIHelper.tv(a, name, 9);
        t.setTextColor(AppState.ACCENT_LIGHT);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(14, 16, 10, 4);
        content.addView(t);
    }

    static EditText field(Activity a, LinearLayout content, String hint, String value) {
        EditText e = new EditText(a);
        e.setHint(hint); e.setText(value);
        e.setTextColor(AppState.TEXT);
        e.setHintTextColor(AppState.TEXT_DIM);
        e.setTextSize(11);
        e.setBackgroundColor(AppState.PANEL_BG);
        e.setPadding(12, 10, 12, 10);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(14, 3, 14, 3);
        e.setLayoutParams(p);
        content.addView(e);
        return e;
    }

    static CheckBox check(Activity a, LinearLayout content, String label, boolean checked) {
        CheckBox c = new CheckBox(a);
        c.setText(label);
        c.setTextColor(AppState.TEXT);
        c.setTextSize(11);
        c.setChecked(checked);
        c.setPadding(14, 3, 14, 3);
        content.addView(c);
        return c;
    }
}
