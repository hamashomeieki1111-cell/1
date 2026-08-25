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

    private static final int BG = Color.rgb(16, 13, 10);
    private static final int BLACK = Color.rgb(11, 9, 7);
    private static final int CARD = Color.rgb(31, 25, 20);
    private static final int CARD_ALT = Color.rgb(43, 34, 26);
    private static final int WARM = Color.rgb(73, 49, 31);
    private static final int GOLD = Color.rgb(203, 168, 92);
    private static final int GOLD_LIGHT = Color.rgb(231, 207, 150);
    private static final int TEXT = Color.rgb(248, 243, 233);
    private static final int MUTED = Color.rgb(184, 169, 146);
    private static final int DIVIDER = Color.rgb(81, 66, 49);
    private static final int GREEN = Color.rgb(47, 67, 50);

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
    private String syncText = "会員情報を確認中…";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BLACK);
        getWindow().setNavigationBarColor(BLACK);
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
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(20), dp(14), dp(20), dp(13));
        header.setBackground(gradient(new int[]{Color.rgb(10, 8, 6), Color.rgb(24, 17, 12)}, 0, 0));

        TextView english = text("HAMASHO  BETTEI", 9, MUTED, true);
        english.setGravity(Gravity.CENTER);
        english.setLetterSpacing(0.22f);
        header.addView(english, matchWrap());

        TextView brand = text("濱匠別邸", 31, GOLD_LIGHT, true);
        brand.setGravity(Gravity.CENTER);
        brand.setTypeface(Typeface.SERIF, Typeface.BOLD);
        brand.setLetterSpacing(0.08f);
        header.addView(brand, topMargin(dp(3)));

        View goldLine = new View(this);
        goldLine.setBackgroundColor(GOLD);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(dp(54), dp(1));
        lineLp.gravity = Gravity.CENTER_HORIZONTAL;
        lineLp.topMargin = dp(5);
        header.addView(goldLine, lineLp);

        TextView tagline = text("蕎麦と酒を、粋に愉しむ。", 11, MUTED, false);
        tagline.setGravity(Gravity.CENTER);
        tagline.setLetterSpacing(0.04f);
        header.addView(tagline, topMargin(dp(6)));
        root.addView(header, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(15), dp(14), dp(15), dp(30));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(5), dp(4), dp(5), dp(5));
        nav.setBackgroundColor(BLACK);
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

        int available = prefs.getInt("available", 0);
        int cumulative = prefs.getInt("cumulative", 0);
        RankInfo rank = rankInfo(cumulative);

        addHero();

        LinearLayout memberCard = gradientCard(
                new int[]{Color.rgb(54, 41, 29), Color.rgb(24, 20, 16)}, 20, GOLD);
        memberCard.setPadding(dp(19), dp(17), dp(19), dp(17));
        memberCard.setElevation(dp(3));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView memberLabel = text("MEMBERS CLUB", 10, MUTED, true);
        memberLabel.setLetterSpacing(0.16f);
        top.addView(memberLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(badge(rank.name));
        memberCard.addView(top, matchWrap());

        memberCard.addView(text("現在のポイント", 11, MUTED, false), topMargin(dp(14)));
        memberCard.addView(text(formatNumber(available) + " pt", 34, GOLD_LIGHT, true), topMargin(dp(1)));

        addDivider(memberCard, dp(12));
        LinearLayout smallStats = new LinearLayout(this);
        smallStats.setOrientation(LinearLayout.HORIZONTAL);
        smallStats.setGravity(Gravity.CENTER_VERTICAL);
        smallStats.addView(text("累計  " + formatNumber(cumulative) + " pt", 13, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        smallStats.addView(text(rank.nextText, 11, MUTED, false));
        memberCard.addView(smallStats, topMargin(dp(11)));

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(rank.progressMax);
        progress.setProgress(rank.progressValue);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(DIVIDER));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5));
        p.topMargin = dp(9);
        memberCard.addView(progress, p);
        memberCard.setOnClickListener(v -> showMember());
        content.addView(memberCard, topMargin(dp(14)));

        LinearLayout syncRow = new LinearLayout(this);
        syncRow.setOrientation(LinearLayout.HORIZONTAL);
        syncRow.setGravity(Gravity.CENTER_VERTICAL);
        syncRow.setPadding(dp(3), dp(8), dp(3), 0);
        TextView syncStatus = text(syncing ? "会員情報を更新しています…" : syncText, 10, MUTED, false);
        syncRow.addView(syncStatus, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button refresh = miniButton(syncing ? "更新中" : "更新");
        refresh.setEnabled(!syncing);
        refresh.setOnClickListener(v -> syncFromSupabase());
        syncRow.addView(refresh, new LinearLayout.LayoutParams(dp(72), dp(36)));
        content.addView(syncRow, matchWrap());

        content.addView(sectionTitle("今月のおもてなし", "MEMBER BENEFIT"), topMargin(dp(18)));
        boolean used = prefs.getBoolean("coupon_used", false);
        LinearLayout coupon = gradientCard(
                used ? new int[]{Color.rgb(43, 39, 34), Color.rgb(30, 27, 24)} : new int[]{Color.rgb(91, 57, 32), Color.rgb(49, 35, 24)},
                18, used ? DIVIDER : GOLD);
        coupon.setPadding(dp(19), dp(17), dp(19), dp(17));
        coupon.addView(text(currentMonth() + "月限定  ｜  " + rank.name + " 会員", 11, GOLD_LIGHT, true), matchWrap());
        coupon.addView(text(couponText(rank.name), 21, used ? MUTED : TEXT, true), topMargin(dp(7)));
        coupon.addView(text(used ? "✓  ご利用済み" : "●  今月1回ご利用いただけます", 11, used ? MUTED : GOLD_LIGHT, true), topMargin(dp(12)));
        coupon.setOnClickListener(v -> showCoupon());
        content.addView(coupon, topMargin(dp(9)));

        content.addView(sectionTitle("別邸を愉しむ", "DISCOVER"), topMargin(dp(22)));
        content.addView(featureRow("旬", "季節のおすすめ", "旬の食材・限定料理", "宴", "宴会・接待", "大切なお席に"), topMargin(dp(9)));
        content.addView(featureRow("酒", "日本酒", "季節酒・おすすめ銘柄", "杯", "おすすめドリンク", "ビール・焼酎・ハイボール"), topMargin(dp(10)));

        LinearLayout story = gradientCard(new int[]{Color.rgb(38, 28, 20), Color.rgb(24, 20, 17)}, 17, DIVIDER);
        story.setPadding(dp(18), dp(17), dp(18), dp(17));
        TextView kicker = text("HAMASHO BETTEI", 9, GOLD, true);
        kicker.setLetterSpacing(0.16f);
        story.addView(kicker, matchWrap());
        story.addView(text("季節と酒、蕎麦を愉しむ時間。", 19, TEXT, true), topMargin(dp(7)));
        story.addView(text("一皿ごとの旬と、選りすぐりの酒。\n濱匠別邸ならではのひとときを。", 13, MUTED, false), topMargin(dp(8)));
        content.addView(story, topMargin(dp(20)));

        TextView note = text("社長確認用 DEMO  ｜  v0.7 DESIGN", 9, Color.rgb(112, 100, 84), false);
        note.setGravity(Gravity.CENTER);
        note.setLetterSpacing(0.08f);
        content.addView(note, topMargin(dp(18)));
    }

    private void addHero() {
        LinearLayout hero = gradientCard(
                new int[]{Color.rgb(84, 53, 30), Color.rgb(45, 30, 21), Color.rgb(20, 17, 14)}, 22, Color.rgb(112, 86, 52));
        hero.setPadding(dp(20), dp(22), dp(20), dp(20));
        hero.setMinimumHeight(dp(154));
        hero.setGravity(Gravity.BOTTOM);

        TextView season = text("SEASONAL  TABLE", 9, GOLD_LIGHT, true);
        season.setLetterSpacing(0.18f);
        hero.addView(season, matchWrap());
        hero.addView(text("季節を味わう、\n別邸の夜。", 28, TEXT, true), topMargin(dp(7)));
        hero.addView(text("蕎麦  ｜  酒  ｜  旬菜", 12, GOLD_LIGHT, false), topMargin(dp(10)));
        content.addView(hero, matchWrap());
    }

    private void showMember() {
        currentPage = PAGE_MEMBER;
        content.removeAllViews();
        selectNav(memberNav);

        int available = prefs.getInt("available", 0);
        int cumulative = prefs.getInt("cumulative", 0);
        RankInfo rank = rankInfo(cumulative);

        content.addView(pageHeading("会員証", "MEMBERS CARD"), matchWrap());

        LinearLayout memberCard = gradientCard(
                new int[]{Color.rgb(74, 55, 37), Color.rgb(29, 24, 19)}, 22, GOLD);
        memberCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        memberCard.setElevation(dp(3));

        LinearLayout first = new LinearLayout(this);
        first.setOrientation(LinearLayout.HORIZONTAL);
        first.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text("HAMASHO BETTEI", 10, MUTED, true);
        label.setLetterSpacing(0.16f);
        first.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        first.addView(badge(rank.name));
        memberCard.addView(first, matchWrap());

        memberCard.addView(text(rank.name + " 会員", 31, GOLD_LIGHT, true), topMargin(dp(15)));
        memberCard.addView(text("MEMBER ID  " + MEMBER_ID, 10, MUTED, false), topMargin(dp(4)));
        addDivider(memberCard, dp(16));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(stat(formatNumber(available) + " pt", "利用可能ポイント"), weighted());
        View divider = new View(this);
        divider.setBackgroundColor(DIVIDER);
        stats.addView(divider, new LinearLayout.LayoutParams(dp(1), dp(46)));
        stats.addView(stat(formatNumber(cumulative) + " pt", "累計ポイント"), weighted());
        memberCard.addView(stats, topMargin(dp(15)));
        content.addView(memberCard, topMargin(dp(13)));

        LinearLayout qrOuter = gradientCard(new int[]{Color.rgb(46, 37, 29), Color.rgb(28, 24, 20)}, 20, DIVIDER);
        qrOuter.setPadding(dp(14), dp(14), dp(14), dp(16));
        qrOuter.setGravity(Gravity.CENTER);

        LinearLayout qrCard = card(Color.WHITE, 16);
        qrCard.setGravity(Gravity.CENTER);
        qrCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        ImageView qr = new ImageView(this);
        Bitmap bitmap = makeQr("HAMASHO_MEMBER:" + MEMBER_ID, 760);
        if (bitmap != null) qr.setImageBitmap(bitmap);
        qrCard.addView(qr, new LinearLayout.LayoutParams(dp(224), dp(224)));
        qrOuter.addView(qrCard, new LinearLayout.LayoutParams(dp(260), dp(260)));

        TextView qrLabel = text("会計時にこちらのQRをご提示ください", 12, TEXT, true);
        qrLabel.setGravity(Gravity.CENTER);
        qrOuter.addView(qrLabel, topMargin(dp(12)));
        content.addView(qrOuter, topMargin(dp(14)));

        addRefreshButton();

        LinearLayout progressCard = gradientCard(new int[]{CARD_ALT, CARD}, 17, DIVIDER);
        progressCard.setPadding(dp(18), dp(17), dp(18), dp(17));
        progressCard.addView(text("次のランクまで", 11, GOLD, true), matchWrap());
        progressCard.addView(text(rank.nextText, 20, TEXT, true), topMargin(dp(5)));
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(rank.progressMax);
        progress.setProgress(rank.progressValue);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(DIVIDER));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5));
        lp.topMargin = dp(12);
        progressCard.addView(progress, lp);
        progressCard.addView(text("粋  0　｜　雅  3,000　｜　匠  10,000　｜　別邸  30,000", 10, MUTED, false), topMargin(dp(10)));
        content.addView(progressCard, topMargin(dp(14)));
    }

    private void showCoupon() {
        currentPage = PAGE_COUPON;
        content.removeAllViews();
        selectNav(couponNav);

        RankInfo rank = rankInfo(prefs.getInt("cumulative", 0));
        boolean used = prefs.getBoolean("coupon_used", false);
        content.addView(pageHeading("会員クーポン", "MONTHLY BENEFIT"), matchWrap());

        LinearLayout coupon = gradientCard(
                used ? new int[]{Color.rgb(45, 42, 38), Color.rgb(29, 27, 24)} : new int[]{Color.rgb(103, 63, 32), Color.rgb(49, 34, 23)},
                22, used ? DIVIDER : GOLD);
        coupon.setPadding(dp(21), dp(21), dp(21), dp(21));
        coupon.addView(text(currentMonth() + "月  MEMBER'S SPECIAL", 11, GOLD_LIGHT, true), matchWrap());
        coupon.addView(text(rank.name + " 会員様限定", 27, TEXT, true), topMargin(dp(8)));
        coupon.addView(text(couponText(rank.name), 22, used ? MUTED : GOLD_LIGHT, true), topMargin(dp(12)));
        addDivider(coupon, dp(18));
        coupon.addView(text(used ? "✓  使用済み" : "●  ご利用いただけます", 17, used ? MUTED : TEXT, true), topMargin(dp(14)));
        coupon.addView(text("有効期限：" + currentMonth() + "月末まで\n利用回数：月1回\n※使用確定は店舗スタッフが行います", 13, MUTED, false), topMargin(dp(10)));
        content.addView(coupon, topMargin(dp(14)));

        addRefreshButton();

        LinearLayout ranks = gradientCard(new int[]{CARD_ALT, CARD}, 17, DIVIDER);
        ranks.setPadding(dp(18), dp(17), dp(18), dp(17));
        ranks.addView(text("RANK BENEFITS", 9, GOLD, true), matchWrap());
        ranks.addView(text("ランクごとのおもてなし", 18, TEXT, true), topMargin(dp(5)));
        ranks.addView(text("粋　　通常会員クーポン\n雅　　ビール・焼酎・ハイボールから1杯\n匠　　日本酒を含む対象ドリンク1杯\n別邸　対象ドリンク1杯＋季節の一品", 13, MUTED, false), topMargin(dp(12)));
        content.addView(ranks, topMargin(dp(14)));
    }

    private void showStore() {
        currentPage = PAGE_STORE;
        content.removeAllViews();
        selectNav(storeNav);
        content.addView(pageHeading("店舗・予約", "HAMASHO BETTEI"), matchWrap());

        LinearLayout store = gradientCard(new int[]{Color.rgb(70, 47, 30), Color.rgb(29, 23, 18)}, 21, GOLD);
        store.setPadding(dp(20), dp(22), dp(20), dp(22));
        TextView english = text("SOBA  &  SAKE", 9, GOLD_LIGHT, true);
        english.setLetterSpacing(0.18f);
        store.addView(english, matchWrap());
        TextView name = text("濱匠別邸", 29, TEXT, true);
        name.setTypeface(Typeface.SERIF, Typeface.BOLD);
        store.addView(name, topMargin(dp(7)));
        store.addView(text("店舗情報・電話・ネット予約は、正式導入時に実際の情報へ設定します。", 13, MUTED, false), topMargin(dp(11)));
        content.addView(store, topMargin(dp(14)));

        LinearLayout guide = gradientCard(new int[]{CARD_ALT, CARD}, 17, DIVIDER);
        guide.setPadding(dp(18), dp(17), dp(18), dp(17));
        guide.addView(text("ポイントのご利用", 17, GOLD_LIGHT, true), matchWrap());
        guide.addView(text("100円（税込）＝ 1ポイント\n1ポイント ＝ 1円\n300ポイントからご利用いただけます。", 14, TEXT, false), topMargin(dp(10)));
        content.addView(guide, topMargin(dp(14)));

        LinearLayout demo = card(Color.rgb(26, 23, 20), 15);
        demo.setPadding(dp(16), dp(14), dp(16), dp(14));
        demo.addView(text("DEMO VERSION", 9, Color.rgb(128, 113, 92), true), matchWrap());
        demo.addView(text("現在は社長確認用の試作版です。", 12, MUTED, false), topMargin(dp(4)));
        content.addView(demo, topMargin(dp(14)));
    }

    private LinearLayout pageHeading(String jp, String en) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView english = text(en, 9, GOLD, true);
        english.setLetterSpacing(0.17f);
        box.addView(english, matchWrap());
        box.addView(text(jp, 25, TEXT, true), topMargin(dp(3)));
        return box;
    }

    private LinearLayout sectionTitle(String jp, String en) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView english = text(en, 8, Color.rgb(149, 125, 80), true);
        english.setLetterSpacing(0.17f);
        box.addView(english, matchWrap());
        box.addView(text(jp, 19, GOLD_LIGHT, true), topMargin(dp(2)));
        return box;
    }

    private LinearLayout featureRow(String aMark, String aTitle, String aSub, String bMark, String bTitle, String bSub) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(0, dp(136), 1f);
        aLp.rightMargin = dp(5);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, dp(136), 1f);
        bLp.leftMargin = dp(5);
        row.addView(featureTile(aMark, aTitle, aSub), aLp);
        row.addView(featureTile(bMark, bTitle, bSub), bLp);
        return row;
    }

    private LinearLayout featureTile(String mark, String title, String sub) {
        LinearLayout box = gradientCard(new int[]{Color.rgb(56, 40, 28), Color.rgb(28, 23, 19)}, 18, Color.rgb(77, 62, 45));
        box.setPadding(dp(15), dp(13), dp(15), dp(14));
        TextView symbol = text(mark, 29, GOLD, true);
        symbol.setTypeface(Typeface.SERIF, Typeface.BOLD);
        box.addView(symbol, matchWrap());
        box.addView(text(title, 16, TEXT, true), topMargin(dp(8)));
        box.addView(text(sub, 10, MUTED, false), topMargin(dp(3)));
        return box;
    }

    private void addRefreshButton() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView status = text(syncing ? "更新しています…" : syncText, 10, MUTED, false);
        row.addView(status, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button b = miniButton(syncing ? "更新中" : "最新に更新");
        b.setEnabled(!syncing);
        b.setOnClickListener(v -> syncFromSupabase());
        row.addView(b, new LinearLayout.LayoutParams(dp(104), dp(38)));
        content.addView(row, topMargin(dp(12)));
    }

    private Button miniButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setTextColor(GOLD_LIGHT);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(outlineRect(Color.rgb(28, 23, 19), GOLD, 12));
        b.setPadding(dp(8), 0, dp(8), 0);
        b.setStateListAnimator(null);
        return b;
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
                    syncText = "会員情報を更新しました";
                    renderCurrentPage();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    syncing = false;
                    syncText = "更新できません：" + safeError(e.getMessage());
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
        return value.length() > 48 ? value.substring(0, 48) : value;
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
        if (cumulative >= 30000) return new RankInfo("別邸", "最高ランク", 30000, 30000);
        if (cumulative >= 10000) return new RankInfo("匠", "あと " + formatNumber(30000 - cumulative) + " pt", cumulative - 10000, 20000);
        if (cumulative >= 3000) return new RankInfo("雅", "あと " + formatNumber(10000 - cumulative) + " pt", cumulative - 3000, 7000);
        return new RankInfo("粋", "あと " + formatNumber(3000 - cumulative) + " pt", cumulative, 3000);
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

    private LinearLayout stat(String value, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.addView(text(value, 20, GOLD_LIGHT, true), matchWrap());
        TextView l = text(label, 10, MUTED, false);
        l.setGravity(Gravity.CENTER);
        box.addView(l, topMargin(dp(4)));
        return box;
    }

    private Button navButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setTextColor(MUTED);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(0, dp(8), 0, dp(8));
        b.setStateListAnimator(null);
        return b;
    }

    private void selectNav(Button selected) {
        Button[] all = {homeNav, memberNav, couponNav, storeNav};
        for (Button b : all) {
            b.setTextColor(b == selected ? GOLD_LIGHT : MUTED);
            b.setTypeface(Typeface.DEFAULT, b == selected ? Typeface.BOLD : Typeface.NORMAL);
            b.setBackground(b == selected ? outlineRect(Color.rgb(28, 22, 17), Color.rgb(70, 55, 38), 11) : null);
        }
    }

    private TextView badge(String value) {
        TextView v = text(value, 14, Color.rgb(34, 26, 17), true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(13), dp(5), dp(13), dp(5));
        v.setBackground(gradient(new int[]{GOLD_LIGHT, GOLD}, 18, 0));
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

    private LinearLayout gradientCard(int[] colors, int radius, int strokeColor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(gradient(colors, radius, strokeColor));
        return box;
    }

    private GradientDrawable gradient(int[] colors, int radiusDp, int strokeColor) {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        if (radiusDp > 0) bg.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) bg.setStroke(dp(1), strokeColor);
        return bg;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radiusDp));
        return bg;
    }

    private GradientDrawable outlineRect(int color, int strokeColor, int radiusDp) {
        GradientDrawable bg = roundRect(color, radiusDp);
        bg.setStroke(dp(1), strokeColor);
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

    private static String formatNumber(int value) {
        return String.format("%,d", value);
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
