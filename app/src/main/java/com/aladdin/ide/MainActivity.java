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

    // CORE
    LinearLayout root, activityBar, sidebar, sidebarContent, editorArea;
    LinearLayout tabContainer, consolePanel, mainRow;
    ScrollView editorScroll, sidebarScroll;
    EditText codeEditor;
    TextView lineNumbers, welcomeScreen, minimap;
    TextView statusLeft, statusRight, statusLang;
    TextView tabProblems, tabOutput, tabTerminal, tabDebug, consoleOutput;
    int activeBottomTab = 2, activeActivity = 0;
    boolean zenMode = false, searchCase = false, sidebarVisible = true;

    // STATE
    File projectsDir, currentProject, currentFile;
    ArrayList<File> projectList = new ArrayList<File>();
    ArrayList<File> openFiles = new ArrayList<File>();
    ArrayList<File> dirtyFiles = new ArrayList<File>();
    ArrayList<File> recentFiles = new ArrayList<File>();
    ArrayList<File> collapsedFolders = new ArrayList<File>();
    HashMap<String, String> unsavedChanges = new HashMap<String, String>();

    // UNDO STACK
    ArrayList<String> undoStack = new ArrayList<String>();
    ArrayList<Integer> undoPos = new ArrayList<Integer>();
    ArrayList<String> redoStack = new ArrayList<String>();
    ArrayList<Integer> redoPos = new ArrayList<Integer>();
    String lastText = "";
    boolean recordingUndo = true;
    long lastEditTime = 0;

    // AUTOSAVE
    Handler autoSaveHandler = new Handler();
    Runnable autoSaveRunnable;

    // SETTINGS
    String githubToken = "", githubUser = "", githubRepo = "";
    int fontSize = 13;
    boolean wordWrap = true, autoClose = true, lineNumbersOn = true, minimapOn = true;
    String currentTheme = "dark";

    // COLORS
    int BG, SIDEBAR_BG, ACTIVITY_BG, PANEL_BG, TAB_INACTIVE, ACCENT, ACCENT_LIGHT;
    int TEXT, TEXT_BRIGHT, TEXT_DIM, GREEN, ORANGE, BLUE, COMMENT, YELLOW, NUMBER_CLR;

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
        minimapOn = p.getBoolean("minimap", false);
        currentTheme = p.getString("theme", "dark");
        sidebarVisible = p.getBoolean("sidebarVisible", true);
        applyTheme(currentTheme);
        buildUI();
        startAutoSave();
    }

    void startAutoSave() {
        autoSaveRunnable = new Runnable() {
            @Override public void run() {
                silentAutoSave();
                autoSaveHandler.postDelayed(this, 30000);
            }
        };
        autoSaveHandler.postDelayed(autoSaveRunnable, 30000);
    }

    void silentAutoSave() {
        if (currentFile != null && dirtyFiles.contains(currentFile)) {
            try {
                writeFile(currentFile, codeEditor.getText().toString());
                unsavedChanges.remove(currentFile.getAbsolutePath());
                dirtyFiles.remove(currentFile);
                addTab(currentFile);
            } catch (Exception e) {}
        }
    }

    @Override
    protected void onDestroy() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        super.onDestroy();
    }

    void applyTheme(String theme) {
        if (theme.equals("dracula")) {
            BG=Color.parseColor("#282A36"); SIDEBAR_BG=Color.parseColor("#21222C");
            ACTIVITY_BG=Color.parseColor("#191A21"); PANEL_BG=Color.parseColor("#343746");
            TAB_INACTIVE=Color.parseColor("#21222C"); ACCENT=Color.parseColor("#BD93F9");
            ACCENT_LIGHT=Color.parseColor("#FF79C6"); TEXT=Color.parseColor("#F8F8F2");
            TEXT_BRIGHT=Color.parseColor("#FFFFFF"); TEXT_DIM=Color.parseColor("#6272A4");
            GREEN=Color.parseColor("#50FA7B"); ORANGE=Color.parseColor("#FFB86C");
            BLUE=Color.parseColor("#8BE9FD"); COMMENT=Color.parseColor("#6272A4");
            YELLOW=Color.parseColor("#F1FA8C"); NUMBER_CLR=Color.parseColor("#BD93F9");
        } else if (theme.equals("monokai")) {
            BG=Color.parseColor("#272822"); SIDEBAR_BG=Color.parseColor("#1E1F1C");
            ACTIVITY_BG=Color.parseColor("#1E1F1C"); PANEL_BG=Color.parseColor("#3E3D32");
            TAB_INACTIVE=Color.parseColor("#3E3D32"); ACCENT=Color.parseColor("#FD971F");
            ACCENT_LIGHT=Color.parseColor("#FD971F"); TEXT=Color.parseColor("#F8F8F2");
            TEXT_BRIGHT=Color.parseColor("#FFFFFF"); TEXT_DIM=Color.parseColor("#75715E");
            GREEN=Color.parseColor("#A6E22E"); ORANGE=Color.parseColor("#E6DB74");
            BLUE=Color.parseColor("#66D9EF"); COMMENT=Color.parseColor("#75715E");
            YELLOW=Color.parseColor("#E6DB74"); NUMBER_CLR=Color.parseColor("#AE81FF");
        } else if (theme.equals("light")) {
            BG=Color.parseColor("#FFFFFF"); SIDEBAR_BG=Color.parseColor("#F3F3F3");
            ACTIVITY_BG=Color.parseColor("#E7E7E7"); PANEL_BG=Color.parseColor("#ECECEC");
            TAB_INACTIVE=Color.parseColor("#ECECEC"); ACCENT=Color.parseColor("#007ACC");
            ACCENT_LIGHT=Color.parseColor("#0098FF"); TEXT=Color.parseColor("#333333");
            TEXT_BRIGHT=Color.parseColor("#000000"); TEXT_DIM=Color.parseColor("#6C6C6C");
            GREEN=Color.parseColor("#22863A"); ORANGE=Color.parseColor("#032F62");
            BLUE=Color.parseColor("#0000FF"); COMMENT=Color.parseColor("#6A737D");
            YELLOW=Color.parseColor("#795E26"); NUMBER_CLR=Color.parseColor("#098658");
        } else {
            BG=Color.parseColor("#1E1E1E"); SIDEBAR_BG=Color.parseColor("#252526");
            ACTIVITY_BG=Color.parseColor("#333333"); PANEL_BG=Color.parseColor("#2D2D2D");
            TAB_INACTIVE=Color.parseColor("#2D2D2D"); ACCENT=Color.parseColor("#007ACC");
            ACCENT_LIGHT=Color.parseColor("#1F8AD2"); TEXT=Color.parseColor("#CCCCCC");
            TEXT_BRIGHT=Color.parseColor("#FFFFFF"); TEXT_DIM=Color.parseColor("#858585");
            GREEN=Color.parseColor("#4EC9B0"); ORANGE=Color.parseColor("#CE9178");
            BLUE=Color.parseColor("#569CD6"); COMMENT=Color.parseColor("#6A9955");
            YELLOW=Color.parseColor("#DCDCAA"); NUMBER_CLR=Color.parseColor("#B5CEA8");
        }
    }

    // ========== HELPERS ==========
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
        t.setPadding(10, 8, 10, 8);
        return t;
    }

    TextView tipIcon(String icon, int size, final String tip) {
        final TextView t = iconBtn(icon, size);
        t.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { toast(tip); return true; }
        });
        return t;
    }

    Button btn(String s) {
        Button b = new Button(this);
        b.setText(s); b.setTextColor(TEXT); b.setTextSize(12);
        b.setAllCaps(false); b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(12, 8, 12, 8);
        b.setMinWidth(0); b.setMinimumWidth(0);
        return b;
    }

    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }

    // ========== MAIN UI ==========
    void buildUI() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // ===== TITLE BAR (compact phone style) =====
        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setBackgroundColor(SIDEBAR_BG);
        titleBar.setElevation(4);

        // Hamburger menu
        TextView menuIcon = tipIcon("☰", 20, "Menu");
        menuIcon.setPadding(15, 12, 12, 12);
        menuIcon.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showMainMenu(); }
        });
        titleBar.addView(menuIcon);

        TextView brand = tv("⚡ Aladdin", 14);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setTextColor(ACCENT);
        brand.setPadding(4, 12, 8, 12);
        titleBar.addView(brand);

        View sp1 = new View(this);
        titleBar.addView(sp1, new LinearLayout.LayoutParams(0, 1, 1));

        TextView undoBtn = tipIcon("↶", 18, "Undo");
        undoBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { performUndo(); }
        });
        titleBar.addView(undoBtn);

        TextView redoBtn = tipIcon("↷", 18, "Redo");
        redoBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { performRedo(); }
        });
        titleBar.addView(redoBtn);

        TextView fmtBtn = tipIcon("≡", 18, "Format code");
        fmtBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { formatDocument(); }
        });
        titleBar.addView(fmtBtn);

        TextView cmdBtn = tipIcon("⌘", 18, "Command Palette");
        cmdBtn.setTextColor(ACCENT);
        cmdBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showCommandPalette(); }
        });
        titleBar.addView(cmdBtn);

        TextView runBtn = tipIcon("▶", 16, "Run Cloud Build");
        runBtn.setTextColor(ACCENT);
        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentProject == null) { toast("Open a project first"); return; }
                triggerCloudBuild();
            }
        });
        titleBar.addView(runBtn);

        root.addView(titleBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // ===== MAIN ROW =====
        mainRow = new LinearLayout(this);
        mainRow.setOrientation(LinearLayout.HORIZONTAL);
        mainRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // ===== ACTIVITY BAR =====
        activityBar = new LinearLayout(this);
        activityBar.setOrientation(LinearLayout.VERTICAL);
        activityBar.setBackgroundColor(ACTIVITY_BG);
        activityBar.setPadding(0, 4, 0, 4);
        activityBar.addView(activityIcon("📁", 0, "Explorer"));
        activityBar.addView(activityIcon("🔍", 1, "Search"));
        activityBar.addView(activityIcon("⑂", 2, "Source Control"));
        activityBar.addView(activityIcon("📦", 3, "Snippets"));
        activityBar.addView(activityIcon("🧠", 4, "Outline"));
        activityBar.addView(activityIcon("⚙", 5, "Settings"));
        View sp2 = new View(this);
        activityBar.addView(sp2, new LinearLayout.LayoutParams(1, 0, 1));
        mainRow.addView(activityBar, new LinearLayout.LayoutParams(50,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // ===== SIDEBAR =====
        sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setBackgroundColor(SIDEBAR_BG);
        sidebarScroll = new ScrollView(this);
        sidebarContent = new LinearLayout(this);
        sidebarContent.setOrientation(LinearLayout.VERTICAL);
        sidebarScroll.addView(sidebarContent);
        sidebar.addView(sidebarScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout.LayoutParams sidebarParams = new LinearLayout.LayoutParams(
                sidebarVisible ? 230 : 0, LinearLayout.LayoutParams.MATCH_PARENT);
        sidebar.setLayoutParams(sidebarParams);
        sidebar.setVisibility(sidebarVisible ? View.VISIBLE : View.GONE);
        mainRow.addView(sidebar);

        // ===== EDITOR AREA =====
        editorArea = new LinearLayout(this);
        editorArea.setOrientation(LinearLayout.VERTICAL);
        editorArea.setBackgroundColor(BG);
        editorArea.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1));

        // Tab bar (compact)
        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setBackgroundColor(PANEL_BG);
        tabContainer = new LinearLayout(this);
        tabContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabScroll.addView(tabContainer);
        editorArea.addView(tabScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 36));

        // Welcome screen
        welcomeScreen = new TextView(this);
        welcomeScreen.setText("\n\n\n" +
                "⚡  ALADDIN IDE  v7.0\n\n" +
                "─  VS Code Edition  ─\n\n\n" +
                "  ☰   Menu\n" +
                "  ↶↷  Undo / Redo\n" +
                "  ≡   Format\n" +
                "  ⌘   Command Palette\n" +
                "  ▶   Cloud Build\n\n\n" +
                "Tap 📁 to open the sidebar\n" +
                "Tap 📁 again to collapse it\n\n\n" +
                "Open a file to start coding");
        welcomeScreen.setTextColor(TEXT_DIM);
        welcomeScreen.setTextSize(13);
        welcomeScreen.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        welcomeScreen.setLineSpacing(4, 1);
        welcomeScreen.setPadding(20, 40, 20, 20);
        editorArea.addView(welcomeScreen, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // Editor row (line numbers + code + optional minimap)
        LinearLayout editorRow = new LinearLayout(this);
        editorRow.setOrientation(LinearLayout.HORIZONTAL);

        lineNumbers = new TextView(this);
        lineNumbers.setTextColor(TEXT_DIM);
        lineNumbers.setTextSize(fontSize);
        lineNumbers.setTypeface(Typeface.MONOSPACE);
        lineNumbers.setPadding(8, 12, 8, 12);
        lineNumbers.setGravity(Gravity.TOP | Gravity.RIGHT);
        lineNumbers.setBackgroundColor(BG);
        lineNumbers.setMinWidth(40);

        codeEditor = new EditText(this);
        codeEditor.setTextColor(TEXT);
        codeEditor.setTextSize(fontSize);
        codeEditor.setTypeface(Typeface.MONOSPACE);
        codeEditor.setGravity(Gravity.TOP | Gravity.LEFT);
        codeEditor.setSingleLine(false);
        codeEditor.setBackgroundColor(BG);
        codeEditor.setPadding(10, 12, 10, 12);
        codeEditor.setHorizontallyScrolling(!wordWrap);
        codeEditor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        codeEditor.addTextChangedListener(new TextWatcher() {
            boolean self = false;
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                if (recordingUndo) {
                    String now = s.toString();
                    if (!now.equals(lastText)) {
                        long t = System.currentTimeMillis();
                        if (t - lastEditTime > 1500) {
                            undoStack.add(lastText);
                            undoPos.add(codeEditor.getSelectionStart());
                            if (undoStack.size() > 50) { undoStack.remove(0); undoPos.remove(0); }
                            redoStack.clear(); redoPos.clear();
                        }
                        lastEditTime = t;
                        lastText = now;
                    }
                }
            }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (self || !autoClose) return;
                if (c > b && st + c <= s.length()) {
                    char a = s.charAt(st + c - 1);
                    String close = null;
                    if (a == '{') close = "}";
                    else if (a == '[') close = "]";
                    else if (a == '(') close = ")";
                    else if (a == '"') close = "\"";
                    else if (a == '\'') close = "'";
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
                if (minimapOn) updateMinimap(s.toString());
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

        editorRow.addView(lineNumbers, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        editorRow.addView(codeEditor, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        minimap = new TextView(this);
        minimap.setTextSize(2);
        minimap.setTextColor(TEXT_DIM);
        minimap.setTypeface(Typeface.MONOSPACE);
        minimap.setBackgroundColor(SIDEBAR_BG);
        minimap.setPadding(3, 8, 3, 8);
        minimap.setGravity(Gravity.TOP);
        minimap.setMaxLines(80);
        minimap.setVisibility(View.GONE);
        editorRow.addView(minimap, new LinearLayout.LayoutParams(50,
                LinearLayout.LayoutParams.MATCH_PARENT));

        editorScroll = new ScrollView(this);
        editorScroll.addView(editorRow);
        editorScroll.setVisibility(View.GONE);
        editorArea.addView(editorScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        mainRow.addView(editorArea);
        root.addView(mainRow);

        // ===== BOTTOM PANEL =====
        consolePanel = new LinearLayout(this);
        consolePanel.setOrientation(LinearLayout.VERTICAL);
        consolePanel.setBackgroundColor(Color.parseColor("#181818"));
        consolePanel.setVisibility(View.GONE);

        LinearLayout panelTabs = new LinearLayout(this);
        panelTabs.setBackgroundColor(SIDEBAR_BG);
        tabProblems = tv("  PROBLEMS", 10);
        tabProblems.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPanelTab(0); }
        });
        panelTabs.addView(tabProblems);
        tabOutput = tv("  OUTPUT", 10);
        tabOutput.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPanelTab(1); }
        });
        panelTabs.addView(tabOutput);
        tabTerminal = tv("  TERMINAL", 10);
        tabTerminal.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPanelTab(2); }
        });
        panelTabs.addView(tabTerminal);
        tabDebug = tv("  DEBUG", 10);
        tabDebug.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPanelTab(3); }
        });
        panelTabs.addView(tabDebug);
        View sp3 = new View(this);
        panelTabs.addView(sp3, new LinearLayout.LayoutParams(0, 1, 1));
        TextView closeP = iconBtn("✕", 13);
        closeP.setPadding(15, 10, 15, 10);
        closeP.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { consolePanel.setVisibility(View.GONE); }
        });
        panelTabs.addView(closeP);
        consolePanel.addView(panelTabs, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 34));

        ScrollView consoleScroll = new ScrollView(this);
        consoleOutput = new TextView(this);
        consoleOutput.setTextColor(GREEN);
        consoleOutput.setTextSize(11);
        consoleOutput.setTypeface(Typeface.MONOSPACE);
        consoleOutput.setPadding(15, 12, 15, 12);
        consoleScroll.addView(consoleOutput);
        consolePanel.addView(consoleScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 180));
        root.addView(consolePanel);

        // ===== STATUS BAR =====
        LinearLayout statusBar = new LinearLayout(this);
        statusBar.setOrientation(LinearLayout.HORIZONTAL);
        statusBar.setBackgroundColor(ACCENT);
        statusBar.setGravity(Gravity.CENTER_VERTICAL);

        statusLeft = tv("⑂ main", 10);
        statusLeft.setTextColor(Color.WHITE);
        statusLeft.setPadding(12, 6, 8, 6);
        statusBar.addView(statusLeft);

        View sp4 = new View(this);
        statusBar.addView(sp4, new LinearLayout.LayoutParams(0, 1, 1));

        statusRight = tv("Ln 1, Col 1", 10);
        statusRight.setTextColor(Color.WHITE);
        statusRight.setPadding(8, 6, 8, 6);
        statusRight.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showGotoLine(); }
        });
        statusBar.addView(statusRight);

        statusLang = tv("Java", 10);
        statusLang.setTextColor(Color.WHITE);
        statusLang.setPadding(8, 6, 12, 6);
        statusLang.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showLanguagePicker(); }
        });
        statusBar.addView(statusLang);

        root.addView(statusBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        selectActivity(0);
        setPanelTab(2);
    }

    // ========== ACTIVITY BAR TOGGLE LOGIC ==========
    TextView activityIcon(String icon, final int index, final String tip) {
        final TextView t = new TextView(this);
        t.setText(icon);
        t.setTextSize(20);
        t.setGravity(Gravity.CENTER);
        t.setPadding(6, 14, 6, 14);
        t.setTextColor(index == activeActivity && sidebarVisible ? TEXT_BRIGHT : TEXT_DIM);
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (activeActivity == index && sidebarVisible) {
                    // Same icon → collapse sidebar
                    sidebarVisible = false;
                } else {
                    // Different icon → show sidebar and switch
                    activeActivity = index;
                    sidebarVisible = true;
                }
                getSharedPreferences("AladdinPrefs", MODE_PRIVATE)
                        .edit().putBoolean("sidebarVisible", sidebarVisible).apply();
                refreshLayout();
            }
        });
        t.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { toast(tip); return true; }
        });
        return t;
    }

    void refreshLayout() {
        // Update activity icon colors
        String[] icons = {"📁","🔍","⑂","📦","🧠","⚙"};
        for (int i = 0; i < activityBar.getChildCount(); i++) {
            View c = activityBar.getChildAt(i);
            if (c instanceof TextView) {
                TextView t = (TextView) c;
                String s = t.getText().toString();
                for (int j = 0; j < icons.length; j++) {
                    if (s.equals(icons[j])) {
                        boolean active = (j == activeActivity) && sidebarVisible;
                        t.setTextColor(active ? TEXT_BRIGHT : TEXT_DIM);
                        t.setBackgroundColor(active ? SIDEBAR_BG : Color.TRANSPARENT);
                    }
                }
            }
        }
        // Show/hide sidebar
        if (sidebarVisible) {
            sidebar.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    230, LinearLayout.LayoutParams.MATCH_PARENT);
            sidebar.setLayoutParams(lp);
            sidebarContent.removeAllViews();
            if (activeActivity == 0) buildExplorerPanel();
            else if (activeActivity == 1) buildSearchPanel();
            else if (activeActivity == 2) buildSourcePanel();
            else if (activeActivity == 3) buildSnippetsPanel();
            else if (activeActivity == 4) buildOutlinePanel();
            else buildSettingsPanel();
        } else {
            sidebar.setVisibility(View.GONE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT);
            sidebar.setLayoutParams(lp);
        }
    }

    void selectActivity(int index) {
        activeActivity = index;
        sidebarVisible = true;
        getSharedPreferences("AladdinPrefs", MODE_PRIVATE)
                .edit().putBoolean("sidebarVisible", sidebarVisible).apply();
        refreshLayout();
    }

    // ========== MAIN MENU (☰ hamburger) ==========
    void showMainMenu() {
        final String[] items = {
                "📁  New Project",
                "📄  New File",
                "📂  New Folder",
                "💾  Save File",
                "💾  Save All Files",
                "✕  Close Current File",
                "↶  Undo",
                "↷  Redo",
                "≡  Format Document",
                "🔍  Find & Replace",
                "→  Go to Line",
                "🎨  Change Theme",
                "🗺  Toggle Minimap",
                "↩  Toggle Word Wrap",
                "🖥  Toggle Zen Mode",
                "📜  Recent Files",
                "⚙  Settings",
                "ℹ  About Aladdin IDE"
        };
        new AlertDialog.Builder(this)
                .setTitle("☰  Menu")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        executeCommand(items[w].substring(3).trim());
                    }
                }).show();
    }

    // ========== EXPLORER ==========
    void buildExplorerPanel() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(14, 14, 8, 10);

        TextView title = tv("EXPLORER", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_DIM);
        title.setPadding(0, 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView nf = tipIcon("📄+", 12, "New file");
        nf.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentProject == null) { toast("Open a project first"); return; }
                createNewFile(currentProject);
            }
        });
        header.addView(nf);
        TextView nd = tipIcon("📁+", 12, "New folder");
        nd.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentProject == null) { toast("Open a project first"); return; }
                createNewFolder(currentProject);
            }
        });
        header.addView(nd);
        TextView rf = tipIcon("↻", 14, "Refresh");
        rf.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { refreshLayout(); }
        });
        header.addView(rf);
        sidebarContent.addView(header);

        File[] list = projectsDir.listFiles();
        if (list == null || list.length == 0) {
            buildEmptyExplorer();
            return;
        }
        Arrays.sort(list, new Comparator<File>() {
            @Override public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        boolean any = false;
        for (final File p : list) {
            if (!p.isDirectory()) continue;
            any = true;
            projectList.add(p);
            boolean isOpen = p.equals(currentProject);
            String icon = isOpen ? "▾ " : "▸ ";
            TextView item = tv(icon + p.getName(), 12);
            item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            item.setTextColor(isOpen ? TEXT_BRIGHT : TEXT);
            item.setPadding(14, 10, 10, 10);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (currentProject != null && currentProject.equals(p)) {
                        currentProject = null;
                    } else {
                        currentProject = p;
                        statusLeft.setText("⑂ " + p.getName());
                    }
                    refreshLayout();
                }
            });
            item.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    showProjectMenu(p); return true;
                }
            });
            sidebarContent.addView(item);
            if (isOpen) renderTree(p, 1);
        }
        if (!any) buildEmptyExplorer();
    }

    void buildEmptyExplorer() {
        TextView empty = tv("\n\n📭\n\nNo projects yet.\n\nTap below to create your first Android project.", 12);
        empty.setTextColor(TEXT_DIM);
        empty.setPadding(16, 20, 16, 20);
        empty.setGravity(Gravity.CENTER);
        sidebarContent.addView(empty);

        Button btn = btn("+  Create Project");
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(ACCENT);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(14, 10, 14, 10);
        btn.setLayoutParams(p);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { createProject(); }
        });
        sidebarContent.addView(btn);
    }

    void showProjectMenu(final File proj) {
        final String[] opts = {"Open","Delete Project"};
        new AlertDialog.Builder(this).setTitle(proj.getName())
                .setItems(opts, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        if (opts[w].equals("Open")) {
                            currentProject = proj;
                            refreshLayout();
                        } else {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Delete Project")
                                    .setMessage("Delete " + proj.getName() + "?")
                                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                        @Override public void onClick(DialogInterface dd, int ww) {
                                            deleteRec(proj);
                                            if (proj.equals(currentProject)) currentProject = null;
                                            refreshLayout();
                                        }
                                    }).setNegativeButton("Cancel", null).show();
                        }
                    }
                }).show();
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
            String icon = f.isDirectory() ? (collapsed ? "▸ " : "▾ ") : fileIcon(f.getName());
            TextView item = tv(icon + f.getName(), 11);
            item.setPadding(14 + depth * 14, 8, 10, 8);
            item.setTextColor(f.isDirectory() ? TEXT : TEXT_DIM);
            if (f.isDirectory()) {
                item.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (collapsedFolders.contains(f)) collapsedFolders.remove(f);
                        else collapsedFolders.add(f);
                        refreshLayout();
                    }
                });
                item.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) { showContextMenu(f, true); return true; }
                });
            } else {
                if (f.equals(currentFile)) item.setTextColor(TEXT_BRIGHT);
                item.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { openFile(f); }
                });
                item.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) { showContextMenu(f, false); return true; }
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
        if (name.endsWith(".png")||name.endsWith(".jpg")) return "🖼";
        if (name.endsWith(".sh")) return "⚡";
        return "📃";
    }

    // ========== OUTLINE ==========
    void buildOutlinePanel() {
        TextView title = tv("OUTLINE", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_DIM);
        title.setPadding(14, 14, 10, 10);
        sidebarContent.addView(title);

        if (currentFile == null) {
            TextView msg = tv("Open a Java file to see symbols.", 11);
            msg.setTextColor(TEXT_DIM);
            msg.setPadding(14, 12, 14, 12);
            sidebarContent.addView(msg);
            return;
        }
        String text = codeEditor.getText().toString();
        ArrayList<String> symbols = extractSymbols(text);
        if (symbols.isEmpty()) {
            TextView msg = tv("No symbols found.", 11);
            msg.setTextColor(TEXT_DIM);
            msg.setPadding(14, 12, 14, 12);
            sidebarContent.addView(msg);
            return;
        }
        for (final String s : symbols) {
            TextView sym = tv(s, 11);
            sym.setTextColor(s.startsWith("C ") ? ACCENT_LIGHT : TEXT);
            sym.setPadding(14, 8, 10, 8);
            final int line = extractLineNumber(s);
            sym.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (line > 0) gotoLine(line);
                }
            });
            sidebarContent.addView(sym);
        }
    }

    ArrayList<String> extractSymbols(String text) {
        ArrayList<String> out = new ArrayList<String>();
        String[] lines = text.split("\n");
        Pattern classPat = Pattern.compile("(class|interface|enum)\\s+(\\w+)");
        Pattern methodPat = Pattern.compile("(public|private|protected|static|void|int|String|boolean|double|float|long)\\s+[\\w<>\\[\\]]+\\s+(\\w+)\\s*\\(");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher cm = classPat.matcher(line);
            if (cm.find() && !line.startsWith("//")) {
                out.add("C " + cm.group(1) + " " + cm.group(2) + "  L" + (i+1));
                continue;
            }
            Matcher mm = methodPat.matcher(line);
            if (mm.find() && !line.startsWith("//") && !line.contains("new ")) {
                out.add("    ▸ " + mm.group(2) + "()  L" + (i+1));
            }
        }
        return out;
    }

    int extractLineNumber(String s) {
        int idx = s.lastIndexOf(" L");
        if (idx < 0) return -1;
        try { return Integer.parseInt(s.substring(idx + 2).trim()); } catch (Exception e) { return -1; }
    }

    void gotoLine(int line) {
        String text = codeEditor.getText().toString();
        int idx = 0, cur = 1;
        while (cur < line && idx < text.length()) {
            if (text.charAt(idx) == '\n') cur++;
            idx++;
        }
        codeEditor.setSelection(Math.min(idx, text.length()));
    }

    // ========== SEARCH ==========
    void buildSearchPanel() {
        TextView title = tv("SEARCH", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_DIM);
        title.setPadding(14, 14, 10, 10);
        sidebarContent.addView(title);

        final EditText query = new EditText(this);
        query.setHint("Search...");
        query.setTextColor(TEXT);
        query.setHintTextColor(TEXT_DIM);
        query.setTextSize(12);
        query.setBackgroundColor(PANEL_BG);
        query.setPadding(12, 10, 12, 10);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        qp.setMargins(14, 4, 14, 6);
        query.setLayoutParams(qp);
        sidebarContent.addView(query);

        final CheckBox cs = new CheckBox(this);
        cs.setText("Case sensitive");
        cs.setTextColor(TEXT_DIM);
        cs.setTextSize(10);
        cs.setPadding(14, 2, 14, 6);
        sidebarContent.addView(cs);

        Button searchBtn = btn("🔍  Search");
        searchBtn.setTextColor(Color.WHITE);
        searchBtn.setBackgroundColor(ACCENT);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.setMargins(14, 4, 14, 12);
        searchBtn.setLayoutParams(bp);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                searchCase = cs.isChecked();
                String q = query.getText().toString().trim();
                if (q.isEmpty()) return;
                performSearch(q);
            }
        });
        sidebarContent.addView(searchBtn);

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
        ArrayList<String> found = new ArrayList<String>();
        searchInDir(currentProject, q, found);
        TextView header = tv(found.size() + " results", 10);
        header.setTextColor(ACCENT_LIGHT);
        header.setPadding(14, 8, 10, 8);
        results.addView(header);
        for (String s : found) {
            final String[] parts = s.split("\\|");
            TextView r = tv("  " + parts[0] + ":" + parts[1], 10);
            r.setTextColor(TEXT_DIM);
            r.setPadding(18, 6, 10, 6);
            r.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try {
                        File target = new File(currentProject, parts[0]);
                        if (target.exists()) { openFile(target); gotoLine(Integer.parseInt(parts[1])); }
                    } catch (Exception e) {}
                }
            });
            results.addView(r);
        }
    }

    void searchInDir(File dir, String q, ArrayList<String> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) searchInDir(f, q, out);
            else if (f.length() < 500000 && !f.getName().startsWith(".")) {
                try {
                    String content = readFile(f);
                    String[] lines = content.split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        String line = searchCase ? lines[i] : lines[i].toLowerCase();
                        String needle = searchCase ? q : q.toLowerCase();
                        if (line.contains(needle)) {
                            out.add(f.getName() + "|" + (i+1));
                            if (out.size() > 100) return;
                        }
                    }
                } catch (Exception e) {}
            }
        }
    }

    // ========== SOURCE CONTROL ==========
    void buildSourcePanel() {
        TextView title = tv("SOURCE CONTROL", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_DIM);
        title.setPadding(14, 14, 10, 10);
        sidebarContent.addView(title);

        if (githubUser.isEmpty() || githubRepo.isEmpty()) {
            TextView msg = tv("No repository configured.\n\nAdd GitHub username and repo in Settings.", 11);
            msg.setTextColor(TEXT_DIM);
            msg.setPadding(14, 12, 14, 12);
            sidebarContent.addView(msg);
            Button setup = btn("Open Settings");
            setup.setTextColor(Color.WHITE);
            setup.setBackgroundColor(ACCENT);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sp.setMargins(14, 10, 14, 10);
            setup.setLayoutParams(sp);
            setup.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { selectActivity(5); }
            });
            sidebarContent.addView(setup);
            return;
        }
        TextView repo = tv("⑂ " + githubUser + "/" + githubRepo, 12);
        repo.setTextColor(TEXT_BRIGHT);
        repo.setPadding(14, 10, 10, 12);
        sidebarContent.addView(repo);
        Button sync = btn("▶  Trigger Cloud Build");
        sync.setTextColor(Color.WHITE);
        sync.setBackgroundColor(ACCENT);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.setMargins(14, 4, 14, 4);
        sync.setLayoutParams(sp);
        sync.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentProject == null) { toast("Open a project first"); return; }
                triggerCloudBuild();
            }
        });
        sidebarContent.addView(sync);
        Button logs = btn("📋  GitHub Logs");
        logs.setTextColor(TEXT);
        logs.setBackgroundColor(PANEL_BG);
        logs.setLayoutParams(sp);
        logs.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/" + githubUser + "/" + githubRepo + "/actions")));
            }
        });
        sidebarContent.addView(logs);
        TextView h = tv("Modified Files", 10);
        h.setTextColor(ACCENT_LIGHT);
        h.setPadding(14, 16, 10, 4);
        sidebarContent.addView(h);
        if (dirtyFiles.isEmpty()) {
            TextView none = tv("  No unsaved changes", 10);
            none.setTextColor(TEXT_DIM);
            none.setPadding(14, 4, 10, 4);
            sidebarContent.addView(none);
        } else {
            for (File d : dirtyFiles) {
                TextView f = tv("  ● " + d.getName(), 11);
                f.setTextColor(YELLOW);
                f.setPadding(14, 4, 10, 4);
                sidebarContent.addView(f);
            }
        }
    }

    // ========== SNIPPETS ==========
    void buildSnippetsPanel() {
        TextView title = tv("SNIPPETS", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_DIM);
        title.setPadding(14, 14, 10, 10);
        sidebarContent.addView(title);
        TextView help = tv("Tap to insert at cursor.", 10);
        help.setTextColor(TEXT_DIM);
        help.setPadding(14, 4, 14, 12);
        sidebarContent.addView(help);
        addSnippet("☕ Java — Activity", "package com.example;\n\nimport android.app.Activity;\nimport android.os.Bundle;\n\npublic class MyActivity extends Activity {\n    @Override\n    protected void onCreate(Bundle savedInstanceState) {\n        super.onCreate(savedInstanceState);\n    }\n}\n");
        addSnippet("☕ Java — For Loop", "for (int i = 0; i < 10; i++) {\n    \n}\n");
        addSnippet("☕ Java — Try/Catch", "try {\n    \n} catch (Exception e) {\n    e.printStackTrace();\n}\n");
        addSnippet("☕ Java — Toast", "Toast.makeText(this, \"Hello\", Toast.LENGTH_SHORT).show();\n");
        addSnippet("📄 XML — LinearLayout", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n    android:layout_width=\"match_parent\"\n    android:layout_height=\"match_parent\"\n    android:orientation=\"vertical\">\n    \n</LinearLayout>\n");
        addSnippet("📄 XML — TextView", "<TextView\n    android:layout_width=\"wrap_content\"\n    android:layout_height=\"wrap_content\"\n    android:text=\"Hello\" />\n");
        addSnippet("📄 XML — Button", "<Button\n    android:layout_width=\"wrap_content\"\n    android:layout_height=\"wrap_content\"\n    android:text=\"Click Me\" />\n");
        addSnippet("🐘 Gradle — App", "plugins {\n    id 'com.android.application'\n}\n\nandroid {\n    namespace 'com.example.app'\n    compileSdk 34\n    defaultConfig {\n        applicationId 'com.example.app'\n        minSdk 24\n        targetSdk 34\n        versionCode 1\n        versionName '1.0'\n    }\n}\n");
        addSnippet("📋 JSON — Object", "{\n    \"key\": \"value\",\n    \"number\": 42\n}\n");
    }

    void addSnippet(final String name, final String code) {
        TextView item = tv(name, 11);
        item.setTextColor(TEXT);
        item.setPadding(14, 12, 10, 12);
        item.setBackgroundColor(PANEL_BG);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(14, 3, 14, 3);
        item.setLayoutParams(p);
        item.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                int start = codeEditor.getSelectionStart();
                codeEditor.getText().insert(start, code);
                toast("Inserted");
            }
        });
        sidebarContent.addView(item);
    }

    // ========== SETTINGS ==========
    void buildSettingsPanel() {
        TextView title = tv("SETTINGS", 10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_DIM);
        title.setPadding(14, 14, 10, 10);
        sidebarContent.addView(title);

        settingsSection("GITHUB");
        final EditText uIn = settingsField("Username", githubUser);
        final EditText rIn = settingsField("Repository", githubRepo);
        final EditText tIn = settingsField("Token", githubToken);

        settingsSection("APPEARANCE");
        Button themeBtn = btn("🎨  Theme: " + currentTheme);
        themeBtn.setTextColor(TEXT);
        themeBtn.setBackgroundColor(PANEL_BG);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(14, 4, 14, 4);
        themeBtn.setLayoutParams(tp);
        themeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showThemePicker(); }
        });
        sidebarContent.addView(themeBtn);

        final EditText fsIn = settingsField("Font size (8-30)", String.valueOf(fontSize));

        settingsSection("EDITOR");
        final CheckBox wrapBox = settingsCheck("Word wrap", wordWrap);
        final CheckBox acBox = settingsCheck("Auto-close brackets", autoClose);
        final CheckBox lnBox = settingsCheck("Show line numbers", lineNumbersOn);
        final CheckBox mmBox = settingsCheck("Show minimap", minimapOn);

        settingsSection("ABOUT");
        TextView about = tv("Aladdin IDE v7.0\nVS Code Edition\n\nTermux + GitHub Actions", 10);
        about.setTextColor(TEXT_DIM);
        about.setPadding(14, 8, 14, 16);
        sidebarContent.addView(about);

        Button save = btn("💾  Save Settings");
        save.setTextColor(Color.WHITE);
        save.setBackgroundColor(ACCENT);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.setMargins(14, 8, 14, 12);
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
                minimapOn = mmBox.isChecked();
                codeEditor.setTextSize(fontSize);
                lineNumbers.setTextSize(fontSize);
                codeEditor.setHorizontallyScrolling(!wordWrap);
                lineNumbers.setVisibility(lineNumbersOn ? View.VISIBLE : View.GONE);
                minimap.setVisibility(minimapOn ? View.VISIBLE : View.GONE);
                getSharedPreferences("AladdinPrefs", MODE_PRIVATE).edit()
                        .putString("user", githubUser).putString("repo", githubRepo)
                        .putString("token", githubToken).putInt("fontSize", fontSize)
                        .putBoolean("wordWrap", wordWrap).putBoolean("autoClose", autoClose)
                        .putBoolean("lineNumbers", lineNumbersOn).putBoolean("minimap", minimapOn)
                        .apply();
                toast("✓ Settings saved");
            }
        });
        sidebarContent.addView(save);
    }

    void settingsSection(String name) {
        TextView t = tv(name, 9);
        t.setTextColor(ACCENT_LIGHT);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(14, 16, 10, 4);
        sidebarContent.addView(t);
    }

    EditText settingsField(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setText(value);
        e.setTextColor(TEXT); e.setHintTextColor(TEXT_DIM);
        e.setTextSize(11); e.setBackgroundColor(PANEL_BG);
        e.setPadding(12, 10, 12, 10);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(14, 3, 14, 3);
        e.setLayoutParams(p);
        sidebarContent.addView(e);
        return e;
    }

    CheckBox settingsCheck(String label, boolean checked) {
        CheckBox c = new CheckBox(this);
        c.setText(label); c.setTextColor(TEXT); c.setTextSize(11);
        c.setChecked(checked); c.setPadding(14, 3, 14, 3);
        sidebarContent.addView(c);
        return c;
    }

    // ========== COMMAND PALETTE ==========
    void showCommandPalette() {
        final String[] commands = {
                "📁  New Project", "📄  New File", "📂  New Folder",
                "💾  Save File", "💾  Save All Files", "✕  Close Current File",
                "↶  Undo", "↷  Redo", "≡  Format Document",
                "🔍  Find & Replace", "→  Go to Line",
                "📦  Insert Snippet", "🧠  Symbol Outline",
                "▶  Trigger Cloud Build", "🌐  View GitHub Actions",
                "🎨  Change Theme", "🗺  Toggle Minimap",
                "↩  Toggle Word Wrap", "🖥  Toggle Zen Mode",
                "📜  Recent Files", "⚙  Settings",
                "ℹ  About Aladdin IDE", "🔄  Refresh Explorer",
                "📋  Copy File Path", "🗑  Clear Terminal"
        };
        new AlertDialog.Builder(this)
                .setTitle("⌘  Command Palette")
                .setItems(commands, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        executeCommand(commands[w].substring(3).trim());
                    }
                }).show();
    }

    void executeCommand(String cmd) {
        if (cmd.equals("New Project")) createProject();
        else if (cmd.equals("New File")) {
            if (currentProject == null) { toast("Open a project first"); return; }
            createNewFile(currentProject);
        }
        else if (cmd.equals("New Folder")) {
            if (currentProject == null) { toast("Open a project first"); return; }
            createNewFolder(currentProject);
        }
        else if (cmd.equals("Save File")) saveCurrentFile();
        else if (cmd.equals("Save All Files")) saveAll();
        else if (cmd.equals("Close Current File")) closeCurrentFile();
        else if (cmd.equals("Undo")) performUndo();
        else if (cmd.equals("Redo")) performRedo();
        else if (cmd.equals("Format Document")) formatDocument();
        else if (cmd.equals("Find & Replace")) showFindReplace();
        else if (cmd.equals("Go to Line")) showGotoLine();
        else if (cmd.equals("Insert Snippet")) selectActivity(3);
        else if (cmd.equals("Symbol Outline")) selectActivity(4);
        else if (cmd.equals("Trigger Cloud Build")) {
            if (currentProject == null) { toast("Open a project first"); return; }
            triggerCloudBuild();
        }
        else if (cmd.equals("View GitHub Actions")) {
            if (githubUser.isEmpty()) { toast("Set GitHub first"); return; }
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/" + githubUser + "/" + githubRepo + "/actions")));
        }
        else if (cmd.equals("Change Theme")) showThemePicker();
        else if (cmd.equals("Toggle Minimap")) {
            minimapOn = !minimapOn;
            minimap.setVisibility(minimapOn ? View.VISIBLE : View.GONE);
            toast("Minimap " + (minimapOn ? "on" : "off"));
        }
        else if (cmd.equals("Toggle Word Wrap")) {
            wordWrap = !wordWrap;
            codeEditor.setHorizontallyScrolling(!wordWrap);
            toast("Word wrap " + (wordWrap ? "on" : "off"));
        }
        else if (cmd.equals("Toggle Zen Mode")) toggleZen();
        else if (cmd.equals("Recent Files")) showRecentFiles();
        else if (cmd.equals("Settings")) selectActivity(5);
        else if (cmd.equals("About Aladdin IDE")) showAbout();
        else if (cmd.equals("Refresh Explorer")) { refreshLayout(); toast("Refreshed"); }
        else if (cmd.equals("Copy File Path")) {
            if (currentFile == null) { toast("No file open"); return; }
            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("path", currentFile.getAbsolutePath()));
            toast("Path copied");
        }
        else if (cmd.equals("Clear Terminal")) { consoleOutput.setText(""); toast("Cleared"); }
    }

    void showRecentFiles() {
        if (recentFiles.isEmpty()) { toast("No recent files"); return; }
        String[] names = new String[recentFiles.size()];
        for (int i = 0; i < recentFiles.size(); i++) names[i] = recentFiles.get(i).getName();
        new AlertDialog.Builder(this).setTitle("Recent Files")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        File f = recentFiles.get(w);
                        if (f.exists()) openFile(f);
                        else toast("File no longer exists");
                    }
                }).show();
    }

    // ========== UNDO/REDO/FORMAT ==========
    void performUndo() {
        if (undoStack.isEmpty()) { toast("Nothing to undo"); return; }
        redoStack.add(codeEditor.getText().toString());
        redoPos.add(codeEditor.getSelectionStart());
        String prev = undoStack.remove(undoStack.size() - 1);
        int pos = undoPos.remove(undoPos.size() - 1);
        recordingUndo = false;
        codeEditor.setText(prev);
        try { codeEditor.setSelection(Math.min(pos, prev.length())); } catch (Exception e) {}
        recordingUndo = true;
        lastText = prev;
        toast("↶ Undo");
    }

    void performRedo() {
        if (redoStack.isEmpty()) { toast("Nothing to redo"); return; }
        undoStack.add(codeEditor.getText().toString());
        undoPos.add(codeEditor.getSelectionStart());
        String next = redoStack.remove(redoStack.size() - 1);
        int pos = redoPos.remove(redoPos.size() - 1);
        recordingUndo = false;
        codeEditor.setText(next);
        try { codeEditor.setSelection(Math.min(pos, next.length())); } catch (Exception e) {}
        recordingUndo = true;
        lastText = next;
        toast("↷ Redo");
    }

    void formatDocument() {
        if (currentFile == null) { toast("No file open"); return; }
        String text = codeEditor.getText().toString();
        StringBuilder out = new StringBuilder();
        int indent = 0;
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("}")) indent = Math.max(0, indent - 1);
            StringBuilder pad = new StringBuilder();
            for (int i = 0; i < indent; i++) pad.append("    ");
            out.append(pad).append(trimmed).append("\n");
            int opens = count(trimmed, '{');
            int closes = count(trimmed, '}');
            indent += opens - closes;
            if (indent < 0) indent = 0;
        }
        codeEditor.setText(out.toString());
        toast("≡ Formatted");
    }

    int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }

    void showGotoLine() {
        final EditText input = new EditText(this);
        input.setHint("Line number");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle("Go to Line").setView(input)
                .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        try {
                            int line = Integer.parseInt(input.getText().toString());
                            gotoLine(line);
                        } catch (Exception e) {}
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    void showLanguagePicker() {
        final String[] langs = {"Java","XML","Groovy","JSON","Markdown","Plain"};
        new AlertDialog.Builder(this).setTitle("Language")
                .setItems(langs, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        statusLang.setText(langs[w]);
                        toast("Language: " + langs[w]);
                    }
                }).show();
    }

    // ========== BOTTOM PANEL ==========
    void setPanelTab(int index) {
        activeBottomTab = index;
        TextView[] tabs = {tabProblems, tabOutput, tabTerminal, tabDebug};
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == null) continue;
            tabs[i].setTextColor(i == index ? ACCENT_LIGHT : TEXT_DIM);
            tabs[i].setBackgroundColor(i == index ? Color.parseColor("#181818") : SIDEBAR_BG);
        }
        if (index == 0) consoleOutput.setText("No problems detected.");
        else if (index == 1) consoleOutput.setText("Output log ready.");
        else if (index == 2) consoleOutput.setText("$ _\nAladdin Terminal v7.0");
        else if (index == 3) consoleOutput.setText("Debug console.\nNo active session.");
    }

    // ========== MINIMAP ==========
    void updateMinimap(String text) {
        if (!minimapOn) return;
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        int max = Math.min(lines.length, 80);
        for (int i = 0; i < max; i++) {
            String line = lines[i];
            int len = Math.min(line.length() / 3, 20);
            for (int j = 0; j < len; j++) sb.append("█");
            sb.append("\n");
        }
        minimap.setText(sb.toString());
    }

    // ========== FILE OPS ==========
    void openFile(File f) {
        if (currentFile != null && !currentFile.equals(f))
            unsavedChanges.put(currentFile.getAbsolutePath(), codeEditor.getText().toString());
        if (!recentFiles.contains(f)) {
            recentFiles.add(0, f);
            if (recentFiles.size() > 10) recentFiles.remove(10);
        } else {
            recentFiles.remove(f);
            recentFiles.add(0, f);
        }
        addTab(f);
        loadFile(f);
    }

    void loadFile(File f) {
        try {
            currentFile = f;
            String content = unsavedChanges.containsKey(f.getAbsolutePath())
                    ? unsavedChanges.get(f.getAbsolutePath()) : readFile(f);
            recordingUndo = false;
            codeEditor.setText(content);
            lastText = content;
            undoStack.clear(); redoStack.clear();
            recordingUndo = true;
            welcomeScreen.setVisibility(View.GONE);
            editorScroll.setVisibility(View.VISIBLE);
            addTab(f);
            updateCursor();
            if (minimapOn) updateMinimap(content);
            statusLeft.setText("⑂ " + (currentProject != null ? currentProject.getName() : ""));
        } catch (Exception e) { toast("Cannot open"); }
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

    void saveAll() {
        for (File f : new ArrayList<File>(dirtyFiles)) {
            try {
                if (f.equals(currentFile)) writeFile(f, codeEditor.getText().toString());
                else if (unsavedChanges.containsKey(f.getAbsolutePath()))
                    writeFile(f, unsavedChanges.get(f.getAbsolutePath()));
            } catch (Exception e) {}
        }
        dirtyFiles.clear();
        if (currentFile != null) addTab(currentFile);
        toast("All saved");
    }

    void closeCurrentFile() {
        if (currentFile == null) return;
        openFiles.remove(currentFile);
        dirtyFiles.remove(currentFile);
        unsavedChanges.remove(currentFile.getAbsolutePath());
        currentFile = null;
        codeEditor.setText("");
        welcomeScreen.setVisibility(View.VISIBLE);
        editorScroll.setVisibility(View.GONE);
        addTab(null);
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
        }
    }

    void addTab(final File file) {
        if (file != null && !openFiles.contains(file)) openFiles.add(file);
        tabContainer.removeAllViews();
        for (final File f : new ArrayList<File>(openFiles)) {
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
            name.setTextSize(10);
            name.setPadding(12, 10, 8, 10);
            final TextView close = new TextView(this);
            close.setText("✕");
            close.setTextColor(TEXT_DIM);
            close.setTextSize(10);
            close.setPadding(4, 10, 12, 10);
            name.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { loadFile(f); }
            });
            name.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) { showTabMenu(f); return true; }
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
                    }
                    addTab(null);
                }
            });
            tl.addView(name); tl.addView(close);
            tabContainer.addView(tl);
        }
    }

    void showTabMenu(final File f) {
        final String[] opts = {"Close","Close Others","Close All"};
        new AlertDialog.Builder(this).setTitle(f.getName())
                .setItems(opts, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        if (opts[w].equals("Close")) {
                            openFiles.remove(f);
                            dirtyFiles.remove(f);
                            unsavedChanges.remove(f.getAbsolutePath());
                            if (f.equals(currentFile)) {
                                currentFile = null;
                                codeEditor.setText("");
                                welcomeScreen.setVisibility(View.VISIBLE);
                                editorScroll.setVisibility(View.GONE);
                            }
                            addTab(null);
                        } else if (opts[w].equals("Close Others")) {
                            ArrayList<File> keep = new ArrayList<File>();
                            keep.add(f);
                            openFiles = keep;
                            if (!f.equals(currentFile)) loadFile(f);
                            addTab(f);
                        } else {
                            openFiles.clear();
                            dirtyFiles.clear();
                            unsavedChanges.clear();
                            currentFile = null;
                            codeEditor.setText("");
                            welcomeScreen.setVisibility(View.VISIBLE);
                            editorScroll.setVisibility(View.GONE);
                            addTab(null);
                        }
                    }
                }).show();
    }

    // ========== PROJECT MGMT ==========
    void createProject() {
        final EditText in = new EditText(this);
        in.setHint("MyAwesomeApp");
        new AlertDialog.Builder(this).setTitle("Create New Project").setView(in)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String n = in.getText().toString().trim();
                        if (n.isEmpty()) { toast("Enter a name"); return; }
                        try {
                            genProject(n);
                            currentProject = new File(projectsDir, n.replaceAll("[^A-Za-z0-9_]", ""));
                            sidebarVisible = true;
                            selectActivity(0);
                            toast("✓ Project created");
                        } catch (Exception e) { toast(e.getMessage()); }
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    void showContextMenu(final File f, final boolean isDir) {
        final String[] opts = isDir
                ? new String[]{"New File","New Folder","Rename","Delete"}
                : new String[]{"Open","Rename","Delete"};
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
                        try { writeFile(new File(p, in.getText().toString()), ""); refreshLayout(); } catch (Exception e) {}
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    void createNewFolder(final File p) {
        final EditText in = new EditText(this);
        in.setHint("folder");
        new AlertDialog.Builder(this).setTitle("New Folder").setView(in)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        new File(p, in.getText().toString()).mkdirs(); refreshLayout();
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
                        refreshLayout();
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    void deleteFile(final File f) {
        new AlertDialog.Builder(this).setTitle("Delete").setMessage("Delete " + f.getName() + "?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        deleteRec(f); refreshLayout();
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    // ========== SYNTAX & CURSOR ==========
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
        statusRight.setText("Ln " + line + ", Col " + col);
    }

    // ========== FIND & REPLACE ==========
    void showFindReplace() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(20, 20, 20, 20);
        final EditText f = new EditText(this); f.setHint("Find..."); ll.addView(f);
        final EditText r = new EditText(this); r.setHint("Replace..."); ll.addView(r);
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

    // ========== THEME, ABOUT, ZEN ==========
    void toggleZen() {
        zenMode = !zenMode;
        activityBar.setVisibility(zenMode ? View.GONE : View.VISIBLE);
        sidebar.setVisibility(zenMode ? View.GONE : View.VISIBLE);
        toast(zenMode ? "Zen Mode on" : "Zen Mode off");
    }

    void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("About Aladdin IDE")
                .setMessage("⚡ Aladdin IDE v7.0\n\nVS Code Edition\n\n" +
                        "Built on Android with:\n" +
                        "• Termux (build environment)\n" +
                        "• GitHub Actions (cloud build)\n" +
                        "• Pure Java UI\n\n" +
                        "Features:\n" +
                        "✓ Toggle sidebar (tap icon twice)\n" +
                        "✓ 4 themes\n" +
                        "✓ Command palette\n" +
                        "✓ Undo/Redo + Auto-save\n" +
                        "✓ Format Document\n" +
                        "✓ Symbol outline\n" +
                        "✓ Cloud build + auto-install")
                .setPositiveButton("Close", null).show();
    }

    void showThemePicker() {
        final String[] themes = {"dark","dracula","monokai","light"};
        new AlertDialog.Builder(this).setTitle("Theme")
                .setItems(themes, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        currentTheme = themes[w];
                        applyTheme(currentTheme);
                        getSharedPreferences("AladdinPrefs", MODE_PRIVATE).edit()
                                .putString("theme", currentTheme).apply();
                        toast("Theme: " + currentTheme);
                        // Restart activity to apply fully
                        finish();
                        startActivity(getIntent());
                    }
                }).show();
    }

    // ========== BUILD ENGINE ==========
    void log(String m) {
        runOnUiThread(new Runnable() {
            @Override public void run() { consoleOutput.append(m + "\n"); }
        });
    }

    void triggerCloudBuild() {
        if (githubToken.isEmpty() || githubUser.isEmpty() || githubRepo.isEmpty()) {
            toast("Set GitHub settings first!"); selectActivity(5); return;
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
                        log("⏳ Waiting...");
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
                    boolean done = false; int p = 0;
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
                    byte[] buf = new byte[8192]; int n;
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
            ZipEntry en; File apk = null;
            while ((en = z.getNextEntry()) != null) {
                if (en.getName().endsWith(".apk")) {
                    apk = new File(out, "AladdinIDE-latest.apk");
                    FileOutputStream fo = new FileOutputStream(apk);
                    byte[] b = new byte[8192]; int n;
                    while ((n = z.read(b)) != -1) fo.write(b, 0, n);
                    fo.close(); break;
                }
            }
            z.close(); return apk;
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
                            startActivity(i); return;
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

    // ========== PROJECT GENERATOR ==========
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

    @Override
    public void onBackPressed() {
        if (consolePanel.getVisibility() == View.VISIBLE) consolePanel.setVisibility(View.GONE);
        else if (sidebarVisible && !zenMode) {
            sidebarVisible = false;
            refreshLayout();
        }
        else if (currentFile != null && dirtyFiles.contains(currentFile)) {
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
