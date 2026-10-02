package com.aladdin.ide;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.*;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends Activity {

    // UI COMPONENTS
    LinearLayout root;
    LinearLayout sidebar;
    LinearLayout editorArea;
    LinearLayout tabContainer;
    LinearLayout consolePanel;
    TextView status;
    EditText codeEditor;
    TextView lineNumbers;
    TextView fileTitle;
    TextView consoleOutput;
    TextView welcomeScreen;

    // FILE MANAGEMENT
    File projectsDir;
    File currentProject;
    File currentFile;
    ArrayList<File> projectList = new ArrayList<File>();
    ArrayList<File> openFiles = new ArrayList<File>();
    HashMap<String, String> unsavedChanges = new HashMap<String, String>();

    // VS CODE DARK THEME COLORS
    int BG = Color.parseColor("#1E1E1E");
    int SIDEBAR_BG = Color.parseColor("#252526");
    int PANEL_BG = Color.parseColor("#2D2D2D");
    int ACCENT = Color.parseColor("#007ACC");
    int TEXT = Color.parseColor("#D4D4D4");
    int TEXT_DIM = Color.parseColor("#858585");
    int LINE_NUM_COLOR = Color.parseColor("#858585");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectsDir = new File(getFilesDir(), "projects");
        if (!projectsDir.exists()) projectsDir.mkdirs();
        buildIDE();
    }

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
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(20, 10, 20, 10);
        return b;
    }

    void buildIDE() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // --- TOP BAR ---
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(SIDEBAR_BG);
        top.setElevation(8);

        TextView title = text("⚡ Aladdin IDE", 18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(ACCENT);
        top.addView(title, new LinearLayout.LayoutParams(0, 60, 1));

        Button newProject = button("+ New Project");
        top.addView(newProject, new LinearLayout.LayoutParams(120, 60));

        Button build = button("▶ Run");
        build.setTextColor(ACCENT);
        top.addView(build, new LinearLayout.LayoutParams(90, 60));

        root.addView(top);

        // --- MAIN AREA (SIDEBAR + EDITOR) ---
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.HORIZONTAL);

        // SIDEBAR
        sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setBackgroundColor(SIDEBAR_BG);

        TextView projectTitle = text("EXPLORER", 12);
        projectTitle.setTextColor(TEXT_DIM);
        projectTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sidebar.addView(projectTitle);

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
                LinearLayout.LayoutParams.MATCH_PARENT, 50));

        // Welcome Screen
        welcomeScreen = new TextView(this);
        welcomeScreen.setText("No file opened\n\nOpen a file from the explorer\nor create a new project.");
        welcomeScreen.setTextColor(TEXT_DIM);
        welcomeScreen.setTextSize(16);
        welcomeScreen.setGravity(Gravity.CENTER);
        welcomeScreen.setVisibility(View.VISIBLE);
        editorArea.addView(welcomeScreen, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        // Editor Container (Line numbers + Code)
        LinearLayout editorContainer = new LinearLayout(this);
        editorContainer.setOrientation(LinearLayout.HORIZONTAL);
        editorContainer.setVisibility(View.GONE);

        lineNumbers = new TextView(this);
        lineNumbers.setTextColor(LINE_NUM_COLOR);
        lineNumbers.setTextSize(14);
        lineNumbers.setTypeface(Typeface.MONOSPACE);
        lineNumbers.setPadding(15, 15, 15, 15);
        lineNumbers.setGravity(Gravity.TOP | Gravity.RIGHT);
        lineNumbers.setBackgroundColor(BG);

        codeEditor = new EditText(this);
        codeEditor.setTextColor(TEXT);
        codeEditor.setTextSize(14);
        codeEditor.setTypeface(Typeface.MONOSPACE);
        codeEditor.setGravity(Gravity.TOP | Gravity.LEFT);
        codeEditor.setSingleLine(false);
        codeEditor.setBackgroundColor(BG);
        codeEditor.setPadding(15, 15, 15, 15);
        codeEditor.setHorizontallyScrolling(true);

        codeEditor.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    int pos = codeEditor.getSelectionStart();
                    String text = codeEditor.getText().toString();
                    if (pos > 0 && text.charAt(pos - 1) == '{') {
                        codeEditor.getText().insert(pos, "\n    ");
                        return true;
                    }
                }
                return false;
            }
        });

        codeEditor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateLineNumbers(s.toString());
                highlightSyntax(s);
            }
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
        consolePanel.setBackgroundColor(Color.BLACK);
        consolePanel.setVisibility(View.GONE);

        TextView consoleTitle = text("TERMINAL", 12);
        consoleTitle.setBackgroundColor(PANEL_BG);
        consoleTitle.setTextColor(TEXT_DIM);
        consolePanel.addView(consoleTitle);

        ScrollView consoleScroll = new ScrollView(this);
        consoleOutput = new TextView(this);
        consoleOutput.setTextColor(Color.GREEN);
        consoleOutput.setTextSize(12);
        consoleOutput.setTypeface(Typeface.MONOSPACE);
        consoleOutput.setPadding(15, 15, 15, 15);
        consoleScroll.addView(consoleOutput);
        consolePanel.addView(consoleScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 250));

        root.addView(consolePanel);

        // --- BOTTOM BAR ---
        LinearLayout bottom = new LinearLayout(this);
        bottom.setBackgroundColor(ACCENT);
        bottom.setElevation(8);

        Button save = button("💾 Save");
        save.setTextColor(Color.WHITE);
        Button closeConsole = button("✖ Close Console");
        closeConsole.setTextColor(Color.WHITE);

        bottom.addView(save, new LinearLayout.LayoutParams(0, 50, 1));
        bottom.addView(closeConsole, new LinearLayout.LayoutParams(0, 50, 1));

        status = text("Ready", 12);
        status.setBackgroundColor(SIDEBAR_BG);
        status.setTextColor(TEXT_DIM);
        root.addView(bottom);
        root.addView(status);

        setContentView(root);
        refreshProjects();

        newProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { createProject(); }
        });
        
        build.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentProject == null) {
                    Toast.makeText(MainActivity.this, "Open a project first", Toast.LENGTH_SHORT).show();
                    return;
                }
                triggerCloudBuild();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { saveCurrentFile(); }
        });

        closeConsole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { consolePanel.setVisibility(View.GONE); }
        });
    }

    private void triggerCloudBuild() {
        Toast.makeText(this, "Build triggered! Check console.", Toast.LENGTH_SHORT).show();
        consolePanel.setVisibility(View.VISIBLE);
        consoleOutput.setText("🚀 Starting build...\n(This is a placeholder. Paste your GitHub build logic here!)");
    }

    private void updateLineNumbers(String text) {
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') lines++;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) sb.append(i).append("\n");
        lineNumbers.setText(sb.toString());
    }

    private void highlightSyntax(Editable editable) {
        int cursorPosition = codeEditor.getSelectionStart();
        ForegroundColorSpan[] oldSpans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : oldSpans) editable.removeSpan(span);

        String text = editable.toString();

        Pattern stringPattern = Pattern.compile("\"[^\"]*\"");
        Matcher stringMatcher = stringPattern.matcher(text);
        while (stringMatcher.find()) {
            editable.setSpan(new ForegroundColorSpan(Color.parseColor("#CE9178")), stringMatcher.start(), stringMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        Pattern commentPattern = Pattern.compile("//.*");
        Matcher commentMatcher = commentPattern.matcher(text);
        while (commentMatcher.find()) {
            editable.setSpan(new ForegroundColorSpan(Color.parseColor("#6A9955")), commentMatcher.start(), commentMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        String[] keywords = {"public", "private", "protected", "class", "void", "int", "String", "boolean", "if", "else", "for", "while", "return", "new", "import", "package", "extends", "implements", "final", "static"};
        for (String keyword : keywords) {
            Pattern keywordPattern = Pattern.compile("\\b" + keyword + "\\b");
            Matcher keywordMatcher = keywordPattern.matcher(text);
            while (keywordMatcher.find()) {
                editable.setSpan(new ForegroundColorSpan(Color.parseColor("#569CD6")), keywordMatcher.start(), keywordMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        Pattern xmlPattern = Pattern.compile("<[^>]*>");
        Matcher xmlMatcher = xmlPattern.matcher(text);
        while (xmlMatcher.find()) {
            editable.setSpan(new ForegroundColorSpan(Color.parseColor("#4EC9B0")), xmlMatcher.start(), xmlMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        codeEditor.setSelection(cursorPosition);
    }

    void addTab(final File file) {
        if (!openFiles.contains(file)) openFiles.add(file);
        tabContainer.removeAllViews();

        for (final File f : openFiles) {
            LinearLayout tabLayout = new LinearLayout(this);
            tabLayout.setOrientation(LinearLayout.HORIZONTAL);
            tabLayout.setBackgroundColor(f.equals(currentFile) ? BG : PANEL_BG);

            TextView tab = new TextView(this);
            tab.setText(" " + f.getName() + " ");
            tab.setTextColor(f.equals(currentFile) ? TEXT : TEXT_DIM);
            tab.setTextSize(12);
            tab.setPadding(30, 15, 10, 15);

            TextView close = new TextView(this);
            close.setText(" ✕ ");
            close.setTextColor(TEXT_DIM);
            close.setTextSize(12);
            close.setPadding(10, 15, 20, 15);

            tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { loadFileIntoEditor(f); }
            });

            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFiles.remove(f);
                    if (f.equals(currentFile)) {
                        currentFile = null;
                        codeEditor.setText("");
                        welcomeScreen.setVisibility(View.VISIBLE);
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
            String content = unsavedChanges.containsKey(file.getAbsolutePath()) ? unsavedChanges.get(file.getAbsolutePath()) : readFile(file);
            codeEditor.setText(content);
            fileTitle.setText(file.getName());
            status.setText("Opened: " + file.getName());
            welcomeScreen.setVisibility(View.GONE);
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
            status.setText("✓ Saved: " + currentFile.getName());
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
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

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) for (File child : children) deleteRecursive(child);
        }
        fileOrDirectory.delete();
    }

    void createProject() {
        final EditText input = new EditText(this);
        input.setHint("Example: MyAwesomeApp");

        new AlertDialog.Builder(this)
                .setTitle("Create Android Project")
                .setMessage("Enter your new app name.")
                .setView(input)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        String name = input.getText().toString().trim();
                        if (name.length() == 0) {
                            Toast.makeText(MainActivity.this, "Enter a project name", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        try {
                            generateAndroidProject(name);
                            Toast.makeText(MainActivity.this, "✓ Project created!", Toast.LENGTH_LONG).show();
                            status.setText("Created: " + name);
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
            TextView item = text("📱 " + p.getName(), 14);
            item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            item.setTextColor(TEXT);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { openProject(p); }
            });
            sidebar.addView(item);
        }
    }

    void openProject(File project) {
        currentProject = project;
        status.setText("Project: " + project.getName());
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
            @Override
            public int compare(File f1, File f2) {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });

        for (final File f : files) {
            if (f.getName().startsWith(".")) continue;
            String prefix = f.isDirectory() ? "📁 " : "📄 ";
            TextView item = text(prefix + f.getName(), 13);
            item.setPadding(10 + depth * 15, 10, 10, 10);
            item.setTextColor(f.isDirectory() ? TEXT : TEXT_DIM);

            if (f.isDirectory()) {
                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { showFiles(currentProject); }
                });
                item.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) { return showContextMenu(f, true); }
                });
            } else {
                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { openFile(f); }
                });
                item.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) { return showContextMenu(f, false); }
                });
            }
            sidebar.addView(item);
            if (f.isDirectory()) showDirectory(f, depth + 1);
        }
    }

    private boolean showContextMenu(final File file, final boolean isDirectory) {
        final String[] options = isDirectory ? new String[]{"New File", "New Folder", "Rename", "Delete"} : new String[]{"Rename", "Delete"};
        new AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String choice = options[which];
                        if (choice.equals("New File")) createNewFile(file);
                        else if (choice.equals("New Folder")) createNewFolder(file);
                        else if (choice.equals("Rename")) renameFile(file);
                        else if (choice.equals("Delete")) deleteFile(file);
                    }
                }).show();
        return true;
    }

    private void createNewFile(final File parentDir) {
        final EditText input = new EditText(this);
        input.setHint("filename.java");
        new AlertDialog.Builder(this).setTitle("New File").setView(input)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        File newFile = new File(parentDir, input.getText().toString());
                        try {
                            writeFile(newFile, "");
                            showFiles(currentProject);
                        } catch (Exception e) {}
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void createNewFolder(final File parentDir) {
        final EditText input = new EditText(this);
        input.setHint("folder_name");
        new AlertDialog.Builder(this).setTitle("New Folder").setView(input)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        File newDir = new File(parentDir, input.getText().toString());
                        if (newDir.mkdirs()) showFiles(currentProject);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void renameFile(final File file) {
        final EditText input = new EditText(this);
        input.setText(file.getName());
        new AlertDialog.Builder(this).setTitle("Rename").setView(input)
                .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        File newFile = new File(file.getParent(), input.getText().toString());
                        if (file.renameTo(newFile)) showFiles(currentProject);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void deleteFile(final File file) {
        new AlertDialog.Builder(this).setTitle("Delete")
                .setMessage("Delete " + file.getName() + "?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        deleteRecursive(file);
                        showFiles(currentProject);
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
}
