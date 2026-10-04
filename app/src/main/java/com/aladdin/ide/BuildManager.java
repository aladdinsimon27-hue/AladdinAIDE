package com.aladdin.ide;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import java.io.*;
import java.net.*;
import java.util.zip.*;
import org.json.*;

public class BuildManager {
    private Activity a;
    private TextView consoleOutput;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    public BuildManager(Activity activity, TextView consoleOutput) {
        this.a = activity;
        this.consoleOutput = consoleOutput;
    }

    private void log(final String m) {
        uiHandler.post(new Runnable() {
            @Override public void run() { consoleOutput.append(m + "\n"); }
        });
    }

    public void triggerCloudBuild() {
        if (AppState.githubToken.isEmpty() || AppState.githubUser.isEmpty() || AppState.githubRepo.isEmpty()) {
            UIHelper.toast(a, "Set GitHub settings first!");
            return;
        }
        consoleOutput.setText("");
        log("$ gradle assembleDebug");
        log("🚀 Cloud build starting...");
        log("📦 " + AppState.githubUser + "/" + AppState.githubRepo);
        log("───────────────────────");
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    URL url = new URL("https://api.github.com/repos/" + AppState.githubUser + "/"
                        + AppState.githubRepo + "/actions/workflows/build.yml/dispatches");
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("POST");
                    c.setRequestProperty("Authorization", "token " + AppState.githubToken);
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
                    } else log("❌ Failed: " + code);
                } catch (Exception e) { log("❌ " + e.getMessage()); }
            }
        }).start();
    }

    private void checkStatus() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    boolean done = false; int p = 0;
                    while (!done && p < 60) {
                        p++;
                        URL url = new URL("https://api.github.com/repos/" + AppState.githubUser + "/"
                            + AppState.githubRepo + "/actions/runs?per_page=1");
                        HttpURLConnection c = (HttpURLConnection) url.openConnection();
                        c.setRequestProperty("Authorization", "token " + AppState.githubToken);
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
                                    log("📥 Downloading...");
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

    private void download(long runId) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    URL url = new URL("https://api.github.com/repos/" + AppState.githubUser + "/"
                        + AppState.githubRepo + "/actions/runs/" + runId + "/artifacts");
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestProperty("Authorization", "token " + AppState.githubToken);
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
                    dc.setRequestProperty("Authorization", "token " + AppState.githubToken);
                    dc.setInstanceFollowRedirects(true);
                    InputStream in = dc.getInputStream();
                    File dir = a.getExternalFilesDir(null);
                    if (dir != null && !dir.exists()) dir.mkdirs();
                    File zip = new File(dir, "AladdinIDE-APK.zip");
                    FileOutputStream o = new FileOutputStream(zip);
                    byte[] buf = new byte[8192]; int n;
                    while ((n = in.read(buf)) != -1) o.write(buf, 0, n);
                    o.close(); in.close();
                    log("✅ ZIP downloaded");
                    File apk = extractApk(zip, dir);
                    if (apk == null) { log("❌ Extract failed"); return; }
                    log("📲 Launching installer...");
                    install(apk);
                } catch (Exception e) { log("❌ " + e.getMessage()); }
            }
        }).start();
    }

    private File extractApk(File zip, File out) {
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

    private void install(final File apk) {
        uiHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!a.getPackageManager().canRequestPackageInstalls()) {
                            log("⚠ Allow install");
                            Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                            i.setData(Uri.parse("package:" + a.getPackageName()));
                            a.startActivity(i);
                            return;
                        }
                    }
                    Uri uri = FileProvider.getUriForFile(a, "com.aladdin.ide.fileprovider", apk);
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(uri, "application/vnd.android.package-archive");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    a.startActivity(i);
                    log("🎊 Installer launched");
                } catch (Exception e) { log("❌ " + e.getMessage()); }
            }
        });
    }
}
