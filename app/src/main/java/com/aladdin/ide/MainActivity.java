package com.aladdin.ide;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.*;
import android.net.Uri;
import android.provider.Settings;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.text.style.BackgroundColorSpan;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import org.json.*;

public class MainActivity extends Activity {

    // ============ CORE LAYOUT ============
    LinearLayout root;
    LinearLayout activityBar;
    LinearLayout sidebar;
    LinearLayout sidebarContent;
    LinearLayout editorArea;
    LinearLayout tabContainer;
    LinearLayout breadcrumbBar;
    LinearLayout consolePanel;
    LinearLayout consoleContent;
    ScrollView editorScroll;
    ScrollView sidebarScroll;

    // ============ EDITOR ============
    EditText codeEditor;
    TextView lineNumbers;
    TextView welcomeScreen;
    TextView breadcrumbText;

    // ============ STATUS BAR ============
    TextView statusLeft;
    TextView statusRight;

    // ============ BOTTOM PANEL ============
    TextView tabProblems, tabOutput, tabTerminal, tabDebug;
    TextView consoleOutput;
    int activeBottomTab = 2; // 0=Problems, 1=Output, 2=Terminal, 3=Debug

    // ============ STATE ============
    int activeActivity = 0; // 0=Explorer, 1=Search, 2=Source Control, 3=Settings
    File projectsDir, currentProject, currentFile;
    ArrayList<File> projectList = new ArrayList<File>();
    ArrayList<File> openFiles = new ArrayList<File>();
    ArrayList<File> dirtyFiles = new ArrayList<File>();
    HashMap<String, String> unsavedChanges = new HashMap<String, String>();
    ArrayList<File> collapsedFolders = new ArrayList<File>();

    // ============ SETTINGS ============
    String githubToken = "", githubUser = "", githubRepo = "";
    int fontSize = 13;
    boolean wordWrap = true, autoClose = true, lineNumbersOn = true;

    // ============ COLORS (VS CODE DARK+) ============
    int BG          = Color.parseColor("#1E1E1E");
    int SIDEBAR_BG  = Color.parseColor("#252526");
    int ACTIVITY_BG = Color.parseColor("#333333");
    int PANEL_BG    = Color.parseColor("#2D2D2D");
    int TAB_INACTIVE= Color.parseColor("#2D2D2D");
    int ACCENT      = Color.parseColor("#007ACC");
    int ACCENT_LIGHT= Color.parseColor("#1F8AD2");
    int TEXT        = Color.parseColor("#CCCCCC");
    int TEXT_BRIGHT = Color.parseColor("#FFFFFF");
    int TEXT_DIM    = Color.parseColor("#858585");
    int BORDER      = Color.parseColor("#1E1E1E");
    int GREEN       = Color.parseColor("#4EC9B0");
    int ORANGE      = Color.parseColor("#CE9178");
    int BLUE        = Color.parseColor("#569CD6");
    int COMMENT     = Color.parseColor("#6A9955");
    int YELLOW      = Color.parseColor("#DCDCAA");
    int PINK        = Color.parseColor("#C586C0");
    int NUMBER_CLR  = Color.parseColor("#B5CEA8");

