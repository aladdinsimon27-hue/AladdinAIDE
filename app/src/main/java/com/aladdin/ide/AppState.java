package com.aladdin.ide;

import android.graphics.Color;
import java.io.File;
import java.util.*;

public class AppState {
    // THEME COLORS
    public static int BG, SIDEBAR_BG, ACTIVITY_BG, PANEL_BG, TAB_INACTIVE;
    public static int ACCENT, ACCENT_LIGHT;
    public static int TEXT, TEXT_BRIGHT, TEXT_DIM;
    public static int GREEN, ORANGE, BLUE, COMMENT, YELLOW, NUMBER_CLR, LINE_HL;
    public static int themeId = 0, accentId = 0;

    // SETTINGS
    public static String githubToken = "", githubUser = "", githubRepo = "";
    public static int fontSize = 13;
    public static boolean wordWrap = true, autoClose = true;
    public static boolean lineNumbersOn = true, minimapOn = false;
    public static String lineEnding = "LF";

    // RUNTIME
    public static File projectsDir, currentProject, currentFile;
    public static ArrayList<File> openFiles = new ArrayList<File>();
    public static ArrayList<File> dirtyFiles = new ArrayList<File>();
    public static ArrayList<File> recentFiles = new ArrayList<File>();
    public static ArrayList<File> recentProjects = new ArrayList<File>();
    public static ArrayList<File> collapsedFolders = new ArrayList<File>();
    public static ArrayList<Integer> bookmarks = new ArrayList<Integer>();
    public static ArrayList<String> searchHistory = new ArrayList<String>();
    public static ArrayList<String> commandHistory = new ArrayList<String>();
    public static HashMap<String, String> unsavedChanges = new HashMap<String, String>();
    public static HashMap<String, String> fileTemplates = new HashMap<String, String>();

    // UI STATE
    public static int activeActivity = 0, activeBottomTab = 2;
    public static boolean zenMode = false, sidebarVisible = true;
    public static boolean showHidden = false, splitMode = false, previewMode = false;

    // UNDO
    public static ArrayList<String> undoStack = new ArrayList<String>();
    public static ArrayList<Integer> undoPos = new ArrayList<Integer>();
    public static ArrayList<String> redoStack = new ArrayList<String>();
    public static ArrayList<Integer> redoPos = new ArrayList<Integer>();
    public static String lastText = "";
    public static boolean recordingUndo = true;
    public static long lastEditTime = 0;

    public static void applyTheme() {
        String[][] themes = {
            {"#1E1E1E","#252526","#333333","#2D2D2D","#2D2D2D","#CCCCCC","#FFFFFF","#858585",
             "#4EC9B0","#CE9178","#569CD6","#6A9955","#DCDCAA","#B5CEA8","#2A2D2E"},
            {"#282A36","#21222C","#191A21","#343746","#21222C","#F8F8F2","#FFFFFF","#6272A4",
             "#50FA7B","#FFB86C","#8BE9FD","#6272A4","#F1FA8C","#BD93F9","#44475A"},
            {"#272822","#1E1F1C","#1E1F1C","#3E3D32","#3E3D32","#F8F8F2","#FFFFFF","#75715E",
             "#A6E22E","#E6DB74","#66D9EF","#75715E","#E6DB74","#AE81FF","#3E3D32"},
            {"#FFFFFF","#F3F3F3","#E7E7E7","#ECECEC","#ECECEC","#333333","#000000","#6C6C6C",
             "#22863A","#032F62","#0000FF","#6A737D","#795E26","#098658","#F0F0F0"}
        };
        String[] t = themes[Math.min(themeId, themes.length - 1)];
        BG = Color.parseColor(t[0]); SIDEBAR_BG = Color.parseColor(t[1]);
        ACTIVITY_BG = Color.parseColor(t[2]); PANEL_BG = Color.parseColor(t[3]);
        TAB_INACTIVE = Color.parseColor(t[4]); TEXT = Color.parseColor(t[5]);
        TEXT_BRIGHT = Color.parseColor(t[6]); TEXT_DIM = Color.parseColor(t[7]);
        GREEN = Color.parseColor(t[8]); ORANGE = Color.parseColor(t[9]);
        BLUE = Color.parseColor(t[10]); COMMENT = Color.parseColor(t[11]);
        YELLOW = Color.parseColor(t[12]); NUMBER_CLR = Color.parseColor(t[13]);
        LINE_HL = Color.parseColor(t[14]);

        String[] accents = {"#007ACC","#A259FF","#22C55E","#F59E0B","#EF4444","#EC4899"};
        int[] light = {0xFF1F8AD2,0xFFB678FF,0xFF4ADE80,0xFFFBBF24,0xFFF87171,0xFFF472B6};
        ACCENT = Color.parseColor(accents[accentId]);
        ACCENT_LIGHT = light[accentId];
    }

    public static void registerTemplates() {
        fileTemplates.put(".html", "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>Document</title>\n<link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n\n<h1>Hello World</h1>\n\n<script src=\"script.js\"></script>\n</body>\n</html>\n");
        fileTemplates.put(".css", "* { margin: 0; padding: 0; box-sizing: border-box; }\nbody { font-family: Arial, sans-serif; background: #f0f0f0; color: #333; }\n");
        fileTemplates.put(".js", "// JavaScript\nconsole.log('Hello, World!');\n");
        fileTemplates.put(".java", "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}\n");
        fileTemplates.put(".xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n    android:layout_width=\"match_parent\"\n    android:layout_height=\"match_parent\"\n    android:orientation=\"vertical\">\n\n</LinearLayout>\n");
        fileTemplates.put(".json", "{\n    \"name\": \"MyApp\",\n    \"version\": \"1.0.0\"\n}\n");
        fileTemplates.put(".md", "# Title\n\n## Subtitle\n\nSome text here.\n");
    }
}
