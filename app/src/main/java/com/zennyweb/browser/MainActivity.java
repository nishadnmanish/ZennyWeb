package com.zennyweb.browser;

import android.app.Activity;
import java.net.URI;
import java.io.FileOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.InputStream;
import android.provider.MediaStore;
import android.os.Environment;
import android.net.Uri;
import android.content.ContentValues;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;


import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends Activity {
    private static GeckoRuntime runtime;
    private final ArrayList<GeckoSession> sessions = new ArrayList<>();
    private final ArrayList<String> urls = new ArrayList<>();

    private LinearLayout root;
    private EditText address;
    private FrameLayout content;
    private LinearLayout homePage;
    private GeckoView browser;
    private GeckoSession session;
    private SharedPreferences prefs;
    private int currentTab = 0;

    private final String HOME = "zennyhome://home";
    private final int BG = Color.rgb(24, 23, 29);
    private final int PANEL = Color.rgb(48, 46, 54);
    private final int WHITE = Color.rgb(245, 244, 248);

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(WHITE);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(PANEL)
        );
        b.setOnClickListener(v -> action.run());
        return b;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        if (runtime == null) runtime = GeckoRuntime.create(this);
        prefs = getSharedPreferences("zennyweb_data", MODE_PRIVATE);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // Firefox-inspired top address bar
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(10), dp(8), dp(8), dp(8));

        LinearLayout addressBox = new LinearLayout(this);
        addressBox.setGravity(Gravity.CENTER_VERTICAL);
        addressBox.setPadding(dp(12), 0, dp(6), 0);
        addressBox.setBackground(shape(PANEL, 32));

        TextView globe = text("🌐", 20, WHITE);
        addressBox.addView(globe);

        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextSize(16);
        address.setTextColor(WHITE);
        address.setHintTextColor(Color.LTGRAY);
        address.setHint("Search or enter address");
        address.setBackgroundColor(Color.TRANSPARENT);
        address.setPadding(dp(8), 0, dp(4), 0);
        address.setInputType(InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_VARIATION_URI);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBox.addView(address, new LinearLayout.LayoutParams(
            0, dp(48), 1
        ));

        addressBox.setOnClickListener(v -> address.requestFocus());
        top.addView(addressBox, new LinearLayout.LayoutParams(0, dp(52), 1));

        Button tabCount = button("1", this::showTabs);
        top.addView(tabCount, new LinearLayout.LayoutParams(dp(54), dp(48)));

        Button menu = button("⋮", this::showMenu);
        menu.setTextSize(24);
        top.addView(menu, new LinearLayout.LayoutParams(dp(46), dp(48)));
        root.addView(top);

        // Browser and custom homepage share the same area
        content = new FrameLayout(this);

        browser = new GeckoView(this);
        content.addView(browser, new FrameLayout.LayoutParams(
            -1, -1
        ));

        homePage = buildHomePage();
        content.addView(homePage, new FrameLayout.LayoutParams(
            -1, -1
        ));

        root.addView(content, new LinearLayout.LayoutParams(
            -1, 0, 1
        ));



        setContentView(root);

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                android.graphics.Insets bars =
                    windowInsets.getInsets(
                        android.view.WindowInsets.Type.systemBars()
                    );
                view.setPadding(0, bars.top, 0, bars.bottom);
                return windowInsets;
            });
        } else {
            root.setPadding(0, dp(24), 0, dp(24));
        }
        root.requestApplyInsets();

        address.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO ||
                action == EditorInfo.IME_ACTION_DONE) {
                navigate();
                return true;
            }
            return false;
        });

        newTab(HOME);
    }

    private LinearLayout buildHomePage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(20));
        page.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] {
                Color.rgb(225, 145, 44),
                Color.rgb(155, 67, 31),
                Color.rgb(49, 31, 39)
            }
        ));

        TextView logo = text("🌐", 66, WHITE);
        logo.setGravity(Gravity.CENTER);
        page.addView(logo, new LinearLayout.LayoutParams(-1, dp(100)));

        TextView title = text("ZennyWeb", 32, Color.BLACK);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        page.addView(title);

        TextView subtitle = text("Your web, your way", 15, WHITE);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(4), 0, dp(24));
        page.addView(subtitle);

        TextView shortcuts = text("Shortcuts", 21, Color.BLACK);
        shortcuts.setPadding(0, dp(8), 0, dp(12));
        page.addView(shortcuts);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        addShortcut(row, "G", "Google", "https://www.google.com/");
        addShortcut(row, "▶", "YouTube", "https://m.youtube.com/");
        addShortcut(row, "W", "Wikipedia", "https://www.wikipedia.org/");
        page.addView(row);

        TextView tip = text("Search the web or enter a website above",
            14, WHITE);
        tip.setGravity(Gravity.CENTER);
        tip.setPadding(0, dp(28), 0, dp(12));
        page.addView(tip);

        Button saved = button("★  Saved bookmarks", this::showBookmarks);
        page.addView(saved, new LinearLayout.LayoutParams(-1, dp(48)));

        return page;
    }

    private void addShortcut(LinearLayout row, String symbol,
                             String label, String url) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(4), dp(8), dp(4), dp(8));

        TextView icon = text(symbol, 27, WHITE);
        icon.setTypeface(null, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(BG, 50));
        item.addView(icon, new LinearLayout.LayoutParams(dp(60), dp(60)));

        TextView name = text(label, 12, Color.BLACK);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(6), 0, 0);
        item.addView(name);

        item.setOnClickListener(v -> openUrl(url));
        row.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void newTab(String url) {
        GeckoSession s = new GeckoSession();
        s.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onExternalResponse(
                GeckoSession source,
                org.mozilla.geckoview.WebResponse response
            ) {
                startDownload(response);
            }
        });
        s.open(runtime);
        sessions.add(s);
        urls.add(url);
        currentTab = sessions.size() - 1;
        session = s;

        browser.releaseSession();
        browser.setSession(session);

        if (HOME.equals(url)) {
            browser.setVisibility(View.GONE);
            homePage.setVisibility(View.VISIBLE);
            address.setText("");
        } else {
            browser.setVisibility(View.VISIBLE);
            homePage.setVisibility(View.GONE);
            session.loadUri(url);
            address.setText(url);
        }
    }

    private void switchTab(int index) {
        if (index < 0 || index >= sessions.size()) return;
        currentTab = index;
        session = sessions.get(index);
        browser.releaseSession();
        browser.setSession(session);

        String url = urls.get(index);
        if (HOME.equals(url)) {
            browser.setVisibility(View.GONE);
            homePage.setVisibility(View.VISIBLE);
            address.setText("");
        } else {
            browser.setVisibility(View.VISIBLE);
            homePage.setVisibility(View.GONE);
            address.setText(url);
        }
    }

    private void showTabs() {
        String[] names = new String[sessions.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = (i == currentTab ? "✓ " : "") + "Tab " + (i + 1)
                + " — " + (HOME.equals(urls.get(i)) ? "Home" : urls.get(i));
        }

        new AlertDialog.Builder(this)
            .setTitle("Tabs (" + sessions.size() + ")")
            .setItems(names, (d, which) -> switchTab(which))
            .setNeutralButton("Close current", (d, w) -> closeTab())
            .setPositiveButton("+ New tab", (d, w) -> newTab(HOME))
            .show();
    }

    private void closeTab() {
        if (sessions.size() <= 1) {
            Toast.makeText(this, "At least one tab must remain",
                Toast.LENGTH_SHORT).show();
            return;
        }

        GeckoSession old = sessions.remove(currentTab);
        urls.remove(currentTab);
        browser.releaseSession();
        old.close();
        currentTab = Math.max(0, currentTab - 1);
        switchTab(currentTab);
    }

    private void navigate() {
        String input = address.getText().toString().trim();
        if (input.isEmpty()) return;

        String url;
        if (input.matches("(?i)^https?://.*")) {
            url = input;
        } else if (input.matches(
            "(?i)^[a-z0-9.-]+\\.[a-z]{2,}(:\\d+)?(/.*)?$")) {
            url = "https://" + input;
        } else {
            try {
                String engine = prefs.getString("search_engine", "Google");
                String base;
                switch (engine) {
                    case "Bing":
                        base = "https://www.bing.com/search?q=";
                        break;
                    case "DuckDuckGo":
                        base = "https://duckduckgo.com/?q=";
                        break;
                    case "Brave":
                        base = "https://search.brave.com/search?q=";
                        break;
                    default:
                        base = "https://www.google.com/search?q=";
                }
                url = base + URLEncoder.encode(input, "UTF-8");
            } catch (Exception e) {
                url = "https://www.google.com/";
            }
        }
        openUrl(url);
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(address.getWindowToken(), 0);
        address.clearFocus();
    }

    private void openUrl(String url) {
        urls.set(currentTab, url);
        if (url != null && url.startsWith("http")) {
            prefs.edit()
                .putString("hist_" + System.currentTimeMillis(), url)
                .apply();
        }
        address.setText(url);
        homePage.setVisibility(View.GONE);
        browser.setVisibility(View.VISIBLE);
        session.loadUri(url);
    }

    private void goHome() {
        urls.set(currentTab, HOME);
        address.setText("");
        browser.setVisibility(View.GONE);
        homePage.setVisibility(View.VISIBLE);
    }

    private void saveBookmark() {
        String url = urls.get(currentTab);
        if (HOME.equals(url)) {
            Toast.makeText(this, "Open a website first", Toast.LENGTH_SHORT).show();
            return;
        }
        String key = "bm_" + url.hashCode();
        prefs.edit().putString(key, url).apply();
        Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show();
    }

    private void showBookmarks() {
        ArrayList<String> saved = new ArrayList<>();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            if (e.getKey().startsWith("bm_") && e.getValue() instanceof String) {
                saved.add((String)e.getValue());
            }
        }

        if (saved.isEmpty()) {
            Toast.makeText(this, "No saved bookmarks yet",
                Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = saved.toArray(new String[0]);
        new AlertDialog.Builder(this)
            .setTitle("Bookmarks")
            .setItems(items, (d, which) -> openUrl(saved.get(which)))
            .setNeutralButton("Remove all", (d, w) -> {
                for (String item : saved) {
                    prefs.edit().remove("bm_" + item.hashCode()).apply();
                }
                Toast.makeText(this, "Bookmarks removed",
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void showHistory() {
        ArrayList<String> history = new ArrayList<>();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            if (e.getKey().startsWith("hist_") && e.getValue() instanceof String) {
                history.add((String)e.getValue());
            }
        }
        if (history.isEmpty()) {
            Toast.makeText(this, "No history yet", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("History")
            .setItems(history.toArray(new String[0]),
                (d, which) -> openUrl(history.get(which)))
            .setNegativeButton("Close", null)
            .show();
    }


    private String safeFileName(String raw) {
        String name = raw == null ? "download" : raw;
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            name = "download";
        }
        return name;
    }

    private void startDownload(org.mozilla.geckoview.WebResponse response) {
        if (response == null || response.body == null) {
            if (response != null && response.body == null) {
                Toast.makeText(this, "No downloadable file data was provided",
                    Toast.LENGTH_LONG).show();
            }
            return;
        }

        String name = "download_" + System.currentTimeMillis();
        try {
            String path = Uri.parse(response.uri).getLastPathSegment();
            if (path != null && !path.trim().isEmpty()) {
                name = java.net.URLDecoder.decode(path, "UTF-8");
            }
        } catch (Exception ignored) {
        }
        name = safeFileName(name);

        final String fileName = name;
        final String recordKey = "dl_" + System.currentTimeMillis();

        prefs.edit().putString(recordKey, fileName + " | Downloading").apply();
        Toast.makeText(this, "Downloading: " + fileName,
            Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            Uri savedUri = null;
            OutputStream out = null;
            InputStream in = response.body;
            boolean success = false;
            String error = "";

            try {
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE,
                        "application/octet-stream");
                    values.put(MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/ZennyWeb");
                    values.put(MediaStore.Downloads.IS_PENDING, 1);

                    savedUri = getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (savedUri == null) {
                        throw new java.io.IOException(
                            "Could not create Downloads file");
                    }
                    out = getContentResolver().openOutputStream(savedUri);
                } else {
                    File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    if (dir == null) throw new java.io.IOException(
                        "Downloads folder unavailable");
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(dir, fileName);
                    out = new FileOutputStream(file);
                    savedUri = Uri.fromFile(file);
                }

                if (out == null) throw new java.io.IOException(
                    "Could not open output file");

                byte[] buffer = new byte[16384];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.flush();
                success = true;
            } catch (Exception e) {
                error = e.getMessage() == null ? "Download failed" : e.getMessage();
            } finally {
                try { if (in != null) in.close(); } catch (Exception ignored) {}
                try { if (out != null) out.close(); } catch (Exception ignored) {}
            }

            final boolean completed = success;
            final String message = error;
            final Uri finalUri = savedUri;

            if (completed && finalUri != null &&
                android.os.Build.VERSION.SDK_INT >= 29) {
                ContentValues done = new ContentValues();
                done.put(MediaStore.Downloads.IS_PENDING, 0);
                try {
                    getContentResolver().update(finalUri, done, null, null);
                } catch (Exception ignored) {
                }
            }

            String record = fileName + " | " +
                (completed ? "Completed" : "Failed: " + message) +
                (finalUri == null ? "" : " | " + finalUri.toString());
            prefs.edit().putString(recordKey, record).apply();

            runOnUiThread(() -> Toast.makeText(
                this,
                completed ? "Download complete: " + fileName
                    : "Download failed: " + message,
                Toast.LENGTH_LONG
            ).show());
        }).start();
    }

    private void showDownloads() {
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getKey().startsWith("dl_") &&
                entry.getValue() instanceof String) {
                keys.add(entry.getKey());
                labels.add((String) entry.getValue());
            }
        }

        java.util.Collections.sort(keys, java.util.Collections.reverseOrder());
        labels.clear();
        for (String key : keys) labels.add(prefs.getString(key, "Download"));

        if (labels.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("Downloads")
                .setMessage("No downloads yet. Download a file from a website and it will appear here.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Downloads")
            .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                String record = labels.get(which);
                int pos = record.lastIndexOf(" | content://");
                if (pos >= 0) {
                    String uriText = record.substring(pos + 3);
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(uriText),
                            "*/*");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } catch (Exception e) {
                        info("File saved, but no app could open it.");
                    }
                } else {
                    info(record);
                }
            })
            .setNeutralButton("Clear list", (dialog, which) -> {
                for (String key : new ArrayList<>(prefs.getAll().keySet())) {
                    if (key.startsWith("dl_")) prefs.edit().remove(key).apply();
                }
                Toast.makeText(this, "Download list cleared",
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void showMenu() {
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(16), dp(12), dp(16), dp(20));
        sheet.setBackground(shape(BG, 28));

        TextView handle = text("━", 20, Color.LTGRAY);
        handle.setGravity(Gravity.CENTER);
        sheet.addView(handle, new LinearLayout.LayoutParams(-1, dp(30)));

        sheet.addView(menuRow("♧   Extensions",
            () -> info("Extension support is not implemented yet.")));

        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        shortcuts.addView(menuButton("◷\nHistory", this::showHistory));
        shortcuts.addView(menuButton("★\nBookmarks", this::showBookmarks));
        shortcuts.addView(menuButton("↓\nDownloads", this::showDownloads));
        shortcuts.addView(menuButton("⚿\nPasswords",
            () -> info("Password manager is not implemented yet.")));
        sheet.addView(shortcuts);

        sheet.addView(menuRow("☻   ZennyWeb account",
            () -> info("Account sync is not available yet.")));
        sheet.addView(menuRow("⌂   Customise homepage",
            () -> info("Homepage customization is coming next.")));
        sheet.addView(menuRow("⚙   Settings", this::showSettings));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(sheet)
            .create();
        dialog.show();
    }

    private View menuRow(String label, Runnable action) {
        TextView row = text(label, 16, WHITE);
        row.setPadding(dp(18), 0, dp(12), 0);
        row.setBackground(shape(PANEL, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(58));
        lp.setMargins(0, dp(5), 0, dp(5));
        row.setLayoutParams(lp);
        row.setOnClickListener(v -> action.run());
        return row;
    }

    private View menuButton(String label, Runnable action) {
        TextView b = text(label, 13, WHITE);
        b.setGravity(Gravity.CENTER);
        b.setBackground(shape(PANEL, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(82), 1);
        lp.setMargins(dp(2), dp(8), dp(2), dp(8));
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void showSettings() {
        String[] sections = {
            "General",
            "Search engine",
            "Homepage",
            "Appearance",
            "Privacy & security",
            "Clear browsing history",
            "Clear saved bookmarks",
            "Bookmarks",
            "History",
            "Downloads",
            "About ZennyWeb"
        };

        new AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(sections, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showGeneralSettings();
                        break;
                    case 1:
                        showSearchEngineSettings();
                        break;
                    case 2:
                        showHomepageSettings();
                        break;
                    case 3:
                        showThemeSettings();
                        break;
                    case 4:
                        showPrivacySettings();
                        break;
                    case 5:
                        new AlertDialog.Builder(this)
                            .setTitle("Clear browsing history?")
                            .setMessage("This removes the history saved by ZennyWeb.")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Clear", (d, w) -> {
                                for (String key : new ArrayList<>(prefs.getAll().keySet())) {
                                    if (key.startsWith("hist_")) {
                                        prefs.edit().remove(key).apply();
                                    }
                                }
                                info("Browsing history cleared.");
                            }).show();
                        break;
                    case 6:
                        new AlertDialog.Builder(this)
                            .setTitle("Clear bookmarks?")
                            .setMessage("This removes all saved ZennyWeb bookmarks.")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Clear", (d, w) -> {
                                for (String key : new ArrayList<>(prefs.getAll().keySet())) {
                                    if (key.startsWith("bm_")) {
                                        prefs.edit().remove(key).apply();
                                    }
                                }
                                info("Bookmarks cleared.");
                            }).show();
                        break;
                    case 7:
                        showBookmarks();
                        break;
                    case 8:
                        showHistory();
                        break;
                    case 9:
                        showDownloads();
                        break;
                    case 10:
                        info("ZennyWeb — GeckoView browser");
                        break;
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void showGeneralSettings() {
        String homepage = prefs.getString("homepage", "Google");
        String engine = prefs.getString("search_engine", "Google");
        String theme = prefs.getString("theme", "System default");
        info("General settings\n\nHomepage: " + homepage
            + "\nSearch engine: " + engine
            + "\nAppearance: " + theme
            + "\n\nChoose each option from the main Settings menu.");
    }

    private void showSearchEngineSettings() {
        String[] engines = {"Google", "Bing", "DuckDuckGo", "Brave"};
        String current = prefs.getString("search_engine", "Google");
        int selected = 0;
        for (int i = 0; i < engines.length; i++) {
            if (engines[i].equals(current)) selected = i;
        }
        new AlertDialog.Builder(this)
            .setTitle("Default search engine")
            .setSingleChoiceItems(engines, selected, (d, which) -> {
                prefs.edit().putString("search_engine", engines[which]).apply();
                d.dismiss();
                Toast.makeText(this, "Search engine: " + engines[which],
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showHomepageSettings() {
        String[] choices = {"Google", "ZennyWeb home"};
        String current = prefs.getString("homepage", "Google");
        int selected = current.equals("ZennyWeb home") ? 1 : 0;
        new AlertDialog.Builder(this)
            .setTitle("Homepage preference")
            .setSingleChoiceItems(choices, selected, (d, which) -> {
                prefs.edit().putString("homepage", choices[which]).apply();
                d.dismiss();
                info("Homepage preference saved. Use the Home button to return to ZennyWeb home.");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showThemeSettings() {
        String[] choices = {"System default", "Light", "Dark"};
        String current = prefs.getString("theme", "System default");
        int selected = 0;
        for (int i = 0; i < choices.length; i++) {
            if (choices[i].equals(current)) selected = i;
        }
        new AlertDialog.Builder(this)
            .setTitle("Appearance")
            .setSingleChoiceItems(choices, selected, (d, which) -> {
                prefs.edit().putString("theme", choices[which]).apply();
                d.dismiss();
                info("Appearance preference saved. Full light/dark UI switching will be connected in a later update.");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showPrivacySettings() {
        String[] choices = {
            "Enhanced tracking protection: Standard",
            "Enhanced tracking protection: Strict",
            "Enhanced tracking protection: Off",
            "Clear browsing history"
        };
        String current = prefs.getString("tracking_protection", "Standard");
        int selected = current.equals("Strict") ? 1 : current.equals("Off") ? 2 : 0;
        new AlertDialog.Builder(this)
            .setTitle("Privacy & security")
            .setSingleChoiceItems(choices, selected, (d, which) -> {
                if (which == 3) {
                    d.dismiss();
                    showSettings();
                    return;
                }
                String level = which == 1 ? "Strict" : which == 2 ? "Off" : "Standard";
                prefs.edit().putString("tracking_protection", level).apply();
                d.dismiss();
                info("Tracking protection preference saved as " + level
                    + ". Enforcement by GeckoView still needs to be connected.");
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void info(String message) {
        new AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }

    @Override
    public void onBackPressed() {
        if (homePage != null && homePage.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
        } else if (session != null) {
            session.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            for (GeckoSession s : sessions) {
                try { s.close(); } catch (Exception ignored) {}
            }
            sessions.clear();
        }
        super.onDestroy();
    }
}
