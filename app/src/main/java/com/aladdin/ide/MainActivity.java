package com.aladdin.ide;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {

    // UI COMPONENTS
    LinearLayout root;
    LinearLayout sidebar;
    LinearLayout editorArea;
    TextView status;
    EditText codeEditor;
    TextView lineNumbers;
    TextView fileTitle;

    // FILE MANAGEMENT
    File projectsDir;
    File currentProject;
    File currentFile;
    ArrayList<File> projectList = new ArrayList<File>();

    // THEME COLORS
    int BG = Color.rgb(18, 18, 18);
    int PANEL = Color.rgb(28, 28, 28);
    int PANEL2 = Color.rgb(38, 38, 38);
    int TEXT = Color.WHITE;
    int LINE_NUM_COLOR = Color.GRAY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectsDir = new File(getFilesDir(), "projects");
        if (!projectsDir.exists()) projectsDir.mkdirs();
        buildIDE();
    }

    // ==============================
    // UI HELPER METHODS
    // ==============================
    TextView text(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(TEXT);
        t.setPadding(14, 12, 14, 12);
        return t;
    }

    Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(TEXT);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setBackgroundColor(PANEL2);
        return b;
    }

    // ==============================
    // BUILD THE UI
    // ==============================
    void buildIDE() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // --- TOP BAR ---
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(PANEL);

        TextView title = text("⚡ Aladdin IDE", 20);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, 60, 1));

        Button newProject = button("+ Project");
        top.addView(newProject, new LinearLayout.LayoutParams(100, 60));

        Button build = button("▶ Build");
        top.addView(build, new LinearLayout.LayoutParams(90, 60));

        root.addView(top);

        // --- MAIN AREA (SIDEBAR + EDITOR) ---
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.HORIZONTAL);

        // SIDEBAR
        sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setBackgroundColor(PANEL);

        TextView projectTitle = text("📁 PROJECTS", 14);
        projectTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sidebar.addView(projectTitle);

        ScrollView sideScroll = new ScrollView(this);
        sideScroll.addView(sidebar);
        main.addView(sideScroll, new LinearLayout.LayoutParams(180, LinearLayout.LayoutParams.MATCH_PARENT));

        // EDITOR AREA
        editorArea = new LinearLayout(this);
        editorArea.setOrientation(LinearLayout.VERTICAL);

        fileTitle = text("No file opened", 14);
        fileTitle.setBackgroundColor(PANEL2);
        editorArea.addView(fileTitle);

        // Editor Container (Line numbers + Code)
        LinearLayout editorContainer = new LinearLayout(this);
        editorContainer.setOrientation(LinearLayout.HORIZONTAL);

        // Line Numbers
        lineNumbers = new TextView(this);
        lineNumbers.setTextColor(LINE_NUM_COLOR);
        lineNumbers.setTextSize(14);
        lineNumbers.setTypeface(Typeface.MONOSPACE);
        lineNumbers.setPadding(10, 10, 10, 10);
        lineNumbers.setGravity(Gravity.TOP | Gravity.RIGHT);
        lineNumbers.setBackgroundColor(PANEL);

        // Code Editor
        codeEditor = new EditText(this);
        codeEditor.setTextColor(TEXT);
        codeEditor.setTextSize(14);
        codeEditor.setTypeface(Typeface.MONOSPACE);
        codeEditor.setGravity(Gravity.TOP | Gravity.LEFT);
        codeEditor.setSingleLine(false);
        codeEditor.setBackgroundColor(BG);
        codeEditor.setPadding(10, 10, 10, 10);
        codeEditor.setHorizontallyScrolling(true);

        // Sync Line Numbers with text changes
        codeEditor.addTextChangedListener(new TextWatcher() {
				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
				@Override public void afterTextChanged(Editable s) {
					updateLineNumbers(s.toString());
				}
			});

        // Sync scrolling (Replaced lambda with anonymous class)
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

        // --- BOTTOM BAR ---
        LinearLayout bottom = new LinearLayout(this);
        Button save = button("💾 Save");
        Button refresh = button("🔄 Refresh");

        bottom.addView(save, new LinearLayout.LayoutParams(0, 55, 1));
        bottom.addView(refresh, new LinearLayout.LayoutParams(0, 55, 1));

        status = text("Ready", 12);
        root.addView(bottom);
        root.addView(status);

        setContentView(root);
        refreshProjects();

        // BUTTON LISTENERS (Converted from Lambdas to Anonymous Classes)
        newProject.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					createProject();
				}
			});

        build.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentProject == null) {
						Toast.makeText(MainActivity.this, "Open a project first", Toast.LENGTH_SHORT).show();
						return;
					}
					buildProjectWithTermux(currentProject);
				}
			});

        save.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					saveCurrentFile();
				}
			});

        refresh.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					refreshProjects();
				}
			});
    }

    // ==============================
    // LINE NUMBERS LOGIC
    // ==============================
    private void updateLineNumbers(String text) {
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
    // TERMUX BUILD ENGINE
    // ==============================
    private void buildProjectWithTermux(File project) {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.termux", "com.termux.app.RunCommandService");
            intent.setAction("com.termux.RUN_COMMAND");
            intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
            intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-c", 
								"cd " + project.getAbsolutePath() + " && ./gradlew assembleDebug"});
            intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", project.getAbsolutePath());
            intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);

            startService(intent);
            status.setText("Build requested: " + project.getName());
            Toast.makeText(this, "Sending build command to Termux...", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Termux not installed or command failed", Toast.LENGTH_LONG).show();
            status.setText("Build failed: Termux not found");
        }
    }

    // ==============================
    // PROJECT MANAGEMENT
    // ==============================
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
			})
			.setNegativeButton("Cancel", null)
			.show();
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

            item.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						openProject(p);
					}
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

        // Sort folders first, then files (Replaced lambda with Comparator)
        Arrays.sort(files, new Comparator<File>() {
				@Override
				public int compare(File f1, File f2) {
					if (f1.isDirectory() && !f2.isDirectory()) return -1;
					if (!f1.isDirectory() && f2.isDirectory()) return 1;
					return f1.getName().compareToIgnoreCase(f2.getName());
				}
			});

        for (final File f : files) {
            if (f.getName().startsWith(".")) continue; // Skip hidden files

            String prefix = f.isDirectory() ? "📁 " : "📄 ";
            TextView item = text(prefix + f.getName(), 13);
            item.setPadding(10 + depth * 15, 10, 10, 10);

            if (f.isDirectory()) {
                item.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							showFiles(currentProject);
						}
					});
                item.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							return showContextMenu(f, true);
						}
					});
            } else {
                item.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							openFile(f);
						}
					});
                item.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							return showContextMenu(f, false);
						}
					});
            }
            sidebar.addView(item);

            if (f.isDirectory()) {
                showDirectory(f, depth + 1);
            }
        }
    }

    // ==============================
    // FILE CONTEXT MENU (LONG PRESS)
    // ==============================
    private boolean showContextMenu(final File file, final boolean isDirectory) {
        final String[] options = isDirectory ? 
        new String[]{"New File", "New Folder", "Rename", "Delete"} : 
        new String[]{"Rename", "Delete"};
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
			})
			.show();
        return true;
    }

    private void createNewFile(final File parentDir) {
        final EditText input = new EditText(this);
        input.setHint("filename.java");
        new AlertDialog.Builder(this)
			.setTitle("New File")
			.setView(input)
			.setPositiveButton("Create", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int w) {
					File newFile = new File(parentDir, input.getText().toString());
					try {
						writeFile(newFile, "");
						showFiles(currentProject);
						Toast.makeText(MainActivity.this, "File created", Toast.LENGTH_SHORT).show();
					} catch (Exception e) {
						Toast.makeText(MainActivity.this, "Error creating file", Toast.LENGTH_SHORT).show();
					}
				}
			})
			.setNegativeButton("Cancel", null).show();
    }

    private void createNewFolder(final File parentDir) {
        final EditText input = new EditText(this);
        input.setHint("folder_name");
        new AlertDialog.Builder(this)
			.setTitle("New Folder")
			.setView(input)
			.setPositiveButton("Create", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int w) {
					File newDir = new File(parentDir, input.getText().toString());
					if (newDir.mkdirs()) {
						showFiles(currentProject);
						Toast.makeText(MainActivity.this, "Folder created", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(MainActivity.this, "Error creating folder", Toast.LENGTH_SHORT).show();
					}
				}
			})
			.setNegativeButton("Cancel", null).show();
    }

    private void renameFile(final File file) {
        final EditText input = new EditText(this);
        input.setText(file.getName());
        new AlertDialog.Builder(this)
			.setTitle("Rename")
			.setView(input)
			.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int w) {
					File newFile = new File(file.getParent(), input.getText().toString());
					if (file.renameTo(newFile)) {
						showFiles(currentProject);
						Toast.makeText(MainActivity.this, "Renamed", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(MainActivity.this, "Rename failed", Toast.LENGTH_SHORT).show();
					}
				}
			})
			.setNegativeButton("Cancel", null).show();
    }

    private void deleteFile(final File file) {
        new AlertDialog.Builder(this)
			.setTitle("Delete")
			.setMessage("Are you sure you want to delete " + file.getName() + "?")
			.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int w) {
					deleteRecursive(file);
					showFiles(currentProject);
					Toast.makeText(MainActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
				}
			})
			.setNegativeButton("Cancel", null).show();
    }

    // ==============================
    // FILE I/O OPERATIONS
    // ==============================
    void openFile(File file) {
        try {
            currentFile = file;
            String content = readFile(file);
            codeEditor.setText(content);
            fileTitle.setText(file.getName());
            status.setText("Opened: " + file.getName());
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
        while ((length = in.read(buffer)) != -1) {
            out.write(buffer, 0, length);
        }
        in.close();
        return out.toString("UTF-8");
    }

    void writeFile(File file, String data) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        FileOutputStream out = new FileOutputStream(file);
        out.write(data.getBytes("UTF-8"));
        out.close();
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    // ==============================
    // INLINED PROJECT GENERATOR
    // ==============================
    void generateAndroidProject(String name) throws Exception {
        String safeName = name.replaceAll("[^A-Za-z0-9_]", "");
        if (safeName.length() == 0) safeName = "MyApp";

        File project = new File(projectsDir, safeName);
        if (project.exists()) throw new Exception("Project already exists");

        File app = new File(project, "app");
        File src = new File(app, "src/main/java/com/aladdin/app");
        File layout = new File(app, "src/main/res/layout");
        File values = new File(app, "src/main/res/values");

        src.mkdirs();
        layout.mkdirs();
        values.mkdirs();

        // settings.gradle
        writeFile(new File(project, "settings.gradle"),
				  "pluginManagement {\n" +
				  "    repositories {\n" +
				  "        google()\n" +
				  "        mavenCentral()\n" +
				  "        gradlePluginPortal()\n" +
				  "    }\n" +
				  "}\n\n" +
				  "dependencyResolutionManagement {\n" +
				  "    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)\n" +
				  "    repositories {\n" +
				  "        google()\n" +
				  "        mavenCentral()\n" +
				  "    }\n" +
				  "}\n\n" +
				  "rootProject.name = \"" + safeName + "\"\n\n" +
				  "include ':app'\n");

        // Root build.gradle
        writeFile(new File(project, "build.gradle"),
				  "plugins {\n" +
				  "    id 'com.android.application' version '8.2.2' apply false\n" +
				  "}\n");

        // App build.gradle
        writeFile(new File(app, "build.gradle"),
				  "plugins {\n" +
				  "    id 'com.android.application'\n" +
				  "}\n\n" +
				  "android {\n" +
				  "    namespace 'com.aladdin.app'\n" +
				  "    compileSdk 34\n\n" +
				  "    defaultConfig {\n" +
				  "        applicationId 'com.aladdin.app'\n" +
				  "        minSdk 24\n" +
				  "        targetSdk 34\n" +
				  "        versionCode 1\n" +
				  "        versionName '1.0'\n" +
				  "    }\n" +
				  "}\n");

        // Manifest
        writeFile(new File(app, "src/main/AndroidManifest.xml"),
				  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
				  "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n\n" +
				  "    <application\n" +
				  "        android:theme=\"@style/AppTheme\"\n" +
				  "        android:label=\"" + safeName + "\">\n\n" +
				  "        <activity\n" +
				  "            android:name=\".MainActivity\"\n" +
				  "            android:exported=\"true\">\n\n" +
				  "            <intent-filter>\n" +
				  "                <action android:name=\"android.intent.action.MAIN\" />\n" +
				  "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
				  "            </intent-filter>\n\n" +
				  "        </activity>\n" +
				  "    </application>\n\n" +
				  "</manifest>\n");

        // MainActivity.java
        writeFile(new File(src, "MainActivity.java"),
				  "package com.aladdin.app;\n\n" +
				  "import android.app.Activity;\n" +
				  "import android.os.Bundle;\n\n" +
				  "public class MainActivity extends Activity {\n\n" +
				  "    @Override\n" +
				  "    protected void onCreate(Bundle savedInstanceState) {\n" +
				  "        super.onCreate(savedInstanceState);\n" +
				  "        setContentView(R.layout.activity_main);\n" +
				  "    }\n\n" +
				  "}\n");

        // activity_main.xml
        writeFile(new File(layout, "activity_main.xml"),
				  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
				  "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
				  "    android:layout_width=\"match_parent\"\n" +
				  "    android:layout_height=\"match_parent\"\n" +
				  "    android:gravity=\"center\"\n" +
				  "    android:orientation=\"vertical\">\n\n" +
				  "    <TextView\n" +
				  "        android:layout_width=\"wrap_content\"\n" +
				  "        android:layout_height=\"wrap_content\"\n" +
				  "        android:text=\"Hello from Aladdin IDE!\"\n" +
				  "        android:textSize=\"24sp\" />\n\n" +
				  "</LinearLayout>\n");

        // strings.xml
        writeFile(new File(values, "strings.xml"),
				  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
				  "<resources>\n" +
				  "    <string name=\"app_name\">" + safeName + "</string>\n" +
				  "</resources>\n");

        // styles.xml
        writeFile(new File(values, "styles.xml"),
				  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
				  "<resources>\n" +
				  "    <style name=\"AppTheme\" parent=\"android:style/Theme.Material.Light.NoActionBar\">\n" +
				  "        <item name=\"android:fontFamily\">sans</item>\n" +
				  "        <item name=\"android:colorAccent\">#3F51B5</item>\n" +
				  "    </style>\n" +
				  "</resources>\n");
    }
}
