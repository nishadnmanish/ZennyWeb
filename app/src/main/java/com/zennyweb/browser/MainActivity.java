package com.zennyweb.browser;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.content.Context;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

public class MainActivity extends Activity {
    private static GeckoRuntime runtime;
    private GeckoSession session;
    private GeckoView browser;
    private EditText address;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (runtime == null) {
            runtime = GeckoRuntime.create(this);
        }

        session = new GeckoSession();
        session.setContentDelegate(new GeckoSession.ContentDelegate() {});
        session.open(runtime);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(15, 18, 30));

        TextView brand = new TextView(this);
        brand.setText("  ZennyWeb");
        brand.setTextSize(25);
        brand.setTextColor(Color.WHITE);
        brand.setPadding(dp(12), dp(14), dp(12), dp(12));
        brand.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(brand);

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

        bar.addView(address, new LinearLayout.LayoutParams(
            0, dp(48), 1
        ));

        Button go = new Button(this);
        go.setText("Go");
        bar.addView(go, new LinearLayout.LayoutParams(
            dp(64), dp(48)
        ));

        root.addView(bar);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setPadding(dp(4), 0, dp(4), dp(4));

        addControl(controls, "Back", v -> session.goBack());
        addControl(controls, "Forward", v -> session.goForward());
        addControl(controls, "Reload", v -> session.reload());
        addControl(controls, "Home", v -> loadHome());

        root.addView(controls);

        browser = new GeckoView(this);
        browser.setSession(session);
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

        loadHome();
    }

    private void addControl(
        LinearLayout parent, String label, View.OnClickListener listener
    ) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(11);
        parent.addView(button, new LinearLayout.LayoutParams(
            0, dp(44), 1
        ));
        button.setOnClickListener(listener);
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
                    java.net.URLEncoder.encode(input, "UTF-8");
            } catch (Exception e) {
                url = "https://www.google.com/";
            }
        }

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

    @Override
    public void onBackPressed() {
        session.goBack();
    }

    @Override
    protected void onDestroy() {
        if (isFinishing() && session != null) {
            session.close();
        }
        super.onDestroy();
    }
}