    // ============ LIFECYCLE ============
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        projectsDir = new File(getFilesDir(), "projects");
        if (!projectsDir.exists()) projectsDir.mkdirs();
        SharedPreferences p = getSharedPreferences("AladdinPrefs", MODE_PRIVATE);
        githubToken = p.getString("token", "");
        githubUser = p.getString("user", "");
        githubRepo = p.getString("repo", "");
        fontSize = p.getInt("fontSize", 13);
        wordWrap = p.getBoolean("wordWrap", true);
        autoClose = p.getBoolean("autoClose", true);
        lineNumbersOn = p.getBoolean("lineNumbers", true);
        buildUI();
    }

    // ============ HELPERS ============
    TextView tv(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(TEXT);
        t.setPadding(18, 12, 18, 12);
        return t;
    }

    TextView iconBtn(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size);
        t.setTextColor(TEXT_DIM);
        t.setGravity(Gravity.CENTER);
        t.setPadding(6, 6, 6, 6);
        return t;
    }

    Button btn(String s) {
        Button b = new Button(this);
        b.setText(s); b.setTextColor(TEXT); b.setTextSize(12);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(12, 8, 12, 8);
        b.setMinWidth(0); b.setMinimumWidth(0);
        return b;
    }

    // ============ MAIN UI ============
    void buildUI() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // --- TITLE BAR ---
        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setBackgroundColor(SIDEBAR_BG);
        titleBar.setElevation(6);

        TextView brand = tv("⚡ Aladdin", 15);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setTextColor(ACCENT);
        brand.setPadding(15, 12, 8, 12);
        titleBar.addView(brand);

        TextView menuLabel = tv("File  Edit  View  Run  Help", 11);
        menuLabel.setTextColor(TEXT_DIM);
        menuLabel.setPadding(8, 12, 8, 12);
        titleBar.addView(menuLabel);

        View sp1 = new View(this);
        titleBar.addView(sp1, new LinearLayout.LayoutParams(0, 1, 1));

        TextView cmdBtn = iconBtn("⌘", 18);
        cmdBtn.setTextColor(ACCENT);
        cmdBtn.setPadding(15, 8, 15, 8);
        cmdBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showCommandPalette(); }
        });
        titleBar.addView(cmdBtn);

        TextView runBtn = iconBtn("▶", 16);
        runBtn.setTextColor(ACCENT);
        runBtn.setPadding(15, 8, 20, 8);
        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentProject == null) { toast("Open a project first"); return; }
                triggerCloudBuild();
            }
        });
        titleBar.addView(runBtn);

        root.addView(titleBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // --- MAIN LAYOUT ---
        LinearLayout mainRow = new LinearLayout(this);
        mainRow.setOrientation(LinearLayout.HORIZONTAL);

        // === ACTIVITY BAR (icon strip) ===
        activityBar = new LinearLayout(this);
        activityBar.setOrientation(LinearLayout.VERTICAL);
        activityBar.setBackgroundColor(ACTIVITY_BG);
        activityBar.setPadding(0, 8, 0, 8);

        activityBar.addView(activityIcon("📁", 0, "Explorer"));
        activityBar.addView(activityIcon("🔍", 1, "Search"));
        activityBar.addView(activityIcon("⑂",  2, "Source"));
        activityBar.addView(activityIcon("⚙",  3, "Settings"));

        View sp2 = new View(this);
        activityBar.addView(sp2, new LinearLayout.LayoutParams(1, 0, 1));

        TextView bottomGear = iconBtn("⚙", 18);
        bottomGear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showSettings(); }
        });
        activityBar.addView(bottomGear, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        mainRow.addView(activityBar, new LinearLayout.LayoutParams(56,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // === SIDEBAR ===
        LinearLayout sidebarWrap = new LinearLayout(this);
        sidebarWrap.setOrientation(LinearLayout.VERTICAL);
        sidebarWrap.setBackgroundColor(SIDEBAR_BG);

        sidebarScroll = new ScrollView(this);
        sidebarContent = new LinearLayout(this);
        sidebarContent.setOrientation(LinearLayout.VERTICAL);
        sidebarScroll.addView(sidebarContent);
        sidebarWrap.addView(sidebarScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        mainRow.addView(sidebarWrap, new LinearLayout.LayoutParams(220,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // === EDITOR AREA ===
        editorArea = new LinearLayout(this);
        editorArea.setOrientation(LinearLayout.VERTICAL);
        editorArea.setBackgroundColor(BG);

        // Breadcrumb bar
        breadcrumbBar = new LinearLayout(this);
        breadcrumbBar.setBackgroundColor(BG);
        breadcrumbBar.setGravity(Gravity.CENTER_VERTICAL);
        breadcrumbBar.setPadding(20, 8, 20, 8);
        breadcrumbText = tv("", 11);
        breadcrumbText.setTextColor(TEXT_DIM);
        breadcrumbText.setPadding(0, 0, 0, 0);
        breadcrumbBar.addView(breadcrumbText);
        editorArea.addView(breadcrumbBar);

        // Tab bar
        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setBackgroundColor(PANEL_BG);
        tabContainer = new LinearLayout(this);
        tabContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabScroll.addView(tabContainer);
        editorArea.addView(tabScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 42));

        // Welcome
        welcomeScreen = new TextView(this);
        welcomeScreen.setText("⚡  ALADDIN IDE\n\n" +
                "─  Professional Edition  ─\n\n" +
                "  ⌘  Command Palette\n" +
                "  📁  Explorer & Search\n" +
                "  ⑂  Source Control\n" +
                "  ▶  Cloud Build & Auto-Install\n" +
                "  🔍  Find & Replace\n" +
                "  {  }  Auto-Close Brackets\n\n" +
                "Open a file to begin coding");
        welcomeScreen.setTextColor(TEXT_DIM);
        welcomeScreen.setTextSize(13);
        welcomeScreen.setGravity(Gravity.CENTER);
        welcomeScreen.setLineSpacing(4, 1);
        editorArea.addView(welcomeScreen, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // Editor container
        final LinearLayout editorContainer = new LinearLayout(this);
        editorContainer.setOrientation(LinearLayout.HORIZONTAL);

        lineNumbers = new TextView(this);
        lineNumbers.setTextColor(TEXT_DIM);
        lineNumbers.setTextSize(fontSize);
        lineNumbers.setTypeface(Typeface.MONOSPACE);
        lineNumbers.setPadding(18, 12, 18, 12);
        lineNumbers.setGravity(Gravity.TOP | Gravity.RIGHT);
        lineNumbers.setBackgroundColor(BG);
        lineNumbers.setMinWidth(50);

        codeEditor = new EditText(this);
        codeEditor.setTextColor(TEXT);
        codeEditor.setTextSize(fontSize);
        codeEditor.setTypeface(Typeface.MONOSPACE);
        codeEditor.setGravity(Gravity.TOP | Gravity.LEFT);
        codeEditor.setSingleLine(false);
        codeEditor.setBackgroundColor(BG);
        codeEditor.setPadding(15, 12, 15, 12);
        codeEditor.setHorizontallyScrolling(!wordWrap);
        codeEditor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        codeEditor.addTextChangedListener(new TextWatcher() {
            boolean self = false;
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (self || !autoClose) return;
                if (c > b && st + c <= s.length()) {
                    char added = s.charAt(st + c - 1);
                    String close = null;
                    if (added == '{') close = "}";
                    else if (added == '[') close = "]";
                    else if (added == '(') close = ")";
                    else if (added == '"') close = "\"";
                    else if (added == '\'') close = "'";
                    if (close != null) {
                        self = true;
                        int cur = codeEditor.getSelectionStart();
                        codeEditor.getText().insert(cur, close);
                        codeEditor.setSelection(cur);
                        self = false;
                    }
                }
            }
            @Override public void afterTextChanged(Editable s) {
                if (lineNumbersOn) updateLineNumbers(s.toString());
                highlight(s);
                updateCursor();
                markDirty();
            }
        });

        codeEditor.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { updateCursor(); }
        });

        codeEditor.setOnKeyListener(new View.OnKeyListener() {
            @Override public boolean onKey(View v, int keyCode, KeyEvent e) {
                if (e.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    int pos = codeEditor.getSelectionStart();
                    String txt = codeEditor.getText().toString();
                    if (pos > 0 && txt.charAt(pos - 1) == '{') {
                        codeEditor.getText().insert(pos, "\n    ");
                        return true;
                    }
                }
                return false;
            }
        });

        codeEditor.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override public void onScrollChange(View v, int sx, int sy, int osx, int osy) {
                lineNumbers.scrollTo(0, sy);
            }
        });

        editorContainer.addView(lineNumbers, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        editorContainer.addView(codeEditor, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        editorScroll = new ScrollView(this);
        editorScroll.addView(editorContainer);
        editorScroll.setVisibility(View.GONE);
        editorArea.addView(editorScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        mainRow.addView(editorArea, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1));

        root.addView(mainRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // === BOTTOM PANEL ===
        consolePanel = new LinearLayout(this);
        consolePanel.setOrientation(LinearLayout.VERTICAL);
        consolePanel.setBackgroundColor(Color.parseColor("#181818"));
        consolePanel.setVisibility(View.GONE);

        LinearLayout panelTabs = new LinearLayout(this);
        panelTabs.setBackgroundColor(SIDEBAR_BG);

        tabProblems = tv("  PROBLEMS  ", 10);
        tabProblems.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPanelTab(0); }
        });
        panelTabs.addView(tabProblems);

        tabOutput = tv("  OUTPUT  ", 10);
        tabOutput.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPanelTab(1); }
        });
        panelTabs.addView(tabOutput);

        tabTerminal = tv("  TERMINAL  ", 10);
        tabTerminal.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPanelTab(2); }
        });
        panelTabs.addView(tabTerminal);

        tabDebug = tv("  DEBUG  ", 10);
        tabDebug.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPanelTab(3); }
        });
        panelTabs.addView(tabDebug);

        View sp3 = new View(this);
        panelTabs.addView(sp3, new LinearLayout.LayoutParams(0, 1, 1));

        TextView closeP = iconBtn("✕", 14);
        closeP.setPadding(20, 10, 20, 10);
        closeP.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { consolePanel.setVisibility(View.GONE); }
        });
        panelTabs.addView(closeP);

        consolePanel.addView(panelTabs, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 38));

        ScrollView consoleScroll = new ScrollView(this);
        consoleOutput = new TextView(this);
        consoleOutput.setTextColor(GREEN);
        consoleOutput.setTextSize(11);
        consoleOutput.setTypeface(Typeface.MONOSPACE);
        consoleOutput.setPadding(20, 12, 20, 12);
        consoleScroll.addView(consoleOutput);
        consolePanel.addView(consoleScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 200));

        root.addView(consolePanel);

        // === STATUS BAR ===
        LinearLayout statusBar = new LinearLayout(this);
        statusBar.setOrientation(LinearLayout.HORIZONTAL);
        statusBar.setBackgroundColor(ACCENT);
        statusBar.setGravity(Gravity.CENTER_VERTICAL);

        statusLeft = tv("  ⑂ main  ✓ 0  ⚠ 0", 10);
        statusLeft.setTextColor(Color.WHITE);
        statusLeft.setPadding(15, 8, 8, 8);
        statusBar.addView(statusLeft);

        View sp4 = new View(this);
        statusBar.addView(sp4, new LinearLayout.LayoutParams(0, 1, 1));

        statusRight = tv("Ln 1, Col 1   UTF-8   LF   Java   ", 10);
        statusRight.setTextColor(Color.WHITE);
        statusRight.setPadding(8, 8, 15, 8);
        statusBar.addView(statusRight);

        root.addView(statusBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 36));

        setContentView(root);
        selectActivity(0);
        setPanelTab(2);
    }

    // ============ ACTIVITY BAR BUTTONS ============
    TextView activityIcon(String icon, final int index, String label) {
        final TextView t = new TextView(this);
        t.setText(icon);
        t.setTextSize(22);
        t.setGravity(Gravity.CENTER);
        t.setPadding(8, 14, 8, 14);
        t.setTextColor(index == activeActivity ? TEXT_BRIGHT : TEXT_DIM);

        // left accent stripe when active
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { selectActivity(index); }
        });
        return t;
    }

    void selectActivity(int index) {
        activeActivity = index;
        // refresh activity bar colors
        for (int i = 0; i < activityBar.getChildCount(); i++) {
            View c = activityBar.getChildAt(i);
            if (c instanceof TextView) {
                TextView t = (TextView) c;
                String s = t.getText().toString();
                if (s.equals("📁") || s.equals("🔍") || s.equals("⑂") || s.equals("⚙")) {
                    // reset
                    t.setTextColor(TEXT_DIM);
                    t.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        }
        // highlight current
        String[] icons = {"📁", "🔍", "⑂", "⚙"};
        for (int i = 0; i < activityBar.getChildCount(); i++) {
            View c = activityBar.getChildAt(i);
            if (c instanceof TextView) {
                TextView t = (TextView) c;
                if (t.getText().toString().equals(icons[index])) {
                    t.setTextColor(TEXT_BRIGHT);
                    t.setBackgroundColor(SIDEBAR_BG);
                }
            }
        }
        // build sidebar content
        sidebarContent.removeAllViews();
        if (index == 0) buildExplorerPanel();
        else if (index == 1) buildSearchPanel();
        else if (index == 2) buildSourcePanel();
        else if (index == 3) buildSettingsPanel();
    }

    // ============ EXPLORER PANEL ============
    void buildExplorerPanel() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(15, 15, 10, 10);

        TextView title = tv("EXPLORER", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_DIM);
        title.setPadding(0, 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView nf = iconBtn("📄+", 14);
        nf.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentProject == null) { toast("Open a project first"); return; }
                createNewFile(currentProject);
            }
        });
        header.addView(nf);

        TextView nd = iconBtn("📁+", 14);
        nd.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentProject == null) { toast("Open a project first"); return; }
                createNewFolder(currentProject);
            }
        });
        header.addView(nd);

        TextView rf = iconBtn("↻", 14);
        rf.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { selectActivity(0); }
        });
        header.addView(rf);

        sidebarContent.addView(header);

        // Projects
        File[] list = projectsDir.listFiles();
        if (list == null) return;
        Arrays.sort(list, new Comparator<File>() {
            @Override public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        for (final File p : list) {
            if (!p.isDirectory()) continue;
            projectList.add(p);
            boolean isOpen = p.equals(currentProject);
            String icon = isOpen ? "▾ " : "▸ ";
            TextView item = tv(icon + p.getName(), 13);
            item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            item.setTextColor(isOpen ? TEXT_BRIGHT : TEXT);
            item.setPadding(15, 10, 10, 10);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    currentProject = p;
                    statusLeft.setText("  ⑂ " + p.getName());
                    selectActivity(0);
                }
            });
            sidebarContent.addView(item);
            if (isOpen) renderTree(p, 1);
        }
    }

    void renderTree(File dir, int depth) {
        File[] list = dir.listFiles();
        if (list == null) return;
        Arrays.sort(list, new Comparator<File>() {
            @Override public int compare(File a, File b) {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        for (final File f : list) {
            if (f.getName().startsWith(".")) continue;
            boolean collapsed = collapsedFolders.contains(f);
            String icon;
            if (f.isDirectory()) icon = collapsed ? "▸ " : "▾ ";
            else icon = fileIcon(f.getName());

            TextView item = tv(icon + f.getName(), 12);
            item.setPadding(15 + depth * 16, 8, 10, 8);
            item.setTextColor(f.isDirectory() ? TEXT : TEXT_DIM);

            if (f.isDirectory()) {
                item.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (collapsedFolders.contains(f)) collapsedFolders.remove(f);
                        else collapsedFolders.add(f);
                        selectActivity(0);
                    }
                });
                item.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) {
                        showContextMenu(f, true); return true;
                    }
                });
            } else {
                boolean isOpen = f.equals(currentFile);
                if (isOpen) item.setTextColor(TEXT_BRIGHT);
                item.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { openFile(f); }
                });
                item.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) {
                        showContextMenu(f, false); return true;
                    }
                });
            }
            sidebarContent.addView(item);
            if (f.isDirectory() && !collapsed) renderTree(f, depth + 1);
        }
    }

    String fileIcon(String name) {
        if (name.endsWith(".java")) return "☕";
        if (name.endsWith(".xml")) return "📄";
        if (name.endsWith(".gradle")) return "🐘";
        if (name.endsWith(".kt")) return "🟣";
        if (name.endsWith(".js")) return "🟨";
        if (name.endsWith(".html")) return "🌐";
        if (name.endsWith(".css")) return "🎨";
        if (name.endsWith(".json")) return "📋";
        if (name.endsWith(".md")) return "📖";
        if (name.endsWith(".txt")) return "📝";
        if (name.endsWith(".png")||name.endsWith(".jpg")||name.endsWith(".jpeg")) return "🖼";
        if (name.endsWith(".sh")) return "⚡";
        return "📃";
    }

    // ============ SEARCH PANEL ============
    void buildSearchPanel() {
        TextView title = tv("SEARCH", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_DIM);
        title.setPadding(15, 15, 10, 10);
        sidebarContent.addView(title);

        final EditText query = new EditText(this);
        query.setHint("Search in project...");
        query.setTextColor(TEXT);
        query.setHintTextColor(TEXT_DIM);
        query.setTextSize(12);
        query.setBackgroundColor(PANEL_BG);
        query.setPadding(15, 12, 15, 12);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        qp.setMargins(15, 5, 15, 10);
        query.setLayoutParams(qp);
        sidebarContent.addView(query);

        Button searchBtn = btn("🔍  Search");
        searchBtn.setTextColor(Color.WHITE);
        searchBtn.setBackgroundColor(ACCENT);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.setMargins(15, 5, 15, 15);
        searchBtn.setLayoutParams(bp);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String q = query.getText().toString().trim();
                if (q.isEmpty()) return;
                performSearch(q);
            }
        });
        sidebarContent.addView(searchBtn);

        // results container
        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setId(999);
        sidebarContent.addView(results);
    }

    void performSearch(String q) {
        LinearLayout results = sidebarContent.findViewById(999);
        if (results == null) return;
        results.removeAllViews();
        if (currentProject == null) { toast("Open a project first"); return; }
        int count = 0;
        ArrayList<String> found = new ArrayList<String>();
        searchInDir(currentProject, q, found);
        TextView header = tv(found.size() + " results for \"" + q + "\"", 11);
        header.setTextColor(ACCENT_LIGHT);
        header.setPadding(15, 10, 10, 10);
        results.addView(header);
        for (String s : found) {
            TextView r = tv(s, 11);
            r.setTextColor(TEXT_DIM);
            r.setPadding(20, 6, 10, 6);
            results.addView(r);
        }
    }

    void searchInDir(File dir, String q, ArrayList<String> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) searchInDir(f, q, out);
            else if (f.length() < 500000) {
                try {
                    String content = readFile(f);
                    String[] lines = content.split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        if (lines[i].contains(q)) {
                            out.add(f.getName() + ":" + (i+1) + "  " + lines[i].trim());
                            if (out.size() > 100) return;
                        }
                    }
                } catch (Exception e) {}
            }
        }
    }

    // ============ SOURCE CONTROL PANEL ============
    void buildSourcePanel() {
        TextView title = tv("SOURCE CONTROL", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_DIM);
        title.setPadding(15, 15, 10, 10);
        sidebarContent.addView(title);

        if (githubUser.isEmpty() || githubRepo.isEmpty()) {
            TextView msg = tv("No repository configured.\n\nGo to Settings to add\nGitHub username, repo, and token.", 12);
            msg.setTextColor(TEXT_DIM);
            msg.setPadding(15, 15, 15, 15);
            sidebarContent.addView(msg);
            Button setup = btn("Open Settings");
            setup.setTextColor(Color.WHITE);
            setup.setBackgroundColor(ACCENT);
            setup.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { selectActivity(3); }
            });
            sidebarContent.addView(setup);
        } else {
            TextView repo = tv("⑂ " + githubUser + "/" + githubRepo, 13);
            repo.setTextColor(TEXT_BRIGHT);
            repo.setPadding(15, 10, 10, 15);
            sidebarContent.addView(repo);

            Button sync = btn("↻  Trigger Build");
            sync.setTextColor(Color.WHITE);
            sync.setBackgroundColor(ACCENT);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sp.setMargins(15, 5, 15, 5);
            sync.setLayoutParams(sp);
            sync.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (currentProject == null) { toast("Open a project first"); return; }
                    triggerCloudBuild();
                }
            });
            sidebarContent.addView(sync);

            Button logs = btn("📋  View GitHub Logs");
            logs.setTextColor(TEXT);
            logs.setBackgroundColor(PANEL_BG);
            logs.setLayoutParams(sp);
            logs.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/" + githubUser + "/" + githubRepo + "/actions"));
                    startActivity(i);
                }
            });
            sidebarContent.addView(logs);

            TextView help = tv("Modified Files", 11);
            help.setTextColor(ACCENT_LIGHT);
            help.setPadding(15, 20, 10, 5);
            sidebarContent.addView(help);
            for (File d : dirtyFiles) {
                TextView f = tv("  ● " + d.getName(), 12);
                f.setTextColor(YELLOW);
                f.setPadding(15, 6, 10, 6);
                sidebarContent.addView(f);
            }
            if (dirtyFiles.isEmpty()) {
                TextView none = tv("  No unsaved changes", 11);
                none.setTextColor(TEXT_DIM);
                none.setPadding(15, 6, 10, 6);
                sidebarContent.addView(none);
            }
        }
    }

    // ============ SETTINGS PANEL ============
    void buildSettingsPanel() {
        TextView title = tv("SETTINGS", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_DIM);
        title.setPadding(15, 15, 10, 10);
        sidebarContent.addView(title);

        settingsSection("GITHUB");
        final EditText uIn = settingsField("Username", githubUser);
        final EditText rIn = settingsField("Repository", githubRepo);
        final EditText tIn = settingsField("Token", githubToken);

        settingsSection("EDITOR");
        final EditText fsIn = settingsField("Font size (8-30)", String.valueOf(fontSize));

        final CheckBox wrapBox = settingsCheck("Word wrap", wordWrap);
        final CheckBox acBox = settingsCheck("Auto-close brackets", autoClose);
        final CheckBox lnBox = settingsCheck("Show line numbers", lineNumbersOn);

        settingsSection("ABOUT");
        TextView about = tv("Aladdin IDE v4.0\nProfessional Edition\n\nBuilt on Android with Termux + GitHub Actions", 11);
        about.setTextColor(TEXT_DIM);
        about.setPadding(15, 10, 15, 20);
        sidebarContent.addView(about);

        Button save = btn("💾  Save Settings");
        save.setTextColor(Color.WHITE);
        save.setBackgroundColor(ACCENT);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.setMargins(15, 10, 15, 15);
        save.setLayoutParams(sp);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                githubUser = uIn.getText().toString().trim();
                githubRepo = rIn.getText().toString().trim();
                githubToken = tIn.getText().toString().trim();
                try { fontSize = Integer.parseInt(fsIn.getText().toString().trim()); } catch (Exception e) {}
                if (fontSize < 8) fontSize = 8;
                if (fontSize > 30) fontSize = 30;
                wordWrap = wrapBox.isChecked();
                autoClose = acBox.isChecked();
                lineNumbersOn = lnBox.isChecked();
                codeEditor.setTextSize(fontSize);
                lineNumbers.setTextSize(fontSize);
                codeEditor.setHorizontallyScrolling(!wordWrap);
                lineNumbers.setVisibility(lineNumbersOn ? View.VISIBLE : View.GONE);
                getSharedPreferences("AladdinPrefs", MODE_PRIVATE).edit()
                        .putString("user", githubUser)
                        .putString("repo", githubRepo)
                        .putString("token", githubToken)
                        .putInt("fontSize", fontSize)
                        .putBoolean("wordWrap", wordWrap)
                        .putBoolean("autoClose", autoClose)
                        .putBoolean("lineNumbers", lineNumbersOn)
                        .apply();
                toast("✓ Settings saved");
            }
        });
        sidebarContent.addView(save);
    }

    void settingsSection(String name) {
        TextView t = tv(name, 10);
        t.setTextColor(ACCENT_LIGHT);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(15, 20, 10, 5);
        sidebarContent.addView(t);
    }

    EditText settingsField(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setTextColor(TEXT);
        e.setHintTextColor(TEXT_DIM);
        e.setTextSize(12);
        e.setBackgroundColor(PANEL_BG);
        e.setPadding(15, 12, 15, 12);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(15, 3, 15, 3);
        e.setLayoutParams(p);
        sidebarContent.addView(e);
        return e;
    }

    CheckBox settingsCheck(String label, boolean checked) {
        CheckBox c = new CheckBox(this);
        c.setText(label);
        c.setTextColor(TEXT);
        c.setTextSize(12);
        c.setChecked(checked);
        c.setPadding(15, 5, 15, 5);
        sidebarContent.addView(c);
        return c;
    }

    // ============ COMMAND PALETTE ============
    void showCommandPalette() {
        final EditText input = new EditText(this);
        input.setHint("Type a command...");
        input.setTextColor(TEXT);
        input.setHintTextColor(TEXT_DIM);
        input.setBackgroundColor(PANEL_BG);
        input.setPadding(30, 25, 30, 25);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("⌘  Command Palette")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    // ============ BOTTOM PANEL TABS ============
    void setPanelTab(int index) {
        activeBottomTab = index;
        TextView[] tabs = {tabProblems, tabOutput, tabTerminal, tabDebug};
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == null) continue;
            tabs[i].setTextColor(i == index ? ACCENT_LIGHT : TEXT_DIM);
            tabs[i].setBackgroundColor(i == index ? Color.parseColor("#181818") : SIDEBAR_BG);
        }
        if (index == 0) consoleOutput.setText("No problems detected in the workspace.");
        else if (index == 1) consoleOutput.setText("Output log ready. Run a build to see results.");
        else if (index == 2) consoleOutput.setText("$ _\nAladdin Terminal v4.0\nReady for cloud build.");
        else if (index == 3) consoleOutput.setText("Debug console.\nNo active session.");
    }

    // ============ FILE OPERATIONS ============
    void openFile(File f) {
        if (currentFile != null && !currentFile.equals(f))
            unsavedChanges.put(currentFile.getAbsolutePath(), codeEditor.getText().toString());
        addTab(f);
        loadFile(f);
    }

    void loadFile(File f) {
        try {
            currentFile = f;
            String content = unsavedChanges.containsKey(f.getAbsolutePath())
                    ? unsavedChanges.get(f.getAbsolutePath()) : readFile(f);
            codeEditor.setText(content);
            breadcrumbText.setText("▸  " + (currentProject != null ? currentProject.getName() : "") + "  ▸  " + f.getName());
            welcomeScreen.setVisibility(View.GONE);
            editorScroll.setVisibility(View.VISIBLE);
            addTab(f);
            updateCursor();
            statusLeft.setText("  ⑂ " + (currentProject != null ? currentProject.getName() : "") + "  ● " + f.getName());
        } catch (Exception e) { toast("Cannot open file"); }
    }

    void saveCurrentFile() {
        if (currentFile == null) { toast("No file open"); return; }
        try {
            writeFile(currentFile, codeEditor.getText().toString());
            unsavedChanges.remove(currentFile.getAbsolutePath());
            dirtyFiles.remove(currentFile);
            addTab(currentFile);
            toast("💾 Saved");
        } catch (Exception e) { toast("Save error"); }
    }

    String readFile(File f) throws Exception {
        FileInputStream in = new FileInputStream(f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[4096]; int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
        in.close();
        return out.toString("UTF-8");
    }

    void writeFile(File f, String d) throws Exception {
        File p = f.getParentFile();
        if (p != null && !p.exists()) p.mkdirs();
        FileOutputStream o = new FileOutputStream(f);
        o.write(d.getBytes("UTF-8"));
        o.close();
    }

    void deleteRec(File f) {
        if (f.isDirectory()) { File[] c = f.listFiles(); if (c != null) for (File x : c) deleteRec(x); }
        f.delete();
    }

    void markDirty() {
        if (currentFile == null) return;
        if (!dirtyFiles.contains(currentFile)) {
            dirtyFiles.add(currentFile);
            addTab(currentFile);
            statusLeft.setText("  ⑂ " + (currentProject != null ? currentProject.getName() : "") + "  ● " + currentFile.getName());
        }
    }

    void addTab(final File file) {
        if (!openFiles.contains(file)) openFiles.add(file);
        tabContainer.removeAllViews();
        for (final File f : openFiles) {
            final boolean act = f.equals(currentFile);
            final boolean dir = dirtyFiles.contains(f);
            LinearLayout tl = new LinearLayout(this);
            tl.setOrientation(LinearLayout.HORIZONTAL);
            tl.setBackgroundColor(act ? BG : TAB_INACTIVE);
            View acc = new View(this);
            acc.setBackgroundColor(act ? ACCENT : Color.TRANSPARENT);
            tl.addView(acc, new LinearLayout.LayoutParams(3, LinearLayout.LayoutParams.MATCH_PARENT));
            TextView name = new TextView(this);
            name.setText((dir ? "● " : "") + f.getName());
            name.setTextColor(act ? TEXT_BRIGHT : TEXT_DIM);
            name.setTextSize(11);
            name.setPadding(15, 14, 10, 14);
            TextView close = new TextView(this);
            close.setText("✕");
            close.setTextColor(TEXT_DIM);
            close.setTextSize(11);
            close.setPadding(6, 14, 15, 14);
            name.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { loadFile(f); }
            });
            close.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    openFiles.remove(f); dirtyFiles.remove(f);
                    unsavedChanges.remove(f.getAbsolutePath());
                    if (f.equals(currentFile)) {
                        currentFile = null;
                        codeEditor.setText("");
                        welcomeScreen.setVisibility(View.VISIBLE);
                        editorScroll.setVisibility(View.GONE);
                        breadcrumbText.setText("");
                    }
                    addTab(f);
                }
            });
            tl.addView(name); tl.addView(close);
            tabContainer.addView(tl);
        }
    }

    // ============ PROJECTS ============
    void createProject() {
        final EditText in = new EditText(this);
        in.setHint("MyAwesomeApp");
        new AlertDialog.Builder(this).setTitle("Create New Project").setView(in)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String n = in.getText().toString().trim();
                        if (n.isEmpty()) return;
                        try {
                            genProject(n);
                            currentProject = new File(projectsDir, n.replaceAll("[^A-Za-z0-9_]", ""));
                            selectActivity(0);
                            toast("✓ Project created");
                        } catch (Exception e) { toast(e.getMessage()); }
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    void showContextMenu(final File f, final boolean isDir) {
        final String[] opts = isDir
                ? new String[]{"New File", "New Folder", "Rename", "Delete"}
                : new String[]{"Open", "Rename", "Delete"};
        new AlertDialog.Builder(this).setTitle(f.getName())
                .setItems(opts, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String c = opts[w];
                        if (c.equals("New File")) createNewFile(f);
                        else if (c.equals("New Folder")) createNewFolder(f);
                        else if (c.equals("Open")) openFile(f);
                        else if (c.equals("Rename")) renameFile(f);
                        else if (c.equals("Delete")) deleteFile(f);
                    }
                }).show();
    }

    void createNewFile(final File p) {
        final EditText in = new EditText(this);
        in.setHint("file.java");
        new AlertDialog.Builder(this).setTitle("New File").setView(in)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        try { writeFile(new File(p, in.getText().toString()), ""); selectActivity(0); } catch (Exception e) {}
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    void createNewFolder(final File p) {
        final EditText in = new EditText(this);
        in.setHint("folder");
        new AlertDialog.Builder(this).setTitle("New Folder").setView(in)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        new File(p, in.getText().toString()).mkdirs();
                        selectActivity(0);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    void renameFile(final File f) {
        final EditText in = new EditText(this);
        in.setText(f.getName());
        new AlertDialog.Builder(this).setTitle("Rename").setView(in)
                .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        f.renameTo(new File(f.getParent(), in.getText().toString()));
                        selectActivity(0);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    void deleteFile(final File f) {
        new AlertDialog.Builder(this).setTitle("Delete").setMessage("Delete " + f.getName() + "?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        deleteRec(f);
                        selectActivity(0);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    // ============ LINE NUMBERS & SYNTAX ============
    void updateLineNumbers(String text) {
        int lines = 1;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') lines++;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) sb.append(i).append("\n");
        lineNumbers.setText(sb.toString());
    }

    void highlight(Editable e) {
        int cur = codeEditor.getSelectionStart();
        ForegroundColorSpan[] old = e.getSpans(0, e.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan s : old) e.removeSpan(s);
        String t = e.toString();

        Matcher m = Pattern.compile("//.*").matcher(t);
        while (m.find()) e.setSpan(new ForegroundColorSpan(COMMENT), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        m = Pattern.compile("\"[^\"]*\"").matcher(t);
        while (m.find()) e.setSpan(new ForegroundColorSpan(ORANGE), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        String[] kw = {"public","private","protected","class","void","int","String","boolean",
                "if","else","for","while","return","new","import","package","extends",
                "implements","final","static","abstract","try","catch","throw","throws",
                "interface","enum","this","super","true","false","null"};
        for (String k : kw) {
            m = Pattern.compile("\\b" + k + "\\b").matcher(t);
            while (m.find()) e.setSpan(new ForegroundColorSpan(BLUE), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        m = Pattern.compile("<[^>]*>").matcher(t);
        while (m.find()) e.setSpan(new ForegroundColorSpan(GREEN), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        m = Pattern.compile("\\b\\d+\\b").matcher(t);
        while (m.find()) e.setSpan(new ForegroundColorSpan(NUMBER_CLR), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        m = Pattern.compile("@\\w+").matcher(t);
        while (m.find()) e.setSpan(new ForegroundColorSpan(YELLOW), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        codeEditor.setSelection(cur);
    }

    void updateCursor() {
        int cur = codeEditor.getSelectionStart();
        String t = codeEditor.getText().toString();
        int line = 1, col = 1;
        for (int i = 0; i < cur && i < t.length(); i++) {
            if (t.charAt(i) == '\n') { line++; col = 1; } else col++;
        }
        String lang = "Plain";
        if (currentFile != null) {
            String n = currentFile.getName();
            if (n.endsWith(".java")) lang = "Java";
            else if (n.endsWith(".xml")) lang = "XML";
            else if (n.endsWith(".gradle")) lang = "Groovy";
            else if (n.endsWith(".js")) lang = "JavaScript";
            else if (n.endsWith(".html")) lang = "HTML";
        }
        statusRight.setText("Ln " + line + ", Col " + col + "   UTF-8   LF   " + lang + "   ");
    }

    // ============ FIND & REPLACE ============
    void showFindReplace() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(20, 20, 20, 20);
        final EditText f = new EditText(this); f.setHint("Find..."); ll.addView(f);
        final EditText r = new EditText(this); r.setHint("Replace with..."); ll.addView(r);
        new AlertDialog.Builder(this).setTitle("🔍  Find & Replace").setView(ll)
                .setPositiveButton("Replace All", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String find = f.getText().toString();
                        String rep = r.getText().toString();
                        if (find.isEmpty()) return;
                        String t = codeEditor.getText().toString();
                        int c = 0;
                        while (t.contains(find) && c < 500) {
                            t = t.replaceFirst(Pattern.quote(find), Matcher.quoteReplacement(rep));
                            c++;
                        }
                        codeEditor.setText(t);
                        toast("Replaced " + c);
                    }
                })
                .setNeutralButton("Find Next", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String find = f.getText().toString();
                        if (find.isEmpty()) return;
                        String t = codeEditor.getText().toString();
                        int cur = codeEditor.getSelectionStart();
                        int idx = t.indexOf(find, cur);
                        if (idx == -1) idx = t.indexOf(find);
                        if (idx >= 0) codeEditor.setSelection(idx, idx + find.length());
                        else toast("Not found");
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    // ============ SETTINGS DIALOG (title bar) ============
    void showSettings() {
        selectActivity(3);
    }

    // ============ BUILD ENGINE ============
    void log(String m) {
        runOnUiThread(new Runnable() {
            @Override public void run() { consoleOutput.append(m + "\n"); }
        });
    }

    void triggerCloudBuild() {
        if (githubToken.isEmpty() || githubUser.isEmpty() || githubRepo.isEmpty()) {
            toast("Set GitHub settings first!");
            selectActivity(3);
            return;
        }
        consolePanel.setVisibility(View.VISIBLE);
        setPanelTab(2);
        consoleOutput.setText("");
        log("$ gradle assembleDebug");
        log("🚀 Initializing cloud build...");
        log("📦 " + githubUser + "/" + githubRepo);
        log("─────────────────────────────");

        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    URL url = new URL("https://api.github.com/repos/" + githubUser + "/" + githubRepo
                            + "/actions/workflows/build.yml/dispatches");
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("POST");
                    c.setRequestProperty("Authorization", "token " + githubToken);
                    c.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    c.setDoOutput(true);
                    OutputStream o = c.getOutputStream();
                    o.write("{\"ref\":\"main\"}".getBytes("UTF-8"));
                    o.close();
                    int code = c.getResponseCode();
                    if (code == 204) {
                        log("✅ Build triggered");
                        log("⏳ Waiting for runner...");
                        Thread.sleep(15000);
                        checkStatus();
                    } else log("❌ Failed. Code: " + code);
                } catch (Exception e) { log("❌ " + e.getMessage()); }
            }
        }).start();
    }

    void checkStatus() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    boolean done = false;
                    int p = 0;
                    while (!done && p < 60) {
                        p++;
                        URL url = new URL("https://api.github.com/repos/" + githubUser + "/"
                                + githubRepo + "/actions/runs?per_page=1");
                        HttpURLConnection c = (HttpURLConnection) url.openConnection();
                        c.setRequestProperty("Authorization", "token " + githubToken);
                        c.setRequestProperty("Accept", "application/vnd.github.v3+json");
                        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String l;
                        while ((l = br.readLine()) != null) sb.append(l);
                        br.close();
                        JSONObject j = new JSONObject(sb.toString());
                        JSONArray runs = j.getJSONArray("workflow_runs");
                        if (runs.length() > 0) {
                            JSONObject r = runs.getJSONObject(0);
                            String st = r.getString("status");
                            String cc = r.optString("conclusion", "pending");
                            log("  ▸ " + st);
                            if (st.equals("completed")) {
                                done = true;
                                if (cc.equals("success")) {
                                    log("🎉 BUILD SUCCESSFUL");
                                    log("📥 Downloading APK...");
                                    download(r.getLong("id"));
                                } else log("❌ BUILD FAILED");
                            }
                        }
                        Thread.sleep(10000);
                    }
                } catch (Exception e) { log("❌ " + e.getMessage()); }
            }
        }).start();
    }

    void download(long runId) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    URL url = new URL("https://api.github.com/repos/" + githubUser + "/"
                            + githubRepo + "/actions/runs/" + runId + "/artifacts");
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestProperty("Authorization", "token " + githubToken);
                    c.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String l;
                    while ((l = br.readLine()) != null) sb.append(l);
                    br.close();
                    JSONObject j = new JSONObject(sb.toString());
                    JSONArray ar = j.getJSONArray("artifacts");
                    if (ar.length() == 0) { log("❌ No artifacts"); return; }
                    String dl = ar.getJSONObject(0).getString("archive_download_url");
                    URL du = new URL(dl);
                    HttpURLConnection dc = (HttpURLConnection) du.openConnection();
                    dc.setRequestProperty("Authorization", "token " + githubToken);
                    dc.setInstanceFollowRedirects(true);
                    InputStream in = dc.getInputStream();
                    File dir = getExternalFilesDir(null);
                    if (dir != null && !dir.exists()) dir.mkdirs();
                    File zip = new File(dir, "AladdinIDE-APK.zip");
                    FileOutputStream o = new FileOutputStream(zip);
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) o.write(buf, 0, n);
                    o.close(); in.close();
                    log("✅ ZIP downloaded");
                    File apk = extract(zip, dir);
                    if (apk == null) { log("❌ Extract failed"); return; }
                    log("📲 Launching installer...");
                    install(apk);
                } catch (Exception e) { log("❌ " + e.getMessage()); }
            }
        }).start();
    }

    File extract(File zip, File out) {
        try {
            ZipInputStream z = new ZipInputStream(new FileInputStream(zip));
            ZipEntry en;
            File apk = null;
            while ((en = z.getNextEntry()) != null) {
                if (en.getName().endsWith(".apk")) {
                    apk = new File(out, "AladdinIDE-latest.apk");
                    FileOutputStream fo = new FileOutputStream(apk);
                    byte[] b = new byte[8192];
                    int n;
                    while ((n = z.read(b)) != -1) fo.write(b, 0, n);
                    fo.close();
                    break;
                }
            }
            z.close();
            return apk;
        } catch (Exception e) { return null; }
    }

    void install(final File apk) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!getPackageManager().canRequestPackageInstalls()) {
                            log("⚠ Allow install for Aladdin IDE");
                            Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                            i.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(i);
                            return;
                        }
                    }
                    Uri uri = FileProvider.getUriForFile(MainActivity.this,
                            "com.aladdin.ide.fileprovider", apk);
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(uri, "application/vnd.android.package-archive");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    log("🎊 Installer launched");
                } catch (Exception e) { log("❌ " + e.getMessage()); }
            }
        });
    }

    // ============ PROJECT GENERATOR ============
    void genProject(String name) throws Exception {
        String s = name.replaceAll("[^A-Za-z0-9_]", "");
        if (s.isEmpty()) s = "MyApp";
        File proj = new File(projectsDir, s);
        if (proj.exists()) throw new Exception("Project already exists");
        File app = new File(proj, "app");
        File src = new File(app, "src/main/java/com/aladdin/app");
        File lay = new File(app, "src/main/res/layout");
        File val = new File(app, "src/main/res/values");
        src.mkdirs(); lay.mkdirs(); val.mkdirs();

        writeFile(new File(proj, "settings.gradle"),
                "pluginManagement {\n    repositories {\n        google()\n        mavenCentral()\n        gradlePluginPortal()\n    }\n}\n"
                + "dependencyResolutionManagement {\n    repositories {\n        google()\n        mavenCentral()\n    }\n}\n"
                + "rootProject.name = \"" + s + "\"\ninclude ':app'\n");
        writeFile(new File(proj, "build.gradle"),
                "plugins {\n    id 'com.android.application' version '8.2.2' apply false\n}\n");
        writeFile(new File(app, "build.gradle"),
                "plugins {\n    id 'com.android.application'\n}\n\nandroid {\n"
                + "    namespace 'com.aladdin.app'\n    compileSdk 34\n"
                + "    defaultConfig {\n        applicationId 'com.aladdin.app'\n"
                + "        minSdk 24\n        targetSdk 34\n        versionCode 1\n        versionName '1.0'\n    }\n}\n");
        writeFile(new File(app, "src/main/AndroidManifest.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                + "    <application android:label=\"" + s + "\">\n"
                + "        <activity android:name=\".MainActivity\" android:exported=\"true\">\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                + "            </intent-filter>\n"
                + "        </activity>\n"
                + "    </application>\n</manifest>\n");
        writeFile(new File(src, "MainActivity.java"),
                "package com.aladdin.app;\n\nimport android.app.Activity;\nimport android.os.Bundle;\n\n"
                + "public class MainActivity extends Activity {\n"
                + "    @Override\n    protected void onCreate(Bundle b) {\n"
                + "        super.onCreate(b);\n        setContentView(R.layout.activity_main);\n    }\n}\n");
        writeFile(new File(lay, "activity_main.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    android:layout_width=\"match_parent\" android:layout_height=\"match_parent\"\n"
                + "    android:gravity=\"center\" android:orientation=\"vertical\">\n"
                + "    <TextView android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"\n"
                + "        android:text=\"Hello from Aladdin IDE!\" android:textSize=\"24sp\" />\n"
                + "</LinearLayout>\n");
        writeFile(new File(val, "strings.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n"
                + "    <string name=\"app_name\">" + s + "</string>\n</resources>\n");
        writeFile(new File(val, "styles.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n"
                + "    <style name=\"AppTheme\" parent=\"android:style/Theme.Material.Light.NoActionBar\">\n"
                + "    </style>\n</resources>\n");
    }

    void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (consolePanel.getVisibility() == View.VISIBLE) {
            consolePanel.setVisibility(View.GONE);
        } else if (currentFile != null && dirtyFiles.contains(currentFile)) {
            new AlertDialog.Builder(this).setTitle("Unsaved Changes")
                    .setMessage("Save " + currentFile.getName() + "?")
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int w) { saveCurrentFile(); finish(); }
                    })
                    .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int w) { finish(); }
                    })
                    .setNeutralButton("Cancel", null).show();
        } else super.onBackPressed();
    }
}
