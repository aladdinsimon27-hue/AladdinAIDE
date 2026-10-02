package com.aladdin.ide;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.*;
import android.content.pm.PackageManager;
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

    // UI COMPONENTS
    LinearLayout root;
    LinearLayout sidebar;
    LinearLayout editorArea;
    LinearLayout tabContainer;
    LinearLayout consolePanel;
    LinearLayout editorContainer;
    TextView status;
    TextView cursorInfo;
    EditText codeEditor;
    TextView lineNumbers;
    TextView fileTitle;
    TextView consoleOutput;
    TextView welcomeScreen;
    TextView terminalTab;
    TextView problemsTab;
    TextView outputTab;
    TextView fontSizeLabel;

    // FILE MANAGEMENT
    File projectsDir;
    File currentProject;
    File currentFile;
    ArrayList<File> projectList = new ArrayList<File>();
    ArrayList<File> openFiles = new ArrayList<File>();
    ArrayList<File> dirtyFiles = new ArrayList<File>();
    HashMap<String, String> unsavedChanges = new HashMap<String, String>();

    // GITHUB SETTINGS
    String githubToken = "";
    String githubUser = "";
    String githubRepo = "";

    // EDITOR SETTINGS
    int fontSize = 14;
    boolean wordWrap = true;
    boolean autoCloseBrackets = true;

    // VS CODE DARK THEME COLORS
    int BG = Color.parseColor("#1E1E1E");
    int SIDEBAR_BG = Color.parseColor("#252526");
    int PANEL_BG = Color.parseColor("#2D2D2D");
    int ACCENT = Color.parseColor("#007ACC");
    int TEXT = Color.parseColor("#D4D4D4");
    int TEXT_DIM = Color.parseColor("#858585");
    int LINE_NUM_COLOR = Color.parseColor("#858585");
    int LINE_NUM_ACTIVE = Color.parseColor("#C6C6C6");
    int GREEN = Color.parseColor("#4EC9B0");
    int ORANGE = Color.parseColor("#CE9178");
    int BLUE = Color.parseColor("#569CD6");
    int COMMENT_GREEN = Color.parseColor("#6A9955");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectsDir = new File(getFilesDir(), "projects");
        if (!projectsDir.exists()) projectsDir.mkdirs();
        
        SharedPreferences prefs = getSharedPreferences("AladdinPrefs", MODE_PRIVATE);
        githubToken = prefs.getString("token", "");
        githubUser = prefs.getString("user", "");
        githubRepo = prefs.getString("repo", "");
        fontSize = prefs.getInt("fontSize", 14);
        wordWrap = prefs.getBoolean("wordWrap", true);
        autoCloseBrackets = prefs.getBoolean("autoClose", true);
        
        buildIDE();
    }

    // ==============================
    // UI HELPERS
    // ==============================
    TextView text(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(TEXT);
        t.setPadding(20, 15, 20, 15);
        return t;
    }

    Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(TEXT);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(15, 10, 15, 10);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        return b;
    }

    // ==============================
    // BUILD THE IDE
    // ==============================
    void buildIDE() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // --- TOP BAR ---
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(SIDEBAR_BG);
        top.setElevation(8);

        TextView title = text("⚡", 18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(ACCENT);
        title.setPadding(15, 15, 5, 15);
        top.addView(title);

        TextView brand = text("Aladdin", 16);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setTextColor(ACCENT);
        brand.setPadding(0, 15, 10, 15);
        top.addView(brand);

        // Spacer
        View spacer = new View(this);
        top.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        // Font size controls
        Button fontMinus = button("A-");
        top.addView(fontMinus, new LinearLayout.LayoutParams(50, 55));
        fontSizeLabel = text(String.valueOf(fontSize), 11);
        fontSizeLabel.setTextColor(TEXT_DIM);
        fontSizeLabel.setPadding(2, 15, 2, 15);
        top.addView(fontSizeLabel, new LinearLayout.LayoutParams(30, 55));
        Button fontPlus = button("A+");
        top.addView(fontPlus, new LinearLayout.LayoutParams(50, 55));

        Button wrapBtn = button("↵");
        top.addView(wrapBtn, new LinearLayout.LayoutParams(45, 55));

        Button findBtn = button("🔍");
        top.addView(findBtn, new LinearLayout.LayoutParams(45, 55));

        Button settingsBtn = button("⚙");
        top.addView(settingsBtn, new LinearLayout.LayoutParams(50, 55));

        Button newProject = button("+");
        newProject.setTextSize(20);
        newProject.setTextColor(ACCENT);
        top.addView(newProject, new LinearLayout.LayoutParams(45, 55));

        Button build = button("▶");
        build.setTextSize(18);
        build.setTextColor(ACCENT);
        top.addView(build, new LinearLayout.LayoutParams(50, 55));

        root.addView(top);

        // --- MAIN AREA ---
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.HORIZONTAL);

        // SIDEBAR
        sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setBackgroundColor(SIDEBAR_BG);

        LinearLayout explorerHeader = new LinearLayout(this);
        explorerHeader.setOrientation(LinearLayout.HORIZONTAL);
        explorerHeader.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView projectTitle = text("EXPLORER", 11);
        projectTitle.setTextColor(TEXT_DIM);
        projectTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        explorerHeader.addView(projectTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button newFileBtn = button("📄+");
        newFileBtn.setTextSize(11);
        explorerHeader.addView(newFileBtn, new LinearLayout.LayoutParams(55, 40));
        
        Button newFolderBtn = button("📁+");
        newFolderBtn.setTextSize(11);
        explorerHeader.addView(newFolderBtn, new LinearLayout.LayoutParams(55, 40));
        
        Button refreshBtn = button("🔄");
        refreshBtn.setTextSize(11);
        explorerHeader.addView(refreshBtn, new LinearLayout.LayoutParams(45, 40));
        
        sidebar.addView(explorerHeader);

        ScrollView sideScroll = new ScrollView(this);
        sideScroll.addView(sidebar);
        main.addView(sideScroll, new LinearLayout.LayoutParams(200, LinearLayout.LayoutParams.MATCH_PARENT));

        // EDITOR AREA
        editorArea = new LinearLayout(this);
        editorArea.setOrientation(LinearLayout.VERTICAL);

        // Tab Bar
        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setBackgroundColor(PANEL_BG);
        tabContainer = new LinearLayout(this);
        tabContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabScroll.addView(tabContainer);
        editorArea.addView(tabScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 48));

        // Welcome Screen
        welcomeScreen = new TextView(this);
        welcomeScreen.setText("\n\n\n⚡ Aladdin IDE v3.0\n\n" +
                "✨ Professional Edition ✨\n\n" +
                "• Auto-Install APKs after cloud build\n" +
                "• Syntax Highlighting\n" +
                "• Find & Replace\n" +
                "• Auto-Close Brackets\n" +
                "• Word Wrap\n" +
                "• Font Size Controls\n" +
                "• Cursor Position Tracking\n\n" +
                "Open a file to begin coding.");
        welcomeScreen.setTextColor(TEXT_DIM);
        welcomeScreen.setTextSize(14);
        welcomeScreen.setGravity(Gravity.CENTER);
        welcomeScreen.setVisibility(View.VISIBLE);
        editorArea.addView(welcomeScreen, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // Editor Container
        editorContainer = new LinearLayout(this);
        editorContainer.setOrientation(LinearLayout.HORIZONTAL);
        editorContainer.setVisibility(View.GONE);

        lineNumbers = new TextView(this);
        lineNumbers.setTextColor(LINE_NUM_COLOR);
        lineNumbers.setTextSize(fontSize);
        lineNumbers.setTypeface(Typeface.MONOSPACE);
        lineNumbers.setPadding(15, 10, 15, 10);
        lineNumbers.setGravity(Gravity.TOP | Gravity.RIGHT);
        lineNumbers.setBackgroundColor(BG);

        codeEditor = new EditText(this);
        codeEditor.setTextColor(TEXT);
        codeEditor.setTextSize(fontSize);
        codeEditor.setTypeface(Typeface.MONOSPACE);
        codeEditor.setGravity(Gravity.TOP | Gravity.LEFT);
        codeEditor.setSingleLine(false);
        codeEditor.setBackgroundColor(BG);
        codeEditor.setPadding(15, 10, 15, 10);
        codeEditor.setHorizontallyScrolling(!wordWrap);
        codeEditor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // AUTO-CLOSE BRACKETS
        codeEditor.setKeyListener(new android.text.method.KeyListener() {
            @Override public int getInputType() {
                return InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            }
            @Override public boolean onKeyDown(View view, Editable text, int keyCode, KeyEvent event) {
                // Auto-indent on Enter
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    int pos = Selection.getSelectionStart(text);
                    if (pos > 0 && text.charAt(pos - 1) == '{') {
                        text.insert(pos, "\n    ");
                        return true;
                    }
                }
                // Auto-close brackets
                if (autoCloseBrackets && keyCode == KeyEvent.KEYCODE_DEL) {
                    return false;
                }
                return false;
            }
            @Override public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) { return false; }
            @Override public boolean onKeyOther(View view, Editable text, KeyEvent event) { return false; }
            @Override public void clearMetaKeyState(View view, Editable content, int states) {}
        });

        // AUTO-CLOSE BRACKETS via TextWatcher
        codeEditor.addTextChangedListener(new TextWatcher() {
            boolean selfChange = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (selfChange || !autoCloseBrackets) return;
                if (count > before && start + count <= s.length()) {
                    char added = s.charAt(start + count - 1);
                    String closing = null;
                    if (added == '{') closing = "}";
                    else if (added == '[') closing = "]";
                    else if (added == '(') closing = ")";
                    
                    if (closing != null) {
                        selfChange = true;
                        int cursor = codeEditor.getSelectionStart();
                        codeEditor.getText().insert(cursor, closing);
                        codeEditor.setSelection(cursor);
                        selfChange = false;
                    }
                }
            }
            @Override public void afterTextChanged(Editable s) {
                updateLineNumbers(s.toString());
                highlightSyntax(s);
                updateCursorInfo();
                markDirty();
            }
        });

        codeEditor.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { updateCursorInfo(); }
        });

        codeEditor.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                lineNumbers.scrollTo(0, scrollY);
            }
        });

        editorContainer.addView(lineNumbers, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        editorContainer.addView(codeEditor, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        ScrollView editorScroll = new ScrollView(this);
        editorScroll.addView(editorContainer);
        editorArea.addView(editorScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        main.addView(editorArea, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        root.addView(main, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // --- CONSOLE PANEL ---
        consolePanel = new LinearLayout(this);
        consolePanel.setOrientation(LinearLayout.VERTICAL);
        consolePanel.setBackgroundColor(Color.parseColor("#0E0E0E"));
        consolePanel.setVisibility(View.GONE);

        LinearLayout consoleHeader = new LinearLayout(this);
        consoleHeader.setOrientation(LinearLayout.HORIZONTAL);
        consoleHeader.setBackgroundColor(PANEL_BG);

        terminalTab = text("TERMINAL", 11);
        terminalTab.setTextColor(ACCENT);
        terminalTab.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        consoleHeader.addView(terminalTab, new LinearLayout.LayoutParams(90, 40));
        
        problemsTab = text("PROBLEMS", 11);
        problemsTab.setTextColor(TEXT_DIM);
        consoleHeader.addView(problemsTab, new LinearLayout.LayoutParams(90, 40));
        
        outputTab = text("OUTPUT", 11);
        outputTab.setTextColor(TEXT_DIM);
        consoleHeader.addView(outputTab, new LinearLayout.LayoutParams(80, 40));
        
        View spacer2 = new View(this);
        consoleHeader.addView(spacer2, new LinearLayout.LayoutParams(0, 1, 1));
        
        TextView closeConsole = text("✕", 16);
        closeConsole.setTextColor(TEXT_DIM);
        closeConsole.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { consolePanel.setVisibility(View.GONE); }
        });
        consoleHeader.addView(closeConsole, new LinearLayout.LayoutParams(50, 40));

        consolePanel.addView(consoleHeader);

        ScrollView consoleScroll = new ScrollView(this);
        consoleOutput = new TextView(this);
        consoleOutput.setTextColor(Color.parseColor("#4EC9B0"));
        consoleOutput.setTextSize(11);
        consoleOutput.setTypeface(Typeface.MONOSPACE);
        consoleOutput.setPadding(15, 15, 15, 15);
        consoleScroll.addView(consoleOutput);
        consolePanel.addView(consoleScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 230));

        root.addView(consolePanel);

        // --- BOTTOM BAR / STATUS BAR ---
        LinearLayout statusBar = new LinearLayout(this);
        statusBar.setOrientation(LinearLayout.HORIZONTAL);
        statusBar.setBackgroundColor(ACCENT);
        statusBar.setGravity(Gravity.CENTER_VERTICAL);

        status = text("Ready", 11);
        status.setTextColor(Color.WHITE);
        status.setPadding(20, 10, 10, 10);
        statusBar.addView(status, new LinearLayout.LayoutParams(0, 42, 1));

        cursorInfo = text("Ln 1, Col 1", 11);
        cursorInfo.setTextColor(Color.WHITE);
        cursorInfo.setPadding(10, 10, 20, 10);
        statusBar.addView(cursorInfo);

        root.addView(statusBar);

        setContentView(root);
        refreshProjects();

        // ============ BUTTON LISTENERS ============
        fontMinus.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { changeFontSize(-2); }
        });
        fontPlus.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { changeFontSize(2); }
        });
        wrapBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleWordWrap(); }
        });
        findBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showFindReplace(); }
        });
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showSettingsDialog(); }
        });
        newProject.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { createProject(); }
        });
        build.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentProject == null) {
                    Toast.makeText(MainActivity.this, "Open a project first", Toast.LENGTH_SHORT).show();
                    return;
                }
                triggerCloudBuild();
            }
        });
        newFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentProject == null) { Toast.makeText(MainActivity.this, "Open a project first", Toast.LENGTH_SHORT).show(); return; }
                createNewFile(currentProject);
            }
        });
        newFolderBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentProject == null) { Toast.makeText(MainActivity.this, "Open a project first", Toast.LENGTH_SHORT).show(); return; }
                createNewFolder(currentProject);
            }
        });
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { refreshProjects(); }
        });
    }

    // ==============================
    // FONT SIZE
    // ==============================
    void changeFontSize(int delta) {
        fontSize += delta;
        if (fontSize < 8) fontSize = 8;
        if (fontSize > 32) fontSize = 32;
        fontSizeLabel.setText(String.valueOf(fontSize));
        codeEditor.setTextSize(fontSize);
        lineNumbers.setTextSize(fontSize);
        SharedPreferences prefs = getSharedPreferences("AladdinPrefs", MODE_PRIVATE);
        prefs.edit().putInt("fontSize", fontSize).apply();
        Toast.makeText(this, "Font: " + fontSize, Toast.LENGTH_SHORT).show();
    }

    void toggleWordWrap() {
        wordWrap = !wordWrap;
        codeEditor.setHorizontallyScrolling(!wordWrap);
        SharedPreferences prefs = getSharedPreferences("AladdinPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("wordWrap", wordWrap).apply();
        Toast.makeText(this, wordWrap ? "Word wrap: ON" : "Word wrap: OFF", Toast.LENGTH_SHORT).show();
    }

    // ==============================
    // CURSOR INFO
    // ==============================
    void updateCursorInfo() {
        int cursor = codeEditor.getSelectionStart();
        String text = codeEditor.getText().toString();
        int line = 1, col = 1;
        for (int i = 0; i < cursor && i < text.length(); i++) {
            if (text.charAt(i) == '\n') { line++; col = 1; }
            else col++;
        }
        cursorInfo.setText("Ln " + line + ", Col " + col);
    }

    // ==============================
    // DIRTY FILES
    // ==============================
    void markDirty() {
        if (currentFile == null) return;
        if (!dirtyFiles.contains(currentFile)) {
            dirtyFiles.add(currentFile);
            addTab(currentFile);
        }
    }

    void markClean() {
        if (currentFile != null) dirtyFiles.remove(currentFile);
    }

    // ==============================
    // FIND & REPLACE
    // ==============================
    void showFindReplace() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        final EditText findInput = new EditText(this);
        findInput.setHint("Find...");
        layout.addView(findInput);

        final EditText replaceInput = new EditText(this);
        replaceInput.setHint("Replace with...");
        layout.addView(replaceInput);

        new AlertDialog.Builder(this)
                .setTitle("Find & Replace")
                .setView(layout)
                .setPositiveButton("Replace All", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String find = findInput.getText().toString();
                        String replace = replaceInput.getText().toString();
                        if (find.isEmpty()) return;
                        String text = codeEditor.getText().toString();
                        int count = 0;
                        while (text.contains(find)) {
                            text = text.replaceFirst(Pattern.quote(find), Matcher.quoteReplacement(replace));
                            count++;
                            if (count > 1000) break;
                        }
                        codeEditor.setText(text);
                        Toast.makeText(MainActivity.this, "Replaced " + count + " occurrences", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Find Next", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String find = findInput.getText().toString();
                        if (find.isEmpty()) return;
                        String text = codeEditor.getText().toString();
                        int cursor = codeEditor.getSelectionStart();
                        int idx = text.indexOf(find, cursor);
                        if (idx == -1) idx = text.indexOf(find);
                        if (idx >= 0) {
                            codeEditor.setSelection(idx, idx + find.length());
                        } else {
                            Toast.makeText(MainActivity.this, "Not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==============================
    // SETTINGS DIALOG
    // ==============================
    void showSettingsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        final EditText userInput = new EditText(this);
        userInput.setHint("GitHub Username");
        userInput.setText(githubUser);
        layout.addView(userInput);

        final EditText repoInput = new EditText(this);
        repoInput.setHint("Repository Name");
        repoInput.setText(githubRepo);
        layout.addView(repoInput);

        final EditText tokenInput = new EditText(this);
        tokenInput.setHint("Personal Access Token");
        tokenInput.setText(githubToken);
        layout.addView(tokenInput);

        final CheckBox autoCloseBox = new CheckBox(this);
        autoCloseBox.setText("Auto-close brackets");
        autoCloseBox.setTextColor(TEXT);
        autoCloseBox.setChecked(autoCloseBrackets);
        layout.addView(autoCloseBox);

        new AlertDialog.Builder(this)
                .setTitle("⚙ Settings")
                .setView(layout)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        githubUser = userInput.getText().toString().trim();
                        githubRepo = repoInput.getText().toString().trim();
                        githubToken = tokenInput.getText().toString().trim();
                        autoCloseBrackets = autoCloseBox.isChecked();
                        
                        SharedPreferences prefs = getSharedPreferences("AladdinPrefs", MODE_PRIVATE);
                        prefs.edit()
                            .putString("user", githubUser)
                            .putString("repo", githubRepo)
                            .putString("token", githubToken)
                            .putBoolean("autoClose", autoCloseBrackets)
                            .apply();
                        Toast.makeText(MainActivity.this, "✓ Settings Saved", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==============================
    // CONSOLE LOG
    // ==============================
    void logToConsole(String message) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                consoleOutput.append(message + "\n");
            }
        });
    }

    // ==============================
    // CLOUD BUILD ENGINE
    // ==============================
    void triggerCloudBuild() {
        if (githubToken.isEmpty() || githubUser.isEmpty() || githubRepo.isEmpty()) {
            Toast.makeText(this, "Please set GitHub settings first!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }

        consolePanel.setVisibility(View.VISIBLE);
        consoleOutput.setText("");
        logToConsole("🚀 Initializing Cloud Build...");
        logToConsole("📦 Repo: " + githubUser + "/" + githubRepo);
        logToConsole("─────────────────────────────────");

        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    URL url = new URL("https://api.github.com/repos/" + githubUser + "/" + githubRepo + "/actions/workflows/build.yml/dispatches");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Authorization", "token " + githubToken);
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    conn.setDoOutput(true);

                    String jsonBody = "{\"ref\":\"main\"}";
                    OutputStream os = conn.getOutputStream();
                    os.write(jsonBody.getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 204) {
                        logToConsole("✅ Build triggered!");
                        logToConsole("⏳ Waiting for GitHub...");
                        Thread.sleep(15000); 
                        checkBuildStatus();
                    } else {
                        logToConsole("❌ Failed. Code: " + responseCode);
                    }
                } catch (Exception e) {
                    logToConsole("❌ Error: " + e.getMessage());
                }
            }
        }).start();
    }

    void checkBuildStatus() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    boolean finished = false;
                    int pollCount = 0;
                    while (!finished && pollCount < 60) {
                        pollCount++;
                        URL url = new URL("https://api.github.com/repos/" + githubUser + "/" + githubRepo + "/actions/runs?per_page=1");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestProperty("Authorization", "token " + githubToken);
                        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) response.append(line);
                        reader.close();

                        JSONObject json = new JSONObject(response.toString());
                        JSONArray runs = json.getJSONArray("workflow_runs");
                        if (runs.length() > 0) {
                            JSONObject latestRun = runs.getJSONObject(0);
                            String status = latestRun.getString("status");
                            String conclusion = latestRun.optString("conclusion", "pending");

                            logToConsole("⏳ " + status + " ...");

                            if (status.equals("completed")) {
                                finished = true;
                                if (conclusion.equals("success")) {
                                    logToConsole("🎉 BUILD SUCCESSFUL!");
                                    logToConsole("📥 Downloading APK...");
                                    downloadArtifact(latestRun.getLong("id"));
                                } else {
                                    logToConsole("❌ BUILD FAILED.");
                                }
                            }
                        }
                        Thread.sleep(10000);
                    }
                } catch (Exception e) {
                    logToConsole("❌ Error: " + e.getMessage());
                }
            }
        }).start();
    }

    void downloadArtifact(long runId) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    URL url = new URL("https://api.github.com/repos/" + githubUser + "/" + githubRepo + "/actions/runs/" + runId + "/artifacts");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Authorization", "token " + githubToken);
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(response.toString());
                    JSONArray artifacts = json.getJSONArray("artifacts");
                    if (artifacts.length() == 0) {
                        logToConsole("❌ No artifacts found.");
                        return;
                    }
                    
                    String downloadUrl = artifacts.getJSONObject(0).getString("archive_download_url");
                    logToConsole("🌐 Downloading...");
                    
                    URL dlUrl = new URL(downloadUrl);
                    HttpURLConnection dlConn = (HttpURLConnection) dlUrl.openConnection();
                    dlConn.setRequestProperty("Authorization", "token " + githubToken);
                    dlConn.setInstanceFollowRedirects(true);
                    
                    InputStream in = dlConn.getInputStream();
                    
                    File downloadDir = getExternalFilesDir(null);
                    if (downloadDir != null && !downloadDir.exists()) downloadDir.mkdirs();
                    File zipFile = new File(downloadDir, "AladdinIDE-APK.zip");
                    
                    FileOutputStream out = new FileOutputStream(zipFile);
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
                    out.close();
                    in.close();

                    logToConsole("✅ ZIP downloaded!");
                    logToConsole("📦 Extracting APK...");
                    
                    // Extract the APK from the ZIP
                    File apkFile = extractApkFromZip(zipFile, downloadDir);
                    if (apkFile == null) {
                        logToConsole("❌ Failed to extract APK.");
                        return;
                    }
                    
                    logToConsole("✅ APK ready: " + apkFile.getName());
                    logToConsole("📲 Launching installer...");
                    
                    installApk(apkFile);
                    
                } catch (Exception e) {
                    logToConsole("❌ Download error: " + e.getMessage());
                }
            }
        }).start();
    }

    File extractApkFromZip(File zipFile, File outputDir) {
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry entry;
            File apkFile = null;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".apk")) {
                    apkFile = new File(outputDir, "AladdinIDE-latest.apk");
                    FileOutputStream fos = new FileOutputStream(apkFile);
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) != -1) fos.write(buffer, 0, len);
                    fos.close();
                    break;
                }
            }
            zis.close();
            return apkFile;
        } catch (Exception e) {
            logToConsole("❌ Extract error: " + e.getMessage());
            return null;
        }
    }

    void installApk(final File apkFile) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    // Check if we can request install packages
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!getPackageManager().canRequestPackageInstalls()) {
                            logToConsole("⚠ Please allow 'Install unknown apps' for AladdinIDE");
                            Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                            settingsIntent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(settingsIntent);
                            return;
                        }
                    }
                    
                    Uri apkUri = FileProvider.getUriForFile(
                            MainActivity.this,
                            "com.aladdin.ide.fileprovider",
                            apkFile);
                    
                    Intent install = new Intent(Intent.ACTION_VIEW);
                    install.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(install);
                    
                    logToConsole("🎊 Installer launched!");
                } catch (Exception e) {
                    logToConsole("❌ Install error: " + e.getMessage());
                    logToConsole("📍 APK location: " + apkFile.getAbsolutePath());
                }
            }
        });
    }

    // ==============================
    // LINE NUMBERS
    // ==============================
    void updateLineNumbers(String text) {
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') lines++;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            sb.append(i).append("\n");
        }
        lineNumbers.setText(sb.toString());
    }

    // ==============================
    // SYNTAX HIGHLIGHTING
    // ==============================
    void highlightSyntax(Editable editable) {
        int cursorPosition = codeEditor.getSelectionStart();
        ForegroundColorSpan[] oldSpans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : oldSpans) editable.removeSpan(span);

        String text = editable.toString();

        // Strings
        Pattern p1 = Pattern.compile("\"[^\"]*\"");
        Matcher m1 = p1.matcher(text);
        while (m1.find()) {
            editable.setSpan(new ForegroundColorSpan(ORANGE), m1.start(), m1.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Comments
        Pattern p2 = Pattern.compile("//.*");
        Matcher m2 = p2.matcher(text);
        while (m2.find()) {
            editable.setSpan(new ForegroundColorSpan(COMMENT_GREEN), m2.start(), m2.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Keywords
        String[] keywords = {"public", "private", "protected", "class", "void", "int", "String", 
                             "boolean", "if", "else", "for", "while", "return", "new", "import", 
                             "package", "extends", "implements", "final", "static", "abstract",
                             "try", "catch", "throw", "throws", "interface", "enum", "this", "super"};
        for (String keyword : keywords) {
            Pattern kp = Pattern.compile("\\b" + keyword + "\\b");
            Matcher km = kp.matcher(text);
            while (km.find()) {
                editable.setSpan(new ForegroundColorSpan(BLUE), km.start(), km.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // XML tags
        Pattern p3 = Pattern.compile("<[^>]*>");
        Matcher m3 = p3.matcher(text);
        while (m3.find()) {
            editable.setSpan(new ForegroundColorSpan(GREEN), m3.start(), m3.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Numbers
        Pattern p4 = Pattern.compile("\\b\\d+\\b");
        Matcher m4 = p4.matcher(text);
        while (m4.find()) {
            editable.setSpan(new ForegroundColorSpan(Color.parseColor("#B5CEA8")), m4.start(), m4.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        codeEditor.setSelection(cursorPosition);
    }

    // ==============================
    // FILE ICONS
    // ==============================
    String getFileIcon(String name) {
        if (name.endsWith(".java")) return "☕ ";
        if (name.endsWith(".xml")) return "📄 ";
        if (name.endsWith(".gradle")) return "🐘 ";
        if (name.endsWith(".kt")) return "🟣 ";
        if (name.endsWith(".js")) return "🟨 ";
        if (name.endsWith(".html")) return "🌐 ";
        if (name.endsWith(".css")) return "🎨 ";
        if (name.endsWith(".json")) return "📋 ";
        if (name.endsWith(".md")) return "📖 ";
        if (name.endsWith(".txt")) return "📝 ";
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) return "🖼 ";
        if (name.endsWith(".sh")) return "⚡ ";
        return "📃 ";
    }

    // ==============================
    // TABS
    // ==============================
    void addTab(final File file) {
        if (!openFiles.contains(file)) openFiles.add(file);
        tabContainer.removeAllViews();

        for (final File f : openFiles) {
            final boolean isActive = f.equals(currentFile);
            final boolean isDirty = dirtyFiles.contains(f);
            
            LinearLayout tabLayout = new LinearLayout(this);
            tabLayout.setOrientation(LinearLayout.HORIZONTAL);
            tabLayout.setBackgroundColor(isActive ? BG : PANEL_BG);

            View accent = new View(this);
            accent.setBackgroundColor(isActive ? ACCENT : Color.TRANSPARENT);
            tabLayout.addView(accent, new LinearLayout.LayoutParams(3, LinearLayout.LayoutParams.MATCH_PARENT));

            TextView tab = new TextView(this);
            tab.setText((isDirty ? "● " : "") + f.getName() + " ");
            tab.setTextColor(isActive ? TEXT : TEXT_DIM);
            tab.setTextSize(12);
            tab.setPadding(15, 15, 8, 15);

            TextView close = new TextView(this);
            close.setText("✕");
            close.setTextColor(TEXT_DIM);
            close.setTextSize(11);
            close.setPadding(8, 15, 15, 15);

            tab.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { loadFileIntoEditor(f); }
            });

            close.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    openFiles.remove(f);
                    dirtyFiles.remove(f);
                    unsavedChanges.remove(f.getAbsolutePath());
                    if (f.equals(currentFile)) {
                        currentFile = null;
                        codeEditor.setText("");
                        welcomeScreen.setVisibility(View.VISIBLE);
                        editorContainer.setVisibility(View.GONE);
                    }
                    addTab(f);
                }
            });

            tabLayout.addView(tab);
            tabLayout.addView(close);
            tabContainer.addView(tabLayout);
        }
    }

    void openFile(File file) {
        if (currentFile != null && !currentFile.equals(file)) {
            unsavedChanges.put(currentFile.getAbsolutePath(), codeEditor.getText().toString());
        }
        addTab(file);
        loadFileIntoEditor(file);
    }

    void loadFileIntoEditor(File file) {
        try {
            currentFile = file;
            String content = unsavedChanges.containsKey(file.getAbsolutePath()) 
                ? unsavedChanges.get(file.getAbsolutePath()) 
                : readFile(file);
            codeEditor.setText(content);
            fileTitle.setText(file.getName());
            status.setText("  " + file.getName());
            welcomeScreen.setVisibility(View.GONE);
            editorContainer.setVisibility(View.VISIBLE);
            addTab(file);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open file", Toast.LENGTH_SHORT).show();
        }
    }

    void saveCurrentFile() {
        if (currentFile == null) {
            Toast.makeText(this, "No file opened", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            writeFile(currentFile, codeEditor.getText().toString());
            unsavedChanges.remove(currentFile.getAbsolutePath());
            markClean();
            addTab(currentFile);
            status.setText("  ✓ Saved: " + currentFile.getName());
            Toast.makeText(this, "💾 Saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Save error", Toast.LENGTH_SHORT).show();
        }
    }

    String readFile(File file) throws Exception {
        FileInputStream in = new FileInputStream(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = in.read(buffer)) != -1) out.write(buffer, 0, length);
        in.close();
        return out.toString("UTF-8");
    }

    void writeFile(File file, String data) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        FileOutputStream out = new FileOutputStream(file);
        out.write(data.getBytes("UTF-8"));
        out.close();
    }

    void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        f.delete();
    }

    // ==============================
    // PROJECT MANAGEMENT
    // ==============================
    void createProject() {
        final EditText input = new EditText(this);
        input.setHint("Example: MyAwesomeApp");

        new AlertDialog.Builder(this)
                .setTitle("Create Android Project")
                .setView(input)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        String name = input.getText().toString().trim();
                        if (name.length() == 0) {
                            Toast.makeText(MainActivity.this, "Enter a project name", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        try {
                            generateAndroidProject(name);
                            Toast.makeText(MainActivity.this, "✓ Project created!", Toast.LENGTH_LONG).show();
                            status.setText("  Created: " + name);
                            refreshProjects();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    void refreshProjects() {
        if (sidebar == null) return;
        while (sidebar.getChildCount() > 1) sidebar.removeViewAt(1);
        projectList.clear();

        File[] list = projectsDir.listFiles();
        if (list == null) return;

        for (final File p : list) {
            if (!p.isDirectory()) continue;
            projectList.add(p);
            TextView item = text("📱  " + p.getName(), 13);
            item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            item.setTextColor(TEXT);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { openProject(p); }
            });
            item.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    return showContextMenu(p, true);
                }
            });
            sidebar.addView(item);
        }
    }

    void openProject(File project) {
        currentProject = project;
        status.setText("  📂 " + project.getName());
        showFiles(project);
    }

    void showFiles(File project) {
        while (sidebar.getChildCount() > 1) sidebar.removeViewAt(1);
        showDirectory(project, 0);
    }

    void showDirectory(final File directory, int depth) {
        File[] files = directory.listFiles();
        if (files == null) return;

        Arrays.sort(files, new Comparator<File>() {
            @Override public int compare(File f1, File f2) {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });

        for (final File f : files) {
            if (f.getName().startsWith(".")) continue;
            String prefix = f.isDirectory() ? "📁 " : getFileIcon(f.getName());
            TextView item = text(prefix + f.getName(), 12);
            item.setPadding(15 + depth * 15, 10, 10, 10);
            item.setTextColor(f.isDirectory() ? TEXT : TEXT_DIM);

            if (f.isDirectory()) {
                item.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { showFiles(currentProject); }
                });
                item.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) { return showContextMenu(f, true); }
                });
            } else {
                item.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { openFile(f); }
                });
                item.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) { return showContextMenu(f, false); }
                });
            }
            sidebar.addView(item);
            if (f.isDirectory()) showDirectory(f, depth + 1);
        }
    }

    boolean showContextMenu(final File file, final boolean isDirectory) {
        final String[] options = isDirectory 
                ? new String[]{"New File", "New Folder", "Rename", "Delete"} 
                : new String[]{"Rename", "Delete"};
        new AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        String choice = options[which];
                        if (choice.equals("New File")) createNewFile(file);
                        else if (choice.equals("New Folder")) createNewFolder(file);
                        else if (choice.equals("Rename")) renameFile(file);
                        else if (choice.equals("Delete")) deleteFile(file);
                    }
                }).show();
        return true;
    }

    void createNewFile(final File parentDir) {
        final EditText input = new EditText(this);
        input.setHint("filename.java");
        new AlertDialog.Builder(this).setTitle("New File").setView(input)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        File newFile = new File(parentDir, input.getText().toString());
                        try {
                            writeFile(newFile, "");
                            if (currentProject != null) showFiles(currentProject);
                        } catch (Exception e) {}
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    void createNewFolder(final File parentDir) {
        final EditText input = new EditText(this);
        input.setHint("folder_name");
        new AlertDialog.Builder(this).setTitle("New Folder").setView(input)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        File newDir = new File(parentDir, input.getText().toString());
                        if (newDir.mkdirs() && currentProject != null) showFiles(currentProject);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    void renameFile(final File file) {
        final EditText input = new EditText(this);
        input.setText(file.getName());
        new AlertDialog.Builder(this).setTitle("Rename").setView(input)
                .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        File newFile = new File(file.getParent(), input.getText().toString());
                        if (file.renameTo(newFile) && currentProject != null) showFiles(currentProject);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    void deleteFile(final File file) {
        new AlertDialog.Builder(this).setTitle("Delete")
                .setMessage("Delete " + file.getName() + "?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        deleteRecursive(file);
                        if (currentProject != null) showFiles(currentProject);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    void generateAndroidProject(String name) throws Exception {
        String safeName = name.replaceAll("[^A-Za-z0-9_]", "");
        if (safeName.length() == 0) safeName = "MyApp";

        File project = new File(projectsDir, safeName);
        if (project.exists()) throw new Exception("Project already exists");

        File app = new File(project, "app");
        File src = new File(app, "src/main/java/com/aladdin/app");
        File layout = new File(app, "src/main/res/layout");
        File values = new File(app, "src/main/res/values");

        src.mkdirs(); layout.mkdirs(); values.mkdirs();

        writeFile(new File(project, "settings.gradle"), "pluginManagement {\n    repositories {\n        google()\n        mavenCentral()\n        gradlePluginPortal()\n    }\n}\ndependencyResolutionManagement {\n    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)\n    repositories {\n        google()\n        mavenCentral()\n    }\n}\nrootProject.name = \"" + safeName + "\"\ninclude ':app'\n");
        writeFile(new File(project, "build.gradle"), "plugins {\n    id 'com.android.application' version '8.2.2' apply false\n}\n");
        writeFile(new File(app, "build.gradle"), "plugins {\n    id 'com.android.application'\n}\n\nandroid {\n    namespace 'com.aladdin.app'\n    compileSdk 34\n\n    defaultConfig {\n        applicationId 'com.aladdin.app'\n        minSdk 24\n        targetSdk 34\n        versionCode 1\n        versionName '1.0'\n    }\n}\n");
        writeFile(new File(app, "src/main/AndroidManifest.xml"), "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n    <application android:theme=\"@style/AppTheme\" android:label=\"" + safeName + "\">\n        <activity android:name=\".MainActivity\" android:exported=\"true\">\n            <intent-filter>\n                <action android:name=\"android.intent.action.MAIN\" />\n                <category android:name=\"android.intent.category.LAUNCHER\" />\n            </intent-filter>\n        </activity>\n    </application>\n</manifest>\n");
        writeFile(new File(src, "MainActivity.java"), "package com.aladdin.app;\n\nimport android.app.Activity;\nimport android.os.Bundle;\n\npublic class MainActivity extends Activity {\n    @Override\n    protected void onCreate(Bundle savedInstanceState) {\n        super.onCreate(savedInstanceState);\n        setContentView(R.layout.activity_main);\n    }\n}\n");
        writeFile(new File(layout, "activity_main.xml"), "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n    android:layout_width=\"match_parent\" android:layout_height=\"match_parent\"\n    android:gravity=\"center\" android:orientation=\"vertical\">\n    <TextView android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"\n        android:text=\"Hello from Aladdin IDE!\" android:textSize=\"24sp\" />\n</LinearLayout>\n");
        writeFile(new File(values, "strings.xml"), "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n    <string name=\"app_name\">" + safeName + "</string>\n</resources>\n");
        writeFile(new File(values, "styles.xml"), "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n    <style name=\"AppTheme\" parent=\"android:style/Theme.Material.Light.NoActionBar\">\n        <item name=\"android:fontFamily\">sans</item>\n        <item name=\"android:colorAccent\">#3F51B5</item>\n    </style>\n</resources>\n");
    }

    @Override
    public void onBackPressed() {
        if (consolePanel != null && consolePanel.getVisibility() == View.VISIBLE) {
            consolePanel.setVisibility(View.GONE);
        } else if (currentFile != null && dirtyFiles.contains(currentFile)) {
            new AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("Save changes to " + currentFile.getName() + "?")
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int w) {
                            saveCurrentFile();
                            finish();
                        }
                    })
                    .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int w) { finish(); }
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}
