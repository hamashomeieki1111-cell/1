package jp.hamasho.bettei;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int BG = Color.rgb(13, 11, 9);
    private static final int BLACK = Color.rgb(8, 7, 6);
    private static final int CARD = Color.rgb(27, 23, 19);
    private static final int CARD_ALT = Color.rgb(39, 31, 24);
    private static final int GOLD = Color.rgb(202, 165, 84);
    private static final int GOLD_LIGHT = Color.rgb(236, 211, 151);
    private static final int TEXT = Color.rgb(249, 244, 235);
    private static final int MUTED = Color.rgb(186, 171, 147);
    private static final int DIVIDER = Color.rgb(78, 63, 45);

    private static final String MEMBER_ID = "HMB-000001";
    private static final String SUPABASE_URL = "https://sedprfuiymcgbhatofwb.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_BIJQSq4IQRxgwwqWd3YmTQ_etujBnSj";

    private static final String IMG_HERO = "https://hamasho-soba.com/images/shop/hamasho/t.webp";
    private static final String IMG_SEASON = "https://hamasho-soba.com/images/shop/hamasho/nishikitop.webp";
    private static final String IMG_BANQUET = "https://hamasho-soba.com/images/shop/hamasho/za.webp";
    private static final String IMG_SAKE = "https://hamasho-soba.com/images/shop/hamasho/c.webp";
    private static final String IMG_DRINK = "https://hamasho-soba.com/images/shop/hamasho/t.webp";

    private static final int PAGE_HOME = 0;
    private static final int PAGE_MEMBER = 1;
    private static final int PAGE_COUPON = 2;
    private static final int PAGE_STORE = 3;

    private LinearLayout content;
    private Button homeNav, memberNav, couponNav, storeNav;
    private SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ExecutorService imageIo = Executors.newFixedThreadPool(3);
    private final Map<String, Bitmap> imageCache = new ConcurrentHashMap<>();
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
        imageIo.shutdownNow();
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
        header.setPadding(dp(16), dp(9), dp(16), dp(9));
        header.setBackgroundColor(BLACK);

        TextView brand = text("濱匠別邸", 22, GOLD_LIGHT, true);
        brand.setGravity(Gravity.CENTER);
        brand.setTypeface(Typeface.SERIF, Typeface.BOLD);
        brand.setLetterSpacing(0.08f);
        header.addView(brand, matchWrap());

        TextView tagline = text("蕎麦と酒を、粋に愉しむ。", 9, MUTED, false);
        tagline.setGravity(Gravity.CENTER);
        tagline.setLetterSpacing(0.05f);
        header.addView(tagline, topMargin(dp(2)));
        root.addView(header, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(10), dp(12), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(5), dp(4), dp(5), dp(5));
        nav.setBackgroundColor(BLACK);
        nav.setFitsSystemWindows(true);

        homeNav = navButton("⌂\nホーム");
        memberNav = navButton("▣\n会員証");
        couponNav = navButton("◇\nクーポン");
        storeNav = navButton("◎\n店舗情報");
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

        addHeroPhoto();

        LinearLayout memberCard = gradientCard(
                new int[]{Color.rgb(55, 42, 29), Color.rgb(21, 18, 15)}, 20, GOLD);
        memberCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        memberCard.setElevation(dp(3));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView memberLabel = text("濱匠別邸 MEMBER", 12, TEXT, true);
        memberLabel.setTypeface(Typeface.SERIF, Typeface.BOLD);
        top.addView(memberLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(badge(rank.name));
        memberCard.addView(top, matchWrap());

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text("現在ポイント", 10, MUTED, false), matchWrap());
        left.addView(text(formatNumber(available) + " pt", 31, GOLD_LIGHT, true), topMargin(dp(2)));
        stats.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        TextView cumLabel = text("累計ポイント", 10, MUTED, false);
        cumLabel.setGravity(Gravity.END);
        right.addView(cumLabel, matchWrap());
        TextView cumValue = text(formatNumber(cumulative) + " pt", 20, TEXT, true);
        cumValue.setGravity(Gravity.END);
        right.addView(cumValue, topMargin(dp(5)));
        stats.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        memberCard.addView(stats, topMargin(dp(15)));

        addDivider(memberCard, dp(12));
        LinearLayout progressText = new LinearLayout(this);
        progressText.setOrientation(LinearLayout.HORIZONTAL);
        progressText.addView(text(rank.nextText + "で次ランク", 11, GOLD_LIGHT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        progressText.addView(text(rank.nextTargetText, 10, MUTED, false));
        memberCard.addView(progressText, topMargin(dp(10)));

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(rank.progressMax);
        progress.setProgress(rank.progressValue);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(DIVIDER));
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        progressLp.topMargin = dp(8);
        memberCard.addView(progress, progressLp);
        memberCard.setOnClickListener(v -> showMember());
        content.addView(memberCard, topMargin(dp(12)));

        LinearLayout syncRow = new LinearLayout(this);
        syncRow.setOrientation(LinearLayout.HORIZONTAL);
        syncRow.setGravity(Gravity.CENTER_VERTICAL);
        syncRow.setPadding(dp(3), dp(7), dp(3), 0);
        TextView syncStatus = text(syncing ? "会員情報を更新しています…" : syncText, 9, Color.rgb(132, 119, 99), false);
        syncRow.addView(syncStatus, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button refresh = miniButton(syncing ? "更新中" : "更新");
        refresh.setEnabled(!syncing);
        refresh.setOnClickListener(v -> syncFromSupabase());
        syncRow.addView(refresh, new LinearLayout.LayoutParams(dp(68), dp(34)));
        content.addView(syncRow, matchWrap());

        content.addView(sectionTitle("今月のおもてなし", "MEMBER BENEFIT"), topMargin(dp(17)));
        boolean used = prefs.getBoolean("coupon_used", false);
        LinearLayout coupon = gradientCard(
                used ? new int[]{Color.rgb(42, 39, 35), Color.rgb(27, 25, 22)} : new int[]{Color.rgb(91, 56, 31), Color.rgb(45, 31, 22)},
                17, used ? DIVIDER : GOLD);
        coupon.setPadding(dp(17), dp(15), dp(17), dp(15));
        coupon.addView(text(currentMonth() + "月限定  ｜  " + rank.name + " 会員", 10, GOLD_LIGHT, true), matchWrap());
        coupon.addView(text(couponText(rank.name), 19, used ? MUTED : TEXT, true), topMargin(dp(6)));
        coupon.addView(text(used ? "ご利用済み" : "月1回ご利用いただけます", 10, used ? MUTED : GOLD_LIGHT, true), topMargin(dp(9)));
        coupon.setOnClickListener(v -> showCoupon());
        content.addView(coupon, topMargin(dp(8)));

        content.addView(sectionTitle("濱匠別邸を愉しむ", "DISCOVER"), topMargin(dp(20)));
        content.addView(photoTileRow(
                IMG_SEASON, "季節のおすすめ", "旬の食材・限定料理",
                IMG_BANQUET, "宴会・接待", "大切なお席に"), topMargin(dp(8)));
        content.addView(photoTileRow(
                IMG_SAKE, "日本酒", "厳選した地酒をご用意",
                IMG_DRINK, "おすすめドリンク", "ビール・焼酎・ハイボール"), topMargin(dp(9)));

        addQuickActions();
        addReservationCard();

        TextView note = text("社長確認用 DEMO  ｜  v0.8 PHOTO DESIGN", 8, Color.rgb(104, 93, 78), false);
        note.setGravity(Gravity.CENTER);
        note.setLetterSpacing(0.07f);
        content.addView(note, topMargin(dp(16)));
    }

    private void addHeroPhoto() {
        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(roundRect(Color.rgb(42, 31, 23), 20));
        hero.setClipToOutline(true);
        hero.setMinimumHeight(dp(220));

        ImageView image = remoteImage(IMG_HERO);
        hero.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));

        View shade = new View(this);
        shade.setBackground(gradient(new int[]{Color.argb(30, 0, 0, 0), Color.argb(190, 0, 0, 0)}, 0, 0));
        hero.addView(shade, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(18), dp(14), dp(18), dp(18));
        FrameLayout.LayoutParams copyLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);

        TextView kicker = text("本格串焼とへぎそば", 11, GOLD_LIGHT, true);
        kicker.setLetterSpacing(0.06f);
        copy.addView(kicker, matchWrap());
        TextView title = text("濱匠別邸", 34, Color.WHITE, true);
        title.setTypeface(Typeface.SERIF, Typeface.BOLD);
        title.setLetterSpacing(0.08f);
        copy.addView(title, topMargin(dp(2)));
        copy.addView(text("四季の恵みを繊細に。\n名駅で味わう上質なひととき。", 12, Color.rgb(240, 231, 216), false), topMargin(dp(6)));
        hero.addView(copy, copyLp);

        content.addView(hero, matchWrap());
    }

    private LinearLayout photoTileRow(String url1, String title1, String sub1, String url2, String title2, String sub2) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, dp(150), 1f);
        lp1.rightMargin = dp(4);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(150), 1f);
        lp2.leftMargin = dp(4);
        row.addView(photoTile(url1, title1, sub1), lp1);
        row.addView(photoTile(url2, title2, sub2), lp2);
        return row;
    }

    private FrameLayout photoTile(String url, String title, String sub) {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(roundRect(CARD_ALT, 16));
        frame.setClipToOutline(true);

        ImageView image = remoteImage(url);
        frame.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View shade = new View(this);
        shade.setBackground(gradient(new int[]{Color.argb(8, 0, 0, 0), Color.argb(210, 0, 0, 0)}, 0, 0));
        frame.addView(shade, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(12), dp(10), dp(12), dp(12));
        labels.addView(text(title, 17, Color.WHITE, true), matchWrap());
        labels.addView(text(sub, 9, Color.rgb(235, 226, 212), false), topMargin(dp(3)));
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        frame.addView(labels, labelLp);
        return frame;
    }

    private void addQuickActions() {
        LinearLayout box = gradientCard(new int[]{Color.rgb(24, 21, 18), Color.rgb(16, 14, 12)}, 16, DIVIDER);
        box.setPadding(dp(5), dp(10), dp(5), dp(10));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(quickAction("◇", "クーポン", v -> showCoupon()), weighted());
        row.addView(quickAction("▣", "会員証", v -> showMember()), weighted());
        row.addView(quickAction("☎", "予約", v -> showStore()), weighted());
        row.addView(quickAction("◎", "店舗情報", v -> showStore()), weighted());
        box.addView(row, matchWrap());
        content.addView(box, topMargin(dp(14)));
    }

    private LinearLayout quickAction(String mark, String label, View.OnClickListener listener) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(2), dp(5), dp(2), dp(5));
        TextView icon = text(mark, 22, GOLD, true);
        icon.setGravity(Gravity.CENTER);
        box.addView(icon, matchWrap());
        TextView name = text(label, 9, TEXT, true);
        name.setGravity(Gravity.CENTER);
        box.addView(name, topMargin(dp(3)));
        box.setOnClickListener(listener);
        return box;
    }

    private void addReservationCard() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(roundRect(Color.rgb(40, 30, 22), 17));
        frame.setClipToOutline(true);
        frame.setMinimumHeight(dp(116));

        ImageView image = remoteImage(IMG_BANQUET);
        frame.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(116)));
        View shade = new View(this);
        shade.setBackgroundColor(Color.argb(145, 0, 0, 0));
        frame.addView(shade, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(116)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER);
        labels.setPadding(dp(15), dp(12), dp(15), dp(12));
        TextView title = text("ご宴会・ご会食のご予約承り中", 18, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        labels.addView(title, matchWrap());
        TextView sub = text("季節のコース料理・個室のご相談はこちら", 10, GOLD_LIGHT, false);
        sub.setGravity(Gravity.CENTER);
        labels.addView(sub, topMargin(dp(5)));
        frame.addView(labels, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(116)));
        frame.setOnClickListener(v -> showStore());
        content.addView(frame, topMargin(dp(13)));
    }

    private void showMember() {
        currentPage = PAGE_MEMBER;
        content.removeAllViews();
        selectNav(memberNav);

        int available = prefs.getInt("available", 0);
        int cumulative = prefs.getInt("cumulative", 0);
        RankInfo rank = rankInfo(cumulative);

        content.addView(pageHeading("会員証", "MEMBERS CARD"), matchWrap());

        LinearLayout memberCard = gradientCard(new int[]{Color.rgb(66, 49, 33), Color.rgb(24, 20, 16)}, 21, GOLD);
        memberCard.setPadding(dp(19), dp(18), dp(19), dp(18));
        memberCard.setElevation(dp(3));

        LinearLayout first = new LinearLayout(this);
        first.setOrientation(LinearLayout.HORIZONTAL);
        first.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text("濱匠別邸 MEMBER", 12, TEXT, true);
        label.setTypeface(Typeface.SERIF, Typeface.BOLD);
        first.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        first.addView(badge(rank.name));
        memberCard.addView(first, matchWrap());
        memberCard.addView(text(rank.name + " 会員", 29, GOLD_LIGHT, true), topMargin(dp(14)));
        memberCard.addView(text("MEMBER ID  " + MEMBER_ID, 10, MUTED, false), topMargin(dp(4)));
        addDivider(memberCard, dp(14));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(stat(formatNumber(available) + " pt", "利用可能ポイント"), weighted());
        View divider = new View(this);
        divider.setBackgroundColor(DIVIDER);
        stats.addView(divider, new LinearLayout.LayoutParams(dp(1), dp(46)));
        stats.addView(stat(formatNumber(cumulative) + " pt", "累計ポイント"), weighted());
        memberCard.addView(stats, topMargin(dp(14)));
        content.addView(memberCard, topMargin(dp(12)));

        LinearLayout qrOuter = gradientCard(new int[]{Color.rgb(42, 34, 27), Color.rgb(25, 21, 18)}, 19, DIVIDER);
        qrOuter.setPadding(dp(13), dp(13), dp(13), dp(15));
        qrOuter.setGravity(Gravity.CENTER);
        LinearLayout qrCard = card(Color.WHITE, 15);
        qrCard.setGravity(Gravity.CENTER);
        qrCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        ImageView qr = new ImageView(this);
        Bitmap bitmap = makeQr("HAMASHO_MEMBER:" + MEMBER_ID, 760);
        if (bitmap != null) qr.setImageBitmap(bitmap);
        qrCard.addView(qr, new LinearLayout.LayoutParams(dp(224), dp(224)));
        qrOuter.addView(qrCard, new LinearLayout.LayoutParams(dp(254), dp(254)));
        TextView qrLabel = text("会計時にこちらのQRをご提示ください", 11, TEXT, true);
        qrLabel.setGravity(Gravity.CENTER);
        qrOuter.addView(qrLabel, topMargin(dp(11)));
        content.addView(qrOuter, topMargin(dp(13)));

        addRefreshButton();

        LinearLayout progressCard = gradientCard(new int[]{CARD_ALT, CARD}, 16, DIVIDER);
        progressCard.setPadding(dp(17), dp(16), dp(17), dp(16));
        progressCard.addView(text("次のランクまで", 10, GOLD, true), matchWrap());
        progressCard.addView(text(rank.nextText + "でランクアップ", 18, TEXT, true), topMargin(dp(4)));
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(rank.progressMax);
        progress.setProgress(rank.progressValue);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(DIVIDER));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5));
        lp.topMargin = dp(10);
        progressCard.addView(progress, lp);
        progressCard.addView(text("粋 0　｜　雅 3,000　｜　匠 10,000　｜　別邸 30,000", 9, MUTED, false), topMargin(dp(9)));
        content.addView(progressCard, topMargin(dp(13)));
    }

    private void showCoupon() {
        currentPage = PAGE_COUPON;
        content.removeAllViews();
        selectNav(couponNav);

        RankInfo rank = rankInfo(prefs.getInt("cumulative", 0));
        boolean used = prefs.getBoolean("coupon_used", false);
        content.addView(pageHeading("会員クーポン", "MONTHLY BENEFIT"), matchWrap());

        LinearLayout coupon = gradientCard(
                used ? new int[]{Color.rgb(43, 40, 36), Color.rgb(28, 26, 23)} : new int[]{Color.rgb(101, 62, 31), Color.rgb(46, 32, 22)},
                21, used ? DIVIDER : GOLD);
        coupon.setPadding(dp(20), dp(20), dp(20), dp(20));
        coupon.addView(text(currentMonth() + "月  MEMBER'S SPECIAL", 10, GOLD_LIGHT, true), matchWrap());
        coupon.addView(text(rank.name + " 会員様限定", 25, TEXT, true), topMargin(dp(7)));
        coupon.addView(text(couponText(rank.name), 21, used ? MUTED : GOLD_LIGHT, true), topMargin(dp(11)));
        addDivider(coupon, dp(16));
        coupon.addView(text(used ? "使用済み" : "ご利用いただけます", 16, used ? MUTED : TEXT, true), topMargin(dp(13)));
        coupon.addView(text("有効期限：" + currentMonth() + "月末まで\n利用回数：月1回\n※使用確定は店舗スタッフが行います", 12, MUTED, false), topMargin(dp(9)));
        content.addView(coupon, topMargin(dp(13)));

        addRefreshButton();

        LinearLayout ranks = gradientCard(new int[]{CARD_ALT, CARD}, 16, DIVIDER);
        ranks.setPadding(dp(17), dp(16), dp(17), dp(16));
        ranks.addView(text("RANK BENEFITS", 8, GOLD, true), matchWrap());
        ranks.addView(text("ランクごとのおもてなし", 17, TEXT, true), topMargin(dp(4)));
        ranks.addView(text("粋　　通常会員クーポン\n雅　　ビール・焼酎・ハイボールから1杯\n匠　　日本酒を含む対象ドリンク1杯\n別邸　対象ドリンク1杯＋季節の一品", 12, MUTED, false), topMargin(dp(10)));
        content.addView(ranks, topMargin(dp(13)));
    }

    private void showStore() {
        currentPage = PAGE_STORE;
        content.removeAllViews();
        selectNav(storeNav);
        content.addView(pageHeading("店舗・予約", "HAMASHO BETTEI"), matchWrap());

        FrameLayout photo = new FrameLayout(this);
        photo.setBackground(roundRect(CARD_ALT, 18));
        photo.setClipToOutline(true);
        ImageView storeImage = remoteImage(IMG_HERO);
        photo.addView(storeImage, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(165)));
        View shade = new View(this);
        shade.setBackgroundColor(Color.argb(105, 0, 0, 0));
        photo.addView(shade, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(165)));
        LinearLayout photoCopy = new LinearLayout(this);
        photoCopy.setOrientation(LinearLayout.VERTICAL);
        photoCopy.setPadding(dp(16), dp(12), dp(16), dp(15));
        photoCopy.addView(text("濱匠 名駅別邸", 25, Color.WHITE, true), matchWrap());
        photoCopy.addView(text("四季の恵みを繊細に。名駅で味わう上質なひととき。", 10, GOLD_LIGHT, false), topMargin(dp(4)));
        photo.addView(photoCopy, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        content.addView(photo, topMargin(dp(12)));

        LinearLayout info = gradientCard(new int[]{Color.rgb(49, 37, 27), Color.rgb(25, 21, 17)}, 17, DIVIDER);
        info.setPadding(dp(17), dp(16), dp(17), dp(16));
        info.addView(text("〒450-0002\n愛知県名古屋市中村区名駅2-41-3\nサンエスケービル1階", 13, TEXT, false), matchWrap());
        info.addView(text("JR名古屋駅 徒歩5分", 11, GOLD_LIGHT, true), topMargin(dp(8)));
        info.addView(text("月〜金 11:30〜14:00\n月〜土 17:00〜23:00\n定休日：日曜（不定休あり）", 12, MUTED, false), topMargin(dp(11)));
        content.addView(info, topMargin(dp(12)));

        Button call = primaryButton("電話で予約  052-583-8040");
        call.setOnClickListener(v -> dial("0525838040"));
        content.addView(call, topMargin(dp(12)));
        Button web = secondaryButton("公式ページを見る");
        web.setOnClickListener(v -> openUrl("https://hamasho-soba.com/hamasho-meieki.html"));
        content.addView(web, topMargin(dp(8)));

        LinearLayout guide = gradientCard(new int[]{CARD_ALT, CARD}, 16, DIVIDER);
        guide.setPadding(dp(17), dp(16), dp(17), dp(16));
        guide.addView(text("ポイントのご利用", 16, GOLD_LIGHT, true), matchWrap());
        guide.addView(text("100円（税込）＝ 1ポイント\n1ポイント ＝ 1円\n300ポイントからご利用いただけます。", 13, TEXT, false), topMargin(dp(9)));
        content.addView(guide, topMargin(dp(13)));
    }

    private ImageView remoteImage(String url) {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(Color.rgb(54, 43, 32));
        loadRemoteImage(image, url);
        return image;
    }

    private void loadRemoteImage(ImageView view, String url) {
        view.setTag(url);
        Bitmap cached = imageCache.get(url);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        imageIo.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 HamashoBetteiApp/0.8");
                conn.setDoInput(true);
                conn.connect();
                if (conn.getResponseCode() < 200 || conn.getResponseCode() >= 300) return;
                Bitmap bitmap = BitmapFactory.decodeStream(conn.getInputStream());
                if (bitmap == null) return;
                imageCache.put(url, bitmap);
                runOnUiThread(() -> {
                    Object tag = view.getTag();
                    if (tag != null && url.equals(tag.toString())) view.setImageBitmap(bitmap);
                });
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private LinearLayout pageHeading(String jp, String en) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView english = text(en, 8, GOLD, true);
        english.setLetterSpacing(0.17f);
        box.addView(english, matchWrap());
        box.addView(text(jp, 24, TEXT, true), topMargin(dp(2)));
        return box;
    }

    private LinearLayout sectionTitle(String jp, String en) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView english = text(en, 8, Color.rgb(148, 124, 78), true);
        english.setLetterSpacing(0.15f);
        box.addView(english, matchWrap());
        box.addView(text(jp, 18, GOLD_LIGHT, true), topMargin(dp(1)));
        return box;
    }

    private void addRefreshButton() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView status = text(syncing ? "更新しています…" : syncText, 9, MUTED, false);
        row.addView(status, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button b = miniButton(syncing ? "更新中" : "最新に更新");
        b.setEnabled(!syncing);
        b.setOnClickListener(v -> syncFromSupabase());
        row.addView(b, new LinearLayout.LayoutParams(dp(100), dp(36)));
        content.addView(row, topMargin(dp(11)));
    }

    private Button miniButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(10);
        b.setAllCaps(false);
        b.setTextColor(GOLD_LIGHT);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(outlineRect(Color.rgb(27, 22, 18), GOLD, 11));
        b.setPadding(dp(7), 0, dp(7), 0);
        b.setStateListAnimator(null);
        return b;
    }

    private Button primaryButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(35, 27, 18));
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(gradient(new int[]{GOLD_LIGHT, GOLD}, 13, 0));
        b.setStateListAnimator(null);
        return b;
    }

    private Button secondaryButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTextColor(GOLD_LIGHT);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(outlineRect(Color.rgb(25, 21, 17), GOLD, 13));
        b.setStateListAnimator(null);
        return b;
    }

    private void dial(String phone) {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
        } catch (Exception ignored) {
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
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
        if (cumulative >= 30000) return new RankInfo("別邸", "最高ランク", 30000, 30000, "30,000 pt");
        if (cumulative >= 10000) return new RankInfo("匠", "あと " + formatNumber(30000 - cumulative) + " pt", cumulative - 10000, 20000, "30,000 pt");
        if (cumulative >= 3000) return new RankInfo("雅", "あと " + formatNumber(10000 - cumulative) + " pt", cumulative - 3000, 7000, "10,000 pt");
        return new RankInfo("粋", "あと " + formatNumber(3000 - cumulative) + " pt", cumulative, 3000, "3,000 pt");
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
        TextView v = text(value, 19, GOLD_LIGHT, true);
        v.setGravity(Gravity.CENTER);
        box.addView(v, matchWrap());
        TextView l = text(label, 9, MUTED, false);
        l.setGravity(Gravity.CENTER);
        box.addView(l, topMargin(dp(3)));
        return box;
    }

    private Button navButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setGravity(Gravity.CENTER);
        b.setTextSize(9);
        b.setAllCaps(false);
        b.setTextColor(MUTED);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(0, dp(4), 0, dp(4));
        b.setStateListAnimator(null);
        return b;
    }

    private void selectNav(Button selected) {
        Button[] all = {homeNav, memberNav, couponNav, storeNav};
        for (Button b : all) {
            b.setTextColor(b == selected ? GOLD_LIGHT : MUTED);
            b.setTypeface(Typeface.DEFAULT, b == selected ? Typeface.BOLD : Typeface.NORMAL);
            b.setBackground(b == selected ? outlineRect(Color.rgb(25, 20, 16), Color.rgb(67, 52, 36), 10) : null);
        }
    }

    private TextView badge(String value) {
        TextView v = text(value, 13, Color.rgb(34, 26, 17), true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(12), dp(4), dp(12), dp(4));
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
        final String nextTargetText;

        RankInfo(String name, String nextText, int progressValue, int progressMax, String nextTargetText) {
            this.name = name;
            this.nextText = nextText;
            this.progressValue = Math.max(0, progressValue);
            this.progressMax = Math.max(1, progressMax);
            this.nextTargetText = nextTargetText;
        }
    }
}
