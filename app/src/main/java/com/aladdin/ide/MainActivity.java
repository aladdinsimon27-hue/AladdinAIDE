package com.aladdin.ide;

import android.app.*;
import android.graphics.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {

    // UI
    LinearLayout root, activityBar, sidebar, sidebarContent, editorArea;
    LinearLayout tabContainer, consolePanel, mainRow, editorRow, rightPane;
    ScrollView editorScroll, sidebarScroll;
    EditText codeEditor, rightEditor;
    TextView lineNumbers, welcomeScreen, minimap;
    TextView statusLeft, statusRight, statusLang, statusCount;
    TextView tabProblems, tabOutput, tabTerminal, tabDebug, consoleOutput;
    FrameLayout mainFrame;
    android.webkit.WebView webPreview;

    // MANAGERS
    EditorManager editorManager;
    FileManager fileManager;
    BuildManager buildManager;

    Handler autoSaveHandler = new Handler();
    Runnable autoSaveRunnable;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        AppState.projectsDir = new File(getFilesDir(), "projects");
        if (!AppState.projectsDir.exists()) AppState.projectsDir.mkdirs();

        SharedPreferences p = getSharedPreferences("AladdinPrefs", MODE_PRIVATE);
        AppState.githubToken = p.getString("token", "");
        AppState.githubUser = p.getString("user", "");
        AppState.githubRepo = p.getString("repo", "");
        AppState.fontSize = p.getInt("fontSize", 13);
        AppState.wordWrap = p.getBoolean("wordWrap", true);
        AppState.autoClose = p.getBoolean("autoClose", true);
        AppState.lineNumbersOn = p.getBoolean("lineNumbers", true);
        AppState.minimapOn = p.getBoolean("minimap", false);
        AppState.themeId = p.getInt("themeId", 0);
        AppState.accentId = p.getInt("accentId", 0);
        AppState.lineEnding = p.getString("lineEnding", "LF");
        AppState.sidebarVisible = p.getBoolean("sidebarVisible", true);

        AppState.applyTheme();
        AppState.registerTemplates();
        buildUI();
        startAutoSave();
    }

    void startAutoSave() {
        autoSaveRunnable = new Runnable() {
            @Override public void run() {
                if (AppState.currentFile != null && AppState.dirtyFiles.contains(AppState.currentFile)) {
                    try {
                        fileManager.writeFile(AppState.currentFile, codeEditor.getText().toString());
                        AppState.unsavedChanges.remove(AppState.currentFile.getAbsolutePath());
                        AppState.dirtyFiles.remove(AppState.currentFile);
                        fileManager.addTab(AppState.currentFile);
                    } catch (Exception e) {}
                }
                autoSaveHandler.postDelayed(this, 30000);
            }
        };
        autoSaveHandler.postDelayed(autoSaveRunnable, 30000);
    }

    @Override protected void onDestroy() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        super.onDestroy();
    }

    void buildUI() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(AppState.BG);

        // TITLE BAR
        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setBackgroundColor(AppState.SIDEBAR_BG);
        titleBar.setElevation(4);

        TextView menuIcon = UIHelper.tipIcon(this, "☰", 20, "Menu");
        menuIcon.setPadding(15, 12, 12, 12);
        menuIcon.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { MenuManager.showCustomMenu(MainActivity.this, fileManager, editorManager, buildManager, sidebarContent, tabContainer); }
        });
        titleBar.addView(menuIcon);

        TextView brand = UIHelper.tv(this, "⚡ Aladdin", 14);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setTextColor(AppState.ACCENT);
        brand.setPadding(4, 12, 8, 12);
        titleBar.addView(brand);

        View sp1 = new View(this);
        titleBar.addView(sp1, new LinearLayout.LayoutParams(0, 1, 1));

        TextView undoBtn = UIHelper.tipIcon(this, "↶", 18, "Undo");
        undoBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editorManager.performUndo(); }
        });
        titleBar.addView(undoBtn);
        TextView redoBtn = UIHelper.tipIcon(this, "↷", 18, "Redo");
        redoBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editorManager.performRedo(); }
        });
        titleBar.addView(redoBtn);
        TextView fmtBtn = UIHelper.tipIcon(this, "≡", 18, "Format code");
        fmtBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editorManager.formatDocument(); }
        });
        titleBar.addView(fmtBtn);
        TextView cmdBtn = UIHelper.tipIcon(this, "⌘", 18, "Command Palette");
        cmdBtn.setTextColor(AppState.ACCENT);
        cmdBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { MenuManager.showCommandPalette(MainActivity.this, fileManager, editorManager, buildManager, sidebarContent, tabContainer); }
        });
        titleBar.addView(cmdBtn);
        TextView runBtn = UIHelper.tipIcon(this, "▶", 16, "Run Cloud Build");
        runBtn.setTextColor(AppState.ACCENT);
        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (AppState.currentProject == null) { UIHelper.toast(MainActivity.this, "Open a project first"); return; }
                consolePanel.setVisibility(View.VISIBLE);
                buildManager.triggerCloudBuild();
            }
        });
        titleBar.addView(runBtn);
        root.addView(titleBar);

        mainFrame = new FrameLayout(this);
        mainFrame.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        mainRow = new LinearLayout(this);
        mainRow.setOrientation(LinearLayout.HORIZONTAL);
        mainRow.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // ACTIVITY BAR
        activityBar = new LinearLayout(this);
        activityBar.setOrientation(LinearLayout.VERTICAL);
        activityBar.setBackgroundColor(AppState.ACTIVITY_BG);
        activityBar.setPadding(0, 4, 0, 4);
        activityBar.addView(activityIcon("📁", 0, "Explorer"));
        activityBar.addView(activityIcon("🔍", 1, "Search"));
        activityBar.addView(activityIcon("🌐", 2, "Web Dev"));
        activityBar.addView(activityIcon("🎮", 3, "Game Dev"));
        activityBar.addView(activityIcon("📦", 4, "Snippets"));
        activityBar.addView(activityIcon("🧠", 5, "Outline"));
        activityBar.addView(activityIcon("🐛", 6, "Diagnostics"));
        activityBar.addView(activityIcon("⚙", 7, "Settings"));
        View sp2 = new View(this);
        activityBar.addView(sp2, new LinearLayout.LayoutParams(1, 0, 1));
        mainRow.addView(activityBar, new LinearLayout.LayoutParams(48,
            LinearLayout.LayoutParams.MATCH_PARENT));

        // SIDEBAR
        sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setBackgroundColor(AppState.SIDEBAR_BG);
        sidebarScroll = new ScrollView(this);
        sidebarContent = new LinearLayout(this);
        sidebarContent.setOrientation(LinearLayout.VERTICAL);
        sidebarScroll.addView(sidebarContent);
        sidebar.addView(sidebarScroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        sidebar.setVisibility(AppState.sidebarVisible ? View.VISIBLE : View.GONE);
        sidebar.setLayoutParams(new LinearLayout.LayoutParams(
            AppState.sidebarVisible ? 210 : 0, LinearLayout.LayoutParams.MATCH_PARENT));
        mainRow.addView(sidebar);

        // EDITOR
        editorArea = new LinearLayout(this);
        editorArea.setOrientation(LinearLayout.VERTICAL);
        editorArea.setBackgroundColor(AppState.BG);
        editorArea.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.MATCH_PARENT, 1));

        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setBackgroundColor(AppState.PANEL_BG);
        tabContainer = new LinearLayout(this);
        tabContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabScroll.addView(tabContainer);
        editorArea.addView(tabScroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 34));

        welcomeScreen = new TextView(this);
        welcomeScreen.setText("\n\n⚡  ALADDIN IDE  v10.0\n\n─  Modular Edition  ─\n\n" +
            "6-File Architecture:\n" +
            "📄  MainActivity.java\n" +
            "🎨  UIHelper.java\n" +
            "🧠  EditorManager.java\n" +
            "📁  FileManager.java\n" +
            "⚡  BuildManager.java\n" +
            "🎛  PanelManager + MenuManager\n\n" +
            "Everything from v9.0 still works!");
        welcomeScreen.setTextColor(AppState.TEXT_DIM);
        welcomeScreen.setTextSize(12);
        welcomeScreen.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        welcomeScreen.setLineSpacing(4, 1);
        welcomeScreen.setPadding(20, 20, 20, 20);
        editorArea.addView(welcomeScreen, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        editorRow = new LinearLayout(this);
        editorRow.setOrientation(LinearLayout.HORIZONTAL);

        lineNumbers = new TextView(this);
        lineNumbers.setTextColor(AppState.TEXT_DIM);
        lineNumbers.setTextSize(AppState.fontSize);
        lineNumbers.setTypeface(Typeface.MONOSPACE);
        lineNumbers.setPadding(8, 12, 8, 12);
        lineNumbers.setGravity(Gravity.TOP | Gravity.RIGHT);
        lineNumbers.setBackgroundColor(AppState.BG);
        lineNumbers.setMinWidth(45);

        codeEditor = new EditText(this);
        codeEditor.setTextColor(AppState.TEXT);
        codeEditor.setTextSize(AppState.fontSize);
        codeEditor.setTypeface(Typeface.MONOSPACE);
        codeEditor.setGravity(Gravity.TOP | Gravity.LEFT);
        codeEditor.setSingleLine(false);
        codeEditor.setBackgroundColor(AppState.BG);
        codeEditor.setPadding(10, 12, 10, 12);
        codeEditor.setHorizontallyScrolling(!AppState.wordWrap);
        codeEditor.setInputType(InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_MULTI_LINE
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        minimap = new TextView(this);
        minimap.setTextSize(2); minimap.setTextColor(AppState.TEXT_DIM);
        minimap.setTypeface(Typeface.MONOSPACE);
        minimap.setBackgroundColor(AppState.SIDEBAR_BG);
        minimap.setPadding(3, 8, 3, 8);
        minimap.setGravity(Gravity.TOP); minimap.setMaxLines(80);
        minimap.setVisibility(View.GONE);

        // Create managers
        editorManager = new EditorManager(this, codeEditor, lineNumbers, minimap, null, null);
        fileManager = new FileManager(this, sidebarContent, tabContainer, editorManager);

        editorRow.addView(lineNumbers, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        editorRow.addView(codeEditor, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        editorRow.addView(minimap, new LinearLayout.LayoutParams(50,
            LinearLayout.LayoutParams.MATCH_PARENT));

        editorScroll = new ScrollView(this);
        editorScroll.addView(editorRow);
        editorScroll.setVisibility(View.GONE);

        rightPane = new LinearLayout(this);
        rightPane.setOrientation(LinearLayout.VERTICAL);
        rightPane.setVisibility(View.GONE);
        rightEditor = new EditText(this);
        rightEditor.setTextColor(AppState.TEXT);
        rightEditor.setTextSize(AppState.fontSize);
        rightEditor.setTypeface(Typeface.MONOSPACE);
        rightEditor.setBackgroundColor(AppState.BG);
        rightEditor.setPadding(10, 12, 10, 12);
        rightPane.addView(rightEditor, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        webPreview = new android.webkit.WebView(this);
        webPreview.getSettings().setJavaScriptEnabled(true);
        webPreview.setVisibility(View.GONE);
        rightPane.addView(webPreview, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout pair = new LinearLayout(this);
        pair.setOrientation(LinearLayout.HORIZONTAL);
        pair.addView(editorScroll, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.MATCH_PARENT, 1));
        pair.addView(rightPane, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.MATCH_PARENT, 1));
        editorArea.addView(pair, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        mainRow.addView(editorArea);
        mainFrame.addView(mainRow);

        // FLOATING BUTTONS
        LinearLayout floatBtns = new LinearLayout(this);
        floatBtns.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        flp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        flp.setMargins(0, 0, 15, 15);
        floatBtns.setLayoutParams(flp);
        TextView fabUndo = UIHelper.createFloatingButton(this, "↶", AppState.ACCENT);
        fabUndo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editorManager.performUndo(); }
        });
        floatBtns.addView(fabUndo);
        TextView fabRedo = UIHelper.createFloatingButton(this, "↷", AppState.ACCENT);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fp.topMargin = 8;
        fabRedo.setLayoutParams(fp);
        fabRedo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editorManager.performRedo(); }
        });
        floatBtns.addView(fabRedo);
        TextView fabSave = UIHelper.createFloatingButton(this, "💾", AppState.GREEN);
        fabSave.setLayoutParams(fp);
        fabSave.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { fileManager.saveCurrentFile(); }
        });
        floatBtns.addView(fabSave);
        mainFrame.addView(floatBtns);
        root.addView(mainFrame);

        // BOTTOM PANEL
        consolePanel = new LinearLayout(this);
        consolePanel.setOrientation(LinearLayout.VERTICAL);
        consolePanel.setBackgroundColor(0xFF181818);
        consolePanel.setVisibility(View.GONE);
        LinearLayout panelTabs = new LinearLayout(this);
        panelTabs.setBackgroundColor(AppState.SIDEBAR_BG);
        tabProblems = UIHelper.tv(this, "  PROBLEMS", 10);
        tabProblems.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPanelTab(0); }
        });
        panelTabs.addView(tabProblems);
        tabOutput = UIHelper.tv(this, "  OUTPUT", 10);
        tabOutput.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPanelTab(1); }
        });
        panelTabs.addView(tabOutput);
        tabTerminal = UIHelper.tv(this, "  TERMINAL", 10);
        tabTerminal.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPanelTab(2); }
        });
        panelTabs.addView(tabTerminal);
        tabDebug = UIHelper.tv(this, "  DEBUG", 10);
        tabDebug.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPanelTab(3); }
        });
        panelTabs.addView(tabDebug);
        View sp3 = new View(this);
        panelTabs.addView(sp3, new LinearLayout.LayoutParams(0, 1, 1));
        TextView closeP = UIHelper.iconBtn(this, "✕", 13);
        closeP.setPadding(15, 10, 15, 10);
        closeP.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { consolePanel.setVisibility(View.GONE); }
        });
        panelTabs.addView(closeP);
        consolePanel.addView(panelTabs, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 34));
        ScrollView consoleScroll = new ScrollView(this);
        consoleOutput = new TextView(this);
        consoleOutput.setTextColor(AppState.GREEN);
        consoleOutput.setTextSize(11);
        consoleOutput.setTypeface(Typeface.MONOSPACE);
        consoleOutput.setPadding(15, 12, 15, 12);
        consoleScroll.addView(consoleOutput);
        consolePanel.addView(consoleScroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 160));
        root.addView(consolePanel);

        // STATUS BAR
        LinearLayout statusBar = new LinearLayout(this);
        statusBar.setOrientation(LinearLayout.HORIZONTAL);
        statusBar.setBackgroundColor(AppState.ACCENT);
        statusBar.setGravity(Gravity.CENTER_VERTICAL);
        statusLeft = UIHelper.tv(this, "⑂ main", 10);
        statusLeft.setTextColor(Color.WHITE);
        statusLeft.setPadding(12, 6, 8, 6);
        statusBar.addView(statusLeft);
        View sp4 = new View(this);
        statusBar.addView(sp4, new LinearLayout.LayoutParams(0, 1, 1));
        statusCount = UIHelper.tv(this, "", 10);
        statusCount.setTextColor(Color.WHITE);
        statusCount.setPadding(8, 6, 8, 6);
        statusBar.addView(statusCount);
        statusRight = UIHelper.tv(this, "Ln 1, Col 1", 10);
        statusRight.setTextColor(Color.WHITE);
        statusRight.setPadding(8, 6, 8, 6);
        statusBar.addView(statusRight);
        statusLang = UIHelper.tv(this, "Java", 10);
        statusLang.setTextColor(Color.WHITE);
        statusLang.setPadding(8, 6, 8, 6);
        statusBar.addView(statusLang);
        TextView ending = UIHelper.tv(this, AppState.lineEnding, 10);
        ending.setTextColor(Color.WHITE);
        ending.setPadding(8, 6, 12, 6);
        statusBar.addView(ending);
        root.addView(statusBar);

        // Rebuild managers with status text views
        editorManager = new EditorManager(this, codeEditor, lineNumbers, minimap, statusRight, statusCount);
        fileManager = new FileManager(this, sidebarContent, tabContainer, editorManager);
        buildManager = new BuildManager(this, consoleOutput);

        // Editor listeners
        codeEditor.addTextChangedListener(new TextWatcher() {
            boolean self = false;
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                if (AppState.recordingUndo) {
                    String now = s.toString();
                    if (!now.equals(AppState.lastText)) {
                        long t = System.currentTimeMillis();
                        if (t - AppState.lastEditTime > 1500) {
                            AppState.undoStack.add(AppState.lastText);
                            AppState.undoPos.add(codeEditor.getSelectionStart());
                            if (AppState.undoStack.size() > 50) {
                                AppState.undoStack.remove(0);
                                AppState.undoPos.remove(0);
                            }
                            AppState.redoStack.clear(); AppState.redoPos.clear();
                        }
                        AppState.lastEditTime = t; AppState.lastText = now;
                    }
                }
            }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (self || !AppState.autoClose) return;
                if (c > b && st + c <= s.length()) {
                    char ch = s.charAt(st + c - 1);
                    String close = null;
                    if (ch == '{') close = "}";
                    else if (ch == '[') close = "]";
                    else if (ch == '(') close = ")";
                    else if (ch == '"') close = "\"";
                    else if (ch == '\'') close = "'";
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
                if (AppState.lineNumbersOn) editorManager.updateLineNumbers(s.toString());
                editorManager.highlight(s);
                editorManager.updateCursor();
                editorManager.updateCount(s.toString());
                if (AppState.minimapOn) editorManager.updateMinimap(s.toString());
                fileManager.markDirty();
            }
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

        setContentView(root);
        refreshLayout();
        setPanelTab(2);
    }

    TextView activityIcon(String icon, final int index, final String tip) {
        final TextView t = new TextView(this);
        t.setText(icon); t.setTextSize(19);
        t.setGravity(Gravity.CENTER);
        t.setPadding(4, 12, 4, 12);
        t.setTextColor(index == AppState.activeActivity && AppState.sidebarVisible
            ? AppState.TEXT_BRIGHT : AppState.TEXT_DIM);
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (AppState.activeActivity == index && AppState.sidebarVisible)
                    AppState.sidebarVisible = false;
                else { AppState.activeActivity = index; AppState.sidebarVisible = true; }
                refreshLayout();
            }
        });
        t.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { UIHelper.toast(MainActivity.this, tip); return true; }
        });
        return t;
    }

    void refreshLayout() {
        String[] icons = {"📁","🔍","🌐","🎮","📦","🧠","🐛","⚙"};
        for (int i = 0; i < activityBar.getChildCount(); i++) {
            View c = activityBar.getChildAt(i);
            if (c instanceof TextView) {
                TextView t = (TextView) c;
                for (int j = 0; j < icons.length; j++) {
                    if (t.getText().toString().equals(icons[j])) {
                        boolean active = (j == AppState.activeActivity) && AppState.sidebarVisible;
                        t.setTextColor(active ? AppState.TEXT_BRIGHT : AppState.TEXT_DIM);
                        t.setBackgroundColor(active ? AppState.SIDEBAR_BG : 0);
                    }
                }
            }
        }
        if (AppState.sidebarVisible) {
            sidebar.setVisibility(View.VISIBLE);
            sidebar.setLayoutParams(new LinearLayout.LayoutParams(210,
                LinearLayout.LayoutParams.MATCH_PARENT));
            PanelManager.buildPanel(this, sidebarContent, fileManager, editorManager,
                buildManager, tabContainer, AppState.activeActivity);
        } else {
            sidebar.setVisibility(View.GONE);
            sidebar.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT));
        }
    }

    void setPanelTab(int index) {
        AppState.activeBottomTab = index;
        TextView[] tabs = {tabProblems, tabOutput, tabTerminal, tabDebug};
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == null) continue;
            tabs[i].setTextColor(i == index ? AppState.ACCENT_LIGHT : AppState.TEXT_DIM);
            tabs[i].setBackgroundColor(i == index ? 0xFF181818 : AppState.SIDEBAR_BG);
        }
        if (index == 0) consoleOutput.setText("No problems detected.");
        else if (index == 1) consoleOutput.setText("Output log ready.");
        else if (index == 2) consoleOutput.setText("$ _\nAladdin Terminal v10.0");
        else if (index == 3) consoleOutput.setText("Debug console.\nNo session.");
    }

    public EditorManager getEditorManager() { return editorManager; }
    public FileManager getFileManager() { return fileManager; }
    public BuildManager getBuildManager() { return buildManager; }
    public LinearLayout getSidebarContent() { return sidebarContent; }
    public LinearLayout getTabContainer() { return tabContainer; }
    public LinearLayout getConsolePanel() { return consolePanel; }
    public FrameLayout getMainFrame() { return mainFrame; }
    public LinearLayout getRightPane() { return rightPane; }
    public EditText getRightEditor() { return rightEditor; }
    public android.webkit.WebView getWebPreview() { return webPreview; }
    public ScrollView getEditorScroll() { return editorScroll; }
    public TextView getWelcomeScreen() { return welcomeScreen; }
    public TextView getMinimap() { return minimap; }
    public TextView getStatusLeft() { return statusLeft; }
    public TextView getStatusLang() { return statusLang; }
    public void refreshUILayout() { refreshLayout(); }
    public void setPanelTabPublic(int i) { setPanelTab(i); }
}
