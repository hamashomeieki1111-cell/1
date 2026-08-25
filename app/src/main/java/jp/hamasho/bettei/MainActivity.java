package jp.hamasho.bettei;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int BG = Color.rgb(23, 19, 15);
    private static final int CARD = Color.rgb(38, 32, 26);
    private static final int CARD_ALT = Color.rgb(48, 40, 31);
    private static final int GOLD = Color.rgb(214, 176, 96);
    private static final int TEXT = Color.rgb(246, 241, 231);
    private static final int MUTED = Color.rgb(188, 175, 154);
    private static final int DIVIDER = Color.rgb(79, 67, 53);

    private static final String MEMBER_ID = "HMB-000001";
    private static final String SUPABASE_URL = "https://sedprfuiymcgbhatofwb.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_BIJQSq4IQRxgwwqWd3YmTQ_etujBnSj";

    private static final int PAGE_HOME = 0;
    private static final int PAGE_MEMBER = 1;
    private static final int PAGE_COUPON = 2;
    private static final int PAGE_STORE = 3;

    private LinearLayout content;
    private Button homeNav, memberNav, couponNav, storeNav;
    private SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private int currentPage = PAGE_HOME;
    private boolean syncing = false;
    private String syncText = "Supabase接続待ち";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("hamasho_trial", MODE_PRIVATE);
        ensureDefaults();
        buildShell();
        showHome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncFromSupabase();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void ensureDefaults() {
        if (!prefs.contains("available")) {
            prefs.edit()
                    .putInt("available", 428)
                    .putInt("cumulative", 8750)
                    .putBoolean("coupon_used", false)
                    .apply();
        }
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(14), dp(20), dp(11));
        header.setBackgroundColor(Color.rgb(18, 15, 12));

        TextView brand = text("濱匠別邸", 28, GOLD, true);
        brand.setGravity(Gravity.CENTER);
        header.addView(brand, matchWrap());

        TextView tagline = text("蕎麦と酒を、粋に愉しむ。", 12, MUTED, false);
        tagline.setGravity(Gravity.CENTER);
        header.addView(tagline, topMargin(dp(3)));

        TextView demo = text("社長確認用・同期DEMO", 10, Color.rgb(34, 28, 21), true);
        demo.setGravity(Gravity.CENTER);
        demo.setPadding(dp(10), dp(4), dp(10), dp(4));
        demo.setBackground(roundRect(GOLD, 20));
        LinearLayout.LayoutParams demoLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        demoLp.gravity = Gravity.CENTER_HORIZONTAL;
        demoLp.topMargin = dp(7);
        header.addView(demo, demoLp);
        root.addView(header, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(6), dp(7), dp(6), dp(8));
        nav.setBackgroundColor(Color.rgb(18, 15, 12));
        nav.setFitsSystemWindows(true);

        homeNav = navButton("ホーム");
        memberNav = navButton("会員証");
        couponNav = navButton("クーポン");
        storeNav = navButton("店舗");
        nav.addView(homeNav, weighted());
        nav.addView(memberNav, weighted());
        nav.addView(couponNav, weighted());
        nav.addView(storeNav, weighted());

        homeNav.setOnClickListener(v -> showHome());
        memberNav.setOnClickListener(v -> showMember());
        couponNav.setOnClickListener(v -> showCoupon());
        storeNav.setOnClickListener(v -> showStore());

        root.addView(nav, matchWrap());
        setContentView(root);
    }

    private void showHome() {
        currentPage = PAGE_HOME;
        content.removeAllViews();
        selectNav(homeNav);
        addSyncCard();

        int available = prefs.getInt("available", 0);
        int cumulative = prefs.getInt("cumulative", 0);
        RankInfo rank = rankInfo(cumulative);

        LinearLayout memberCard = card(CARD_ALT, 18);
        memberCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text("HAMASHO MEMBER", 13, MUTED, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(badge(rank.name));
        memberCard.addView(top, matchWrap());
        memberCard.addView(text(available + " pt", 32, GOLD, true), topMargin(dp(10)));
        memberCard.addView(text("利用可能ポイント", 12, MUTED, false), matchWrap());
        memberCard.addView(text("累計 " + cumulative + " pt", 14, TEXT, false), topMargin(dp(12)));
        memberCard.addView(text(rank.nextText, 13, MUTED, false), topMargin(dp(5)));

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(rank.progressMax);
        progress.setProgress(rank.progressValue);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(DIVIDER));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7));
        p.topMargin = dp(9);
        memberCard.addView(progress, p);
        memberCard.setOnClickListener(v -> showMember());
        content.addView(memberCard, topMargin(dp(12)));

        content.addView(sectionTitle("今月の会員特典"), topMargin(dp(22)));
        LinearLayout coupon = card(Color.rgb(59, 42, 28), 16);
        coupon.setPadding(dp(18), dp(16), dp(18), dp(16));
        coupon.addView(text(currentMonth() + "月限定クーポン", 13, GOLD, true), matchWrap());
        coupon.addView(text(couponText(rank.name), 20, TEXT, true), topMargin(dp(7)));
        boolean used = prefs.getBoolean("coupon_used", false);
        coupon.addView(text(used ? "使用済み" : "未使用・月1回", 12, used ? MUTED : GOLD, true), topMargin(dp(9)));
        coupon.setOnClickListener(v -> showCoupon());
        content.addView(coupon, topMargin(dp(9)));

        content.addView(sectionTitle("濱匠別邸を愉しむ"), topMargin(dp(22)));
        content.addView(tileRow("季節のおすすめ", "旬の食材・限定料理", "宴会・接待", "コース・お席のご案内"), topMargin(dp(9)));
        content.addView(tileRow("日本酒", "季節酒・おすすめ銘柄", "おすすめドリンク", "ビール・焼酎・ハイボール"), topMargin(dp(10)));

        LinearLayout flow = card(CARD, 16);
        flow.setPadding(dp(18), dp(16), dp(18), dp(16));
        flow.addView(text("同期デモの流れ", 16, GOLD, true), matchWrap());
        flow.addView(text("① Androidで会員QRを表示\n② iPadでQRを読み取る\n③ iPadでポイント付与\n④ Supabaseへ保存\n⑤ Androidで「最新情報を同期」", 14, TEXT, false), topMargin(dp(9)));
        content.addView(flow, topMargin(dp(20)));

        TextView note = text("v0.6 同期デモ　※会員情報はデモデータです", 11, MUTED, false);
        note.setGravity(Gravity.CENTER);
        content.addView(note, topMargin(dp(18)));
    }

    private void showMember() {
        currentPage = PAGE_MEMBER;
        content.removeAllViews();
        selectNav(memberNav);
        content.addView(sectionTitle("会員証・ポイント"), matchWrap());

        int available = prefs.getInt("available", 0);
        int cumulative = prefs.getInt("cumulative", 0);
        RankInfo rank = rankInfo(cumulative);

        LinearLayout memberCard = card(CARD_ALT, 20);
        memberCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        memberCard.addView(text("濱匠別邸 MEMBER", 14, MUTED, true), matchWrap());
        memberCard.addView(text(rank.name + " 会員", 30, GOLD, true), topMargin(dp(8)));
        memberCard.addView(text("会員ID  " + MEMBER_ID, 13, MUTED, false), topMargin(dp(7)));
        addDivider(memberCard, dp(15));
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(stat(available + " pt", "利用可能"), weighted());
        stats.addView(stat(cumulative + " pt", "累計"), weighted());
        memberCard.addView(stats, topMargin(dp(14)));
        content.addView(memberCard, topMargin(dp(12)));

        LinearLayout qrCard = card(Color.WHITE, 18);
        qrCard.setGravity(Gravity.CENTER);
        qrCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        ImageView qr = new ImageView(this);
        Bitmap bitmap = makeQr("HAMASHO_MEMBER:" + MEMBER_ID, 760);
        if (bitmap != null) qr.setImageBitmap(bitmap);
        qrCard.addView(qr, new LinearLayout.LayoutParams(dp(230), dp(230)));
        TextView label = text("会計時にこのQRをスタッフへご提示ください", 13, Color.rgb(45, 40, 34), true);
        label.setGravity(Gravity.CENTER);
        qrCard.addView(label, topMargin(dp(10)));
        content.addView(qrCard, topMargin(dp(14)));

        addSyncButton();

        LinearLayout progressCard = card(CARD, 16);
        progressCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        progressCard.addView(text("ランクアップ", 15, GOLD, true), matchWrap());
        progressCard.addView(text(rank.nextText, 21, TEXT, true), topMargin(dp(7)));
        progressCard.addView(text("粋 0　雅 3,000　匠 10,000　別邸 30,000", 12, MUTED, false), topMargin(dp(10)));
        content.addView(progressCard, topMargin(dp(14)));
    }

    private void showCoupon() {
        currentPage = PAGE_COUPON;
        content.removeAllViews();
        selectNav(couponNav);
        content.addView(sectionTitle("月1回 会員クーポン"), matchWrap());

        RankInfo rank = rankInfo(prefs.getInt("cumulative", 0));
        boolean used = prefs.getBoolean("coupon_used", false);
        LinearLayout coupon = card(used ? Color.rgb(42, 39, 35) : Color.rgb(61, 43, 28), 20);
        coupon.setPadding(dp(20), dp(20), dp(20), dp(20));
        coupon.addView(text(currentMonth() + "月 会員様限定", 14, GOLD, true), matchWrap());
        coupon.addView(text(rank.name + " 会員", 28, TEXT, true), topMargin(dp(8)));
        coupon.addView(text(couponText(rank.name), 21, used ? MUTED : GOLD, true), topMargin(dp(10)));
        addDivider(coupon, dp(16));
        coupon.addView(text(used ? "使用済み" : "未使用", 19, used ? MUTED : GOLD, true), topMargin(dp(13)));
        coupon.addView(text("有効期限：" + currentMonth() + "月末まで\n利用回数：月1回\n※使用確定は店舗スタッフが行います", 14, TEXT, false), topMargin(dp(10)));
        content.addView(coupon, topMargin(dp(12)));
        addSyncButton();

        LinearLayout ranks = card(CARD, 16);
        ranks.setPadding(dp(18), dp(16), dp(18), dp(16));
        ranks.addView(text("ランク連動", 16, GOLD, true), matchWrap());
        ranks.addView(text("粋　　通常会員クーポン\n雅　　ビール・焼酎・ハイボールから1杯\n匠　　日本酒を含む対象ドリンク1杯\n別邸　対象ドリンク＋季節の一品", 14, TEXT, false), topMargin(dp(12)));
        content.addView(ranks, topMargin(dp(14)));
    }

    private void showStore() {
        currentPage = PAGE_STORE;
        content.removeAllViews();
        selectNav(storeNav);
        content.addView(sectionTitle("店舗・予約"), matchWrap());

        LinearLayout store = card(CARD_ALT, 18);
        store.setPadding(dp(18), dp(18), dp(18), dp(18));
        store.addView(text("濱匠別邸", 26, GOLD, true), matchWrap());
        store.addView(text("店舗情報・電話・ネット予約は、正式導入時に実際の情報を設定します。", 14, TEXT, false), topMargin(dp(10)));
        content.addView(store, topMargin(dp(12)));

        LinearLayout guide = card(CARD, 16);
        guide.setPadding(dp(18), dp(16), dp(18), dp(16));
        guide.addView(text("ポイントご利用について", 16, GOLD, true), matchWrap());
        guide.addView(text("100円（税込）＝1ポイント\n1ポイント＝1円\n300ポイントからご利用いただけます。", 14, TEXT, false), topMargin(dp(10)));
        content.addView(guide, topMargin(dp(14)));

        LinearLayout demo = card(Color.rgb(59, 42, 28), 16);
        demo.setPadding(dp(18), dp(16), dp(18), dp(16));
        demo.addView(text("現在は社長確認用デモ", 16, GOLD, true), matchWrap());
        demo.addView(text("Supabaseにはデモ会員 HMB-000001 のみ登録しています。本番前に認証・スタッフ権限・履歴管理を追加します。", 13, TEXT, false), topMargin(dp(8)));
        content.addView(demo, topMargin(dp(14)));
    }

    private void addSyncCard() {
        LinearLayout box = card(Color.rgb(34, 47, 36), 14);
        box.setPadding(dp(15), dp(12), dp(15), dp(12));
        box.addView(text(syncing ? "Supabase 同期中…" : syncText, 13, syncing ? GOLD : TEXT, true), matchWrap());
        Button b = actionButton(syncing ? "同期中…" : "最新情報を同期");
        b.setEnabled(!syncing);
        b.setOnClickListener(v -> syncFromSupabase());
        box.addView(b, topMargin(dp(8)));
        content.addView(box, matchWrap());
    }

    private void addSyncButton() {
        Button b = actionButton(syncing ? "Supabase 同期中…" : "最新情報を同期");
        b.setEnabled(!syncing);
        b.setOnClickListener(v -> syncFromSupabase());
        content.addView(b, topMargin(dp(14)));
        TextView status = text(syncText, 11, MUTED, false);
        status.setGravity(Gravity.CENTER);
        content.addView(status, topMargin(dp(5)));
    }

    private void syncFromSupabase() {
        if (syncing) return;
        syncing = true;
        renderCurrentPage();
        io.execute(() -> {
            try {
                String id = URLEncoder.encode(MEMBER_ID, StandardCharsets.UTF_8.name());
                String endpoint = SUPABASE_URL + "/rest/v1/demo_members?member_id=eq." + id
                        + "&select=available_points,cumulative_points,coupon_used,updated_at";
                HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                InputStream input = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                String body = readAll(input);
                conn.disconnect();
                if (code < 200 || code >= 300) throw new Exception("HTTP " + code + " " + body);

                JSONArray rows = new JSONArray(body);
                if (rows.length() == 0) throw new Exception("デモ会員が見つかりません");
                JSONObject row = rows.getJSONObject(0);
                int available = row.getInt("available_points");
                int cumulative = row.getInt("cumulative_points");
                boolean couponUsed = row.getBoolean("coupon_used");
                prefs.edit()
                        .putInt("available", available)
                        .putInt("cumulative", cumulative)
                        .putBoolean("coupon_used", couponUsed)
                        .apply();
                runOnUiThread(() -> {
                    syncing = false;
                    syncText = "Supabase同期済み  利用可能 " + available + "pt";
                    renderCurrentPage();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    syncing = false;
                    syncText = "同期エラー：" + safeError(e.getMessage());
                    renderCurrentPage();
                });
            }
        });
    }

    private String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) out.append(line);
        reader.close();
        return out.toString();
    }

    private String safeError(String value) {
        if (value == null || value.trim().isEmpty()) return "通信できません";
        return value.length() > 70 ? value.substring(0, 70) : value;
    }

    private void renderCurrentPage() {
        if (content == null) return;
        switch (currentPage) {
            case PAGE_MEMBER: showMember(); break;
            case PAGE_COUPON: showCoupon(); break;
            case PAGE_STORE: showStore(); break;
            default: showHome(); break;
        }
    }

    private RankInfo rankInfo(int cumulative) {
        if (cumulative >= 30000) return new RankInfo("別邸", "最高ランクです", 30000, 30000);
        if (cumulative >= 10000) return new RankInfo("匠", "あと " + (30000 - cumulative) + " pt で「別邸」", cumulative - 10000, 20000);
        if (cumulative >= 3000) return new RankInfo("雅", "あと " + (10000 - cumulative) + " pt で「匠」", cumulative - 3000, 7000);
        return new RankInfo("粋", "あと " + (3000 - cumulative) + " pt で「雅」", cumulative, 3000);
    }

    private String couponText(String rank) {
        switch (rank) {
            case "雅": return "ビール・焼酎・ハイボールから\nお好きな1杯サービス";
            case "匠": return "日本酒を含む対象ドリンク\nお好きな1杯サービス";
            case "別邸": return "対象ドリンク1杯 ＋\n季節の一品サービス";
            default: return "今月の会員限定サービス";
        }
    }

    private Bitmap makeQr(String value, int size) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size);
            int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) {
                int offset = y * size;
                for (int x = 0; x < size; x++) pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private LinearLayout tileRow(String aTitle, String aSub, String bTitle, String bSub) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(0, dp(116), 1f);
        aLp.rightMargin = dp(5);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, dp(116), 1f);
        bLp.leftMargin = dp(5);
        row.addView(tile(aTitle, aSub), aLp);
        row.addView(tile(bTitle, bSub), bLp);
        return row;
    }

    private LinearLayout tile(String title, String sub) {
        LinearLayout box = card(CARD, 16);
        box.setGravity(Gravity.BOTTOM);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.addView(text(title, 17, TEXT, true), matchWrap());
        box.addView(text(sub, 11, MUTED, false), topMargin(dp(4)));
        return box;
    }

    private LinearLayout stat(String value, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.addView(text(value, 21, GOLD, true), matchWrap());
        box.addView(text(label, 11, MUTED, false), topMargin(dp(4)));
        return box;
    }

    private Button navButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTextColor(MUTED);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(0, dp(7), 0, dp(7));
        return b;
    }

    private Button actionButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(34, 28, 21));
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(roundRect(GOLD, 12));
        b.setPadding(dp(12), dp(10), dp(12), dp(10));
        return b;
    }

    private void selectNav(Button selected) {
        Button[] all = {homeNav, memberNav, couponNav, storeNav};
        for (Button b : all) {
            b.setTextColor(b == selected ? GOLD : MUTED);
            b.setTypeface(Typeface.DEFAULT, b == selected ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private TextView sectionTitle(String value) { return text(value, 19, GOLD, true); }

    private TextView badge(String value) {
        TextView v = text(value, 13, Color.rgb(34, 28, 21), true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(13), dp(6), dp(13), dp(6));
        v.setBackground(roundRect(GOLD, 20));
        return v;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setLineSpacing(0, 1.12f);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private LinearLayout card(int color, int radius) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(roundRect(color, radius));
        return box;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radiusDp));
        return bg;
    }

    private void addDivider(LinearLayout parent, int top) {
        View line = new View(this);
        line.setBackgroundColor(DIVIDER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.topMargin = top;
        parent.addView(line, lp);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int margin) {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = margin;
        return lp;
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int currentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    private static class RankInfo {
        final String name;
        final String nextText;
        final int progressValue;
        final int progressMax;
        RankInfo(String name, String nextText, int progressValue, int progressMax) {
            this.name = name;
            this.nextText = nextText;
            this.progressValue = Math.max(0, progressValue);
            this.progressMax = Math.max(1, progressMax);
        }
    }
}
