package com.zennyweb.browser;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

import java.net.URLEncoder;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static GeckoRuntime runtime;
    private final ArrayList<GeckoSession> sessions = new ArrayList<>();
    private final ArrayList<String> tabUrls = new ArrayList<>();
    private GeckoSession session;
    private GeckoView browser;
    private EditText address;
    private LinearLayout root;
    private LinearLayout controls;
    private LinearLayout tabBar;
    private SharedPreferences bookmarks;
    private int currentTab = 0;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (runtime == null) runtime = GeckoRuntime.create(this);
        bookmarks = getSharedPreferences("zenny_bookmarks", MODE_PRIVATE);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(15, 18, 30));

        TextView brand = new TextView(this);
        brand.setText("  ZennyWeb");
        brand.setTextSize(25);
        brand.setTextColor(Color.WHITE);
        brand.setPadding(dp(12), dp(10), dp(12), dp(10));
        brand.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(brand);

        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(tabBar);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(8), dp(4), dp(8), dp(8));

        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextColor(Color.BLACK);
        address.setHintTextColor(Color.DKGRAY);
        address.setHint("Search or enter website");
        address.setTextSize(15);
        address.setPadding(dp(12), 0, dp(8), 0);
        address.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(Color.WHITE)
        );
        address.setInputType(
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
        );
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        bar.addView(address, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button go = new Button(this);
        go.setText("Go");
        bar.addView(go, new LinearLayout.LayoutParams(dp(64), dp(48)));
        root.addView(bar);

        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setPadding(dp(4), 0, dp(4), dp(4));
        addControl(controls, "Back", v -> {
            if (session != null) session.goBack();
        });
        addControl(controls, "Forward", v -> {
            if (session != null) session.goForward();
        });
        addControl(controls, "Reload", v -> {
            if (session != null) session.reload();
        });
        addControl(controls, "Home", v -> loadHome());
        root.addView(controls);

        LinearLayout extras = new LinearLayout(this);
        extras.setOrientation(LinearLayout.HORIZONTAL);
        addControl(extras, "+ Tab", v -> newTab("https://www.google.com/"));
        addControl(extras, "Tabs", v -> showTabs());
        addControl(extras, "Bookmark", v -> saveBookmark());
        addControl(extras, "Saved", v -> showBookmarks());
        root.addView(extras);

        browser = new GeckoView(this);
        root.addView(browser, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1
        ));

        setContentView(root);

        go.setOnClickListener(v -> navigate());
        address.setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO ||
                action == EditorInfo.IME_ACTION_DONE) {
                navigate();
                return true;
            }
            return false;
        });

        newTab("https://www.google.com/");
    }

    private void addControl(
        LinearLayout parent, String label, View.OnClickListener listener
    ) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(10);
        parent.addView(button, new LinearLayout.LayoutParams(0, dp(42), 1));
        button.setOnClickListener(listener);
    }

    private void newTab(String url) {
        GeckoSession newSession = new GeckoSession();
        newSession.setContentDelegate(new GeckoSession.ContentDelegate() {});
        newSession.open(runtime);
        sessions.add(newSession);
        tabUrls.add(url);
        currentTab = sessions.size() - 1;
        session = newSession;
        browser.setSession(session);
        refreshTabs();
        session.loadUri(url);
    }

    private void switchTab(int index) {
        if (index < 0 || index >= sessions.size()) return;
        currentTab = index;
        session = sessions.get(index);
        browser.releaseSession();
        browser.setSession(session);
        refreshTabs();
    }

    private void refreshTabs() {
        tabBar.removeAllViews();
        for (int i = 0; i < sessions.size(); i++) {
            final int index = i;
            Button tab = new Button(this);
            tab.setText((i == currentTab ? "● " : "") + "Tab " + (i + 1));
            tab.setTextSize(10);
            tabBar.addView(tab, new LinearLayout.LayoutParams(0, dp(38), 1));
            tab.setOnClickListener(v -> switchTab(index));
        }
    }

    private void showTabs() {
        if (sessions.isEmpty()) return;
        String[] names = new String[sessions.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = "Tab " + (i + 1) + (i == currentTab ? " (current)" : "");
        }
        new AlertDialog.Builder(this)
            .setTitle("Open tabs")
            .setItems(names, (dialog, which) -> switchTab(which))
            .setNegativeButton("Close current tab", (dialog, which) -> closeCurrentTab())
            .setPositiveButton("New tab", (dialog, which) ->
                newTab("https://www.google.com/"))
            .show();
    }

    private void closeCurrentTab() {
        if (sessions.size() <= 1) {
            Toast.makeText(this, "Keep at least one tab open", Toast.LENGTH_SHORT).show();
            return;
        }
        GeckoSession old = sessions.remove(currentTab);
        tabUrls.remove(currentTab);
        browser.releaseSession();
        old.close();
        currentTab = Math.max(0, currentTab - 1);
        session = sessions.get(currentTab);
        browser.setSession(session);
        refreshTabs();
    }

    private void navigate() {
        String input = address.getText().toString().trim();
        if (input.isEmpty()) return;

        String url;
        if (input.matches("(?i)^https?://.*")) {
            url = input;
        } else if (input.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}(:\\d+)?(/.*)?$")) {
            url = "https://" + input;
        } else {
            try {
                url = "https://www.google.com/search?q=" +
                    URLEncoder.encode(input, "UTF-8");
            } catch (Exception e) {
                url = "https://www.google.com/";
            }
        }

        tabUrls.set(currentTab, url);
        session.loadUri(url);
        address.clearFocus();
        ((android.view.inputmethod.InputMethodManager)
            getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(address.getWindowToken(), 0);
    }

    private void loadHome() {
        address.setText("");
        session.loadUri("https://www.google.com/");
    }

    private void saveBookmark() {
        String url = tabUrls.get(currentTab);
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Open a website first", Toast.LENGTH_SHORT).show();
            return;
        }
        String key = "bookmark_" + url.hashCode();
        bookmarks.edit().putString(key, url).apply();
        Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show();
    }

    private void showBookmarks() {
        java.util.Map<String, ?> all = bookmarks.getAll();
        ArrayList<String> urls = new ArrayList<>();
        for (Object value : all.values()) {
            if (value instanceof String) urls.add((String) value);
        }
        if (urls.isEmpty()) {
            Toast.makeText(this, "No bookmarks saved yet", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = urls.toArray(new String[0]);
        new AlertDialog.Builder(this)
            .setTitle("Saved bookmarks")
            .setItems(items, (dialog, which) -> {
                String url = urls.get(which);
                tabUrls.set(currentTab, url);
                session.loadUri(url);
                address.setText(url);
            })
            .setNegativeButton("Close", null)
            .show();
    }

    @Override
    public void onBackPressed() {
        if (session != null) session.goBack();
        else super.onBackPressed();
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
