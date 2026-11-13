package ch.stellenweg.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Hülle für die Web-App Stellenweg. Die Web-App liegt in assets/web und wird über WebViewAssetLoader
 * unter https://appassets.androidplatform.net geladen (sicherer Kontext: IndexedDB, Zwischenablage).
 * window.AndroidApp ist die Brücke für alles, was eine Web-App nicht darf.
 */
public class MainActivity extends Activity {
    static final String HOME = "https://appassets.androidplatform.net/assets/web/index.html";
    private static final int REQ_FILE = 11, REQ_NOTIFY = 12;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraUri;
    private String sharedText, pendingGo;
    private boolean loaded;
    /** Geteilte Dateien als JSON [{name,type,b64}], abgeholt mit takeSharedFiles */
    private String sharedFiles;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.setFitsSystemWindows(true);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setSupportMultipleWindows(false);
        s.setTextZoom(100);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                WebResourceResponse res = loader.shouldInterceptRequest(r.getUrl());
                // Module (.mjs) und WebAssembly (.wasm) für das Porträt brauchen den richtigen Typ, sonst lädt die WebView sie nicht
                String p = r.getUrl().getPath();
                if (res != null && p != null) {
                    if (p.endsWith(".mjs")) res.setMimeType("text/javascript");
                    else if (p.endsWith(".wasm")) res.setMimeType("application/wasm");
                }
                return res;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("appassets.androidplatform.net".equals(u.getHost())) return false;
                openExternal(u);
                return true;
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                loaded = true;
                flushPending();
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                return startChooser(p);
            }
        });

        web.addJavascriptInterface(new Bridge(), "AndroidApp");
        handleIntent(getIntent());
        if (state != null) web.restoreState(state);
        else web.loadUrl(HOME);
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        handleIntent(i);
        flushPending();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    /** Geteilter Text oder ein Tipp auf eine Erinnerung */
    private void handleIntent(Intent i) {
        if (i == null) return;
        String act = i.getAction();
        if (Intent.ACTION_SEND.equals(act) || Intent.ACTION_SEND_MULTIPLE.equals(act)) {
            ArrayList<Uri> uris = new ArrayList<>();
            try {
                if (Intent.ACTION_SEND_MULTIPLE.equals(act)) {
                    ArrayList<Uri> l = i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    if (l != null) uris.addAll(l);
                } else {
                    Uri u = i.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (u != null) uris.add(u);
                }
            } catch (Exception ignored) { }
            if (!uris.isEmpty()) { readShared(uris, i.getType()); return; }
        }
        if (Intent.ACTION_SEND.equals(act) && "text/plain".equals(i.getType())) {
            String t = i.getStringExtra(Intent.EXTRA_TEXT);
            String sub = i.getStringExtra(Intent.EXTRA_SUBJECT);
            if (t != null) sharedText = (sub != null && !t.contains(sub) ? sub + "\n" : "") + t;
        }
        String go = i.getStringExtra("go");
        if (go != null) pendingGo = go;
    }

    /** Geteilte Dateien im Hintergrund lesen (höchstens 25 MB je Datei), danach an die Web-App geben */
    private void readShared(final ArrayList<Uri> uris, final String type) {
        new Thread(() -> {
            JSONArray out = new JSONArray();
            for (Uri u : uris) {
                try {
                    String name = "Dokument", mime = getContentResolver().getType(u);
                    try (Cursor c = getContentResolver().query(u, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                        if (c != null && c.moveToFirst() && c.getString(0) != null) name = c.getString(0);
                    } catch (Exception ignored) { }
                    if (mime == null) mime = type != null && !type.contains("*") ? type : "application/octet-stream";
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    try (InputStream in = getContentResolver().openInputStream(u)) {
                        byte[] buf = new byte[65536]; int n; long total = 0;
                        while ((n = in.read(buf)) > 0) { total += n; if (total > 25L * 1024 * 1024) throw new Exception("zu gross"); bo.write(buf, 0, n); }
                    }
                    out.put(new JSONObject().put("name", name).put("type", mime)
                            .put("b64", Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP)));
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "Eine Datei liess sich nicht übernehmen (zu gross oder gesperrt).", Toast.LENGTH_LONG).show());
                }
            }
            if (out.length() == 0) return;
            synchronized (MainActivity.this) { sharedFiles = out.toString(); }
            runOnUiThread(this::flushPending);
        }).start();
    }

    private synchronized String takeFiles() {
        String f = sharedFiles;
        sharedFiles = null;
        return f;
    }

    private void flushPending() {
        if (!loaded) return;
        boolean files;
        synchronized (this) { files = sharedFiles != null; }
        if (files) web.postDelayed(() -> web.evaluateJavascript("window.stellenwegFiles&&window.stellenwegFiles()", null), 600);
        if (sharedText != null) {
            final String t = JSONObject.quote(sharedText);
            sharedText = null;
            web.postDelayed(() -> web.evaluateJavascript("window.stellenwegShared&&window.stellenwegShared(" + t + ")", null), 600);
        }
        if (pendingGo != null) {
            final String g = JSONObject.quote(pendingGo);
            pendingGo = null;
            web.postDelayed(() -> web.evaluateJavascript("window.stellenwegGo&&window.stellenwegGo(" + g + ")", null), 700);
        }
    }

    private void openExternal(Uri u) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, u);
            if ("mailto".equals(u.getScheme())) i = new Intent(Intent.ACTION_SENDTO, u);
            if ("tel".equals(u.getScheme())) i = new Intent(Intent.ACTION_DIAL, u);
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Keine App gefunden, die das öffnen kann.", Toast.LENGTH_SHORT).show();
        }
    }

    /** Dateiwahl mit Kamera, wenn das Feld es verlangt (capture) oder als zusätzliche Wahl */
    private boolean startChooser(WebChromeClient.FileChooserParams p) {
        Intent pick = p.createIntent();
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        if (p.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE)
            pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        Intent cam = null;
        boolean wantsImage = false;
        for (String a : p.getAcceptTypes()) if (a.startsWith("image") || a.isEmpty()) wantsImage = true;
        if (wantsImage) {
            try {
                File dir = new File(getCacheDir(), "camera");
                dir.mkdirs();
                File f = File.createTempFile("foto_", ".jpg", dir);
                cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".files", f);
                cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cam.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception e) {
                cam = null;
            }
        }
        Intent start;
        if (p.isCaptureEnabled() && cam != null) start = cam;
        else {
            start = Intent.createChooser(pick, "Datei wählen");
            if (cam != null) start.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cam});
        }
        try {
            startActivityForResult(start, REQ_FILE);
            return true;
        } catch (ActivityNotFoundException e) {
            fileCallback = null;
            return false;
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_FILE || fileCallback == null) return;
        Uri[] result = null;
        if (res == RESULT_OK) {
            if (data != null && data.getClipData() != null) {
                ClipData c = data.getClipData();
                result = new Uri[c.getItemCount()];
                for (int k = 0; k < c.getItemCount(); k++) result[k] = c.getItemAt(k).getUri();
            } else if (data != null && data.getData() != null) {
                result = new Uri[]{data.getData()};
            } else if (cameraUri != null) {
                result = new Uri[]{cameraUri};
            }
        }
        fileCallback.onReceiveValue(result);
        fileCallback = null;
    }

    /** Läuft auf dem UI-Thread und wartet auf das Ergebnis (Brückenaufrufe kommen aus einem eigenen Thread) */
    private <T> T onUi(java.util.concurrent.Callable<T> c) {
        final Object[] out = new Object[1];
        final CountDownLatch l = new CountDownLatch(1);
        runOnUiThread(() -> {
            try { out[0] = c.call(); } catch (Exception e) { out[0] = null; }
            l.countDown();
        });
        try { l.await(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
        @SuppressWarnings("unchecked") T t = (T) out[0];
        return t;
    }

    private void jsUpdate(String arg) {
        runOnUiThread(() -> web.evaluateJavascript("window.stellenwegUpdate&&window.stellenwegUpdate(" + arg + ")", null));
    }

    private File writeCache(String b64, String name) throws Exception {
        File dir = new File(getCacheDir(), "share");
        dir.mkdirs();
        File f = new File(dir, name.replaceAll("[\\\\/:*?\"<>|]", "_"));
        try (FileOutputStream o = new FileOutputStream(f)) { o.write(Base64.decode(b64, Base64.DEFAULT)); }
        return f;
    }

    class Bridge {
        @JavascriptInterface public boolean isNative() { return true; }

        /** Apps mit Startsymbol, alphabetisch: [{pkg,label}] */
        @JavascriptInterface
        public String listApps() {
            PackageManager pm = getPackageManager();
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> L = pm.queryIntentActivities(i, 0);
            List<String[]> apps = new ArrayList<>();
            for (ResolveInfo r : L) {
                String pkg = r.activityInfo.packageName;
                if (pkg.equals(getPackageName())) continue;
                apps.add(new String[]{pkg, String.valueOf(r.loadLabel(pm))});
            }
            Collections.sort(apps, (a, b) -> a[1].compareToIgnoreCase(b[1]));
            JSONArray out = new JSONArray();
            try {
                for (String[] a : apps) out.put(new JSONObject().put("pkg", a[0]).put("label", a[1]));
            } catch (Exception ignored) { }
            return out.toString();
        }

        /** Echtes App-Symbol als PNG-Data-URL (96 px) */
        @JavascriptInterface
        public String appIcon(String pkg) {
            try {
                Drawable d = getPackageManager().getApplicationIcon(pkg);
                int px = 96;
                Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(b);
                d.setBounds(0, 0, px, px);
                d.draw(c);
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                b.compress(Bitmap.CompressFormat.PNG, 100, o);
                return "data:image/png;base64," + Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP);
            } catch (Exception e) {
                return "";
            }
        }

        /**
         * App öffnen: "float" = schwebendes Fenster (wenn das Handy Freiform-Fenster kann, z.B. ColorOS),
         * "split" = geteilter Bildschirm neben Stellenweg, sonst ganz. Fällt jeweils auf die nächste Art zurück.
         */
        @JavascriptInterface
        public boolean openApp(String pkg, String mode) {
            Boolean ok = onUi(() -> {
                Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
                if (i == null) return false;
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if ("float".equals(mode)) {
                    try {
                        DisplayMetrics dm = getResources().getDisplayMetrics();
                        int w = (int) (dm.widthPixels * .8), h = (int) (dm.heightPixels * .55);
                        int x = (dm.widthPixels - w) / 2, y = (int) (dm.heightPixels * .12);
                        ActivityOptions o = ActivityOptions.makeBasic();
                        o.setLaunchBounds(new Rect(x, y, x + w, y + h));
                        try {
                            // Freiform-Fenster (WINDOWING_MODE_FREEFORM = 5); nicht öffentliche Schnittstelle, darum geschützt
                            ActivityOptions.class.getMethod("setLaunchWindowingMode", int.class).invoke(o, 5);
                        } catch (Throwable ignored) { }
                        i.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        startActivity(i, o.toBundle());
                        return true;
                    } catch (Throwable t) { /* weiter mit geteiltem Bildschirm */ }
                }
                if ("float".equals(mode) || "split".equals(mode)) {
                    try {
                        Intent j = new Intent(i);
                        j.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        startActivity(j);
                        return true;
                    } catch (Throwable t) { /* weiter mit ganz */ }
                }
                startActivity(i);
                return true;
            });
            return ok != null && ok;
        }

        @JavascriptInterface
        public String readClipboard() {
            return onUi(() -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm == null || !cm.hasPrimaryClip()) return "";
                ClipData c = cm.getPrimaryClip();
                if (c == null || c.getItemCount() == 0) return "";
                CharSequence t = c.getItemAt(0).coerceToText(MainActivity.this);
                return t == null ? "" : t.toString();
            });
        }

        @JavascriptInterface
        public boolean copyText(String t) {
            runOnUiThread(() -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Stellenweg", t));
            });
            return true;
        }

        @JavascriptInterface
        public boolean shareText(String t) {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, t);
                startActivity(Intent.createChooser(i, "Teilen"));
            });
            return true;
        }

        /** PDF (oder andere Datei) mit einer App teilen, z.B. Mail oder WhatsApp */
        @JavascriptInterface
        public boolean sharePdf(String b64, String name, String mime) {
            try {
                File f = writeCache(b64, name);
                Uri u = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".files", f);
                Intent i = new Intent(Intent.ACTION_SEND).setType(mime == null || mime.isEmpty() ? "application/pdf" : mime)
                        .putExtra(Intent.EXTRA_STREAM, u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(() -> startActivity(Intent.createChooser(i, "Senden")));
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        /** Dokument mit der passenden App öffnen (PDF-Leser, Galerie …) */
        @JavascriptInterface
        public boolean openFile(String b64, String name, String mime) {
            try {
                File f = writeCache(b64, name);
                Uri u = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".files", f);
                Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(u, mime == null || mime.isEmpty() ? "application/pdf" : mime)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                // Ohne resolveActivity: ab Android 11 sähe die App fremde Apps nur mit <queries>; startActivity geht trotzdem
                runOnUiThread(() -> {
                    try { startActivity(i); } catch (ActivityNotFoundException e) {
                        Toast.makeText(MainActivity.this, "Keine App zum Öffnen gefunden", Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        /** Griff der Seitenleiste von der Zurück-Geste am rechten Rand ausnehmen (Android 10+).
         *  top und height in CSS-Pixeln der Seite; height 0 hebt die Ausnahme auf. */
        @JavascriptInterface
        public void setGestureExclusion(int top, int height, int cssWidth) {
            if (Build.VERSION.SDK_INT < 29) return;
            runOnUiThread(() -> {
                List<Rect> rects = new ArrayList<>();
                if (height > 0 && cssWidth > 0) {
                    float k = web.getWidth() / (float) cssWidth;
                    int w = web.getWidth(), band = Math.round(48 * k);
                    rects.add(new Rect(w - band, Math.round(top * k), w, Math.round((top + height) * k)));
                }
                web.setSystemGestureExclusionRects(rects);
            });
        }

        /** In „Downloads“ speichern */
        @JavascriptInterface
        public boolean savePdf(String b64, String name, String mime) {
            try {
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                String type = mime == null || mime.isEmpty() ? "application/pdf" : mime;
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    v.put(MediaStore.Downloads.MIME_TYPE, type);
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (u == null) return false;
                    try (OutputStream o = getContentResolver().openOutputStream(u)) { if (o != null) o.write(bytes); }
                } else {
                    File f = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), name);
                    try (FileOutputStream o = new FileOutputStream(f)) { o.write(bytes); }
                }
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "In „Downloads“ gespeichert", Toast.LENGTH_SHORT).show());
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void vibrate(int ms) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(Math.max(5, Math.min(ms, 60)), VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(Math.max(5, Math.min(ms, 60)));
        }

        /** true, wenn Mitteilungen erlaubt sind; fragt sonst (Android 13+) nach */
        @JavascriptInterface
        public boolean requestNotifications() {
            if (NotificationManagerCompat.from(MainActivity.this).areNotificationsEnabled()) return true;
            if (Build.VERSION.SDK_INT >= 33) {
                runOnUiThread(() -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY));
                return true;
            }
            return false;
        }

        @JavascriptInterface
        public void testNotification() {
            Reminders.show(MainActivity.this, "test", "Mitteilungen sind eingeschaltet", "Stellenweg erinnert Sie an Fristen, Termine und Warnungen.", "heute");
        }

        /** Erinnerungen als JSON [{key,title,body,go,at}] übernehmen und mit dem AlarmManager planen */
        @JavascriptInterface
        public void scheduleReminders(String json) {
            Reminders.schedule(MainActivity.this, json);
        }

        /** Installierte Versionsnummer (versionCode) für den Vergleich mit version.json */
        @JavascriptInterface
        public int appVersion() {
            try {
                android.content.pm.PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                return Build.VERSION.SDK_INT >= 28 ? (int) pi.getLongVersionCode() : pi.versionCode;
            } catch (Exception e) {
                return 0;
            }
        }

        /**
         * Neue APK von GitHub Pages laden und die Installation starten. Android fragt immer selbst nach;
         * beim ersten Mal muss die Person „Apps aus dieser Quelle installieren“ erlauben.
         * Fortschritt geht an window.stellenwegUpdate(prozent | 'fertig' | 'fehler').
         */
        @JavascriptInterface
        public boolean installUpdate(String url) {
            Uri u = Uri.parse(url);
            if (!"https".equals(u.getScheme()) || !"erwachsenenbildungdidaktik-blip.github.io".equals(u.getHost())) return false;
            if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Bitte „Apps aus dieser Quelle erlauben“ einschalten und dann nochmals auf Aktualisieren tippen.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
                });
                return false;
            }
            new Thread(() -> {
                java.net.HttpURLConnection c = null;
                try {
                    File dir = new File(getCacheDir(), "update");
                    dir.mkdirs();
                    File f = new File(dir, "Stellenweg.apk");
                    c = (java.net.HttpURLConnection) new java.net.URL(url + (url.contains("?") ? "&" : "?") + "v=" + System.currentTimeMillis()).openConnection();
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(30000);
                    int len = c.getContentLength(), done = 0, last = -1;
                    try (java.io.InputStream in = c.getInputStream(); FileOutputStream o = new FileOutputStream(f)) {
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            o.write(buf, 0, n);
                            done += n;
                            int pct = len > 0 ? done * 100 / len : -1;
                            if (pct >= 0 && pct / 10 != last / 10) { last = pct; jsUpdate(String.valueOf(pct)); }
                        }
                    }
                    Uri apk = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".files", f);
                    Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(apk, "application/vnd.android.package-archive")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    jsUpdate("'fertig'");
                    runOnUiThread(() -> startActivity(i));
                } catch (Exception e) {
                    jsUpdate("'fehler'");
                } finally {
                    if (c != null) c.disconnect();
                }
            }).start();
            return true;
        }

        /** Geteilte Dateien als JSON (danach leer) */
        @JavascriptInterface
        public String takeSharedFiles() {
            String f = takeFiles();
            return f == null ? "" : f;
        }

        /** Beim Kaltstart geteilter Text (danach leer) */
        @JavascriptInterface
        public String takeSharedText() {
            String t = sharedText;
            sharedText = null;
            return t == null ? "" : t;
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] p, int[] r) {
        super.onRequestPermissionsResult(req, p, r);
        if (req == REQ_NOTIFY && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED)
            Reminders.reschedule(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
}
