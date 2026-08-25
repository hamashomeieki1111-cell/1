package jp.hamasho.bettei;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.util.Calendar;

public class MainActivity extends Activity {

    private static final int BG = Color.rgb(23, 19, 15);
    private static final int CARD = Color.rgb(38, 32, 26);
    private static final int CARD_ALT = Color.rgb(48, 40, 31);
    private static final int GOLD = Color.rgb(214, 176, 96);
    private static final int TEXT = Color.rgb(246, 241, 231);
    private static final int MUTED = Color.rgb(188, 175, 154);
    private static final int DIVIDER = Color.rgb(79, 67, 53);
    private static final int DANGER = Color.rgb(133, 55, 48);

    private static final String MEMBER_ID = "HMB-000001";
    private static final String STAFF_PIN = "1188";

    private LinearLayout content;
    private Button homeNav, memberNav, couponNav, staffNav;
    private SharedPreferences prefs;
    private boolean staffUnlocked = false;

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

    private void ensureDefaults() {
        if (!prefs.contains("available")) {
            prefs.edit()
                    .putInt("available", 428)
                    .putInt("cumulative", 8750)
                    .putBoolean("coupon_used", false)
                    .putString("last_action", "試作データで開始")
                    .apply();
        }
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(16), dp(20), dp(12));
        header.setBackgroundColor(Color.rgb(18, 15, 12));

        TextView brand = text("濱匠別邸", 28, GOLD, true);
        brand.setGravity(Gravity.CENTER);
        header.addView(brand, matchWrap());

        TextView tagline = text("蕎麦と酒を、粋に愉しむ。", 12, MUTED, false);
        tagline.setGravity(Gravity.CENTER);
        header.addView(tagline, topMargin(dp(3)));
        root.addView(header, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(6), dp(7), dp(6), dp(8));
        nav.setBackgroundColor(Color.rgb(18, 15, 12));

        homeNav = navButton("ホーム");
        memberNav = navButton("会員証");
        couponNav = navButton("クーポン");
        staffNav = navButton("スタッフ");

        nav.addView(homeNav, weighted());
        nav.addView(memberNav, weighted());
        nav.addView(couponNav, weighted());
        nav.addView(staffNav, weighted());

        homeNav.setOnClickListener(v -> showHome());
        memberNav.setOnClickListener(v -> showMember());
        couponNav.setOnClickListener(v -> showCoupon());
        staffNav.setOnClickListener(v -> showStaff());

        root.addView(nav, matchWrap());
        setContentView(root);
    }

    private void showHome() {
        content.removeAllViews();
        selectNav(homeNav);

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
        content.addView(memberCard, matchWrap());

        content.addView(sectionTitle("今月の会員特典"), topMargin(dp(22)));
        LinearLayout coupon = card(Color.rgb(59, 42, 28), 16);
        coupon.setPadding(dp(18), dp(16), dp(18), dp(16));
        coupon.addView(text(currentMonth() + "月限定クーポン", 13, GOLD, true), matchWrap());
        coupon.addView(text(couponText(rank.name), 20, TEXT, true), topMargin(dp(7)));
        String state = prefs.getBoolean("coupon_used", false) ? "使用済み" : "未使用・月1回";
        coupon.addView(text(state, 12, prefs.getBoolean("coupon_used", false) ? MUTED : GOLD, true), topMargin(dp(9)));
        coupon.setOnClickListener(v -> showCoupon());
        content.addView(coupon, topMargin(dp(9)));

        content.addView(sectionTitle("濱匠別邸を愉しむ"), topMargin(dp(22)));
        content.addView(tileRow("季節のおすすめ", "旬の食材・限定料理", "宴会・接待", "コース・お席のご案内"), topMargin(dp(9)));
        content.addView(tileRow("日本酒", "季節酒・おすすめ銘柄", "おすすめドリンク", "ビール・焼酎・ハイボール"), topMargin(dp(10)));

        TextView note = text("v0.3 試作版　ポイントとクーポンの操作は端末内に保存されます", 11, MUTED, false);
        note.setGravity(Gravity.CENTER);
        content.addView(note, topMargin(dp(20)));
    }

    private void showMember() {
        content.removeAllViews();
        selectNav(memberNav);

        int available = prefs.getInt("available", 0);
        int cumulative = prefs.getInt("cumulative", 0);
        RankInfo rank = rankInfo(cumulative);

        content.addView(sectionTitle("会員証・ポイント"), matchWrap());

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
        LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams(dp(230), dp(230));
        qrCard.addView(qr, qrLp);
        TextView qrLabel = text("会計時にこのQRをスタッフへご提示ください", 13, Color.rgb(45, 40, 34), true);
        qrLabel.setGravity(Gravity.CENTER);
        qrCard.addView(qrLabel, topMargin(dp(10)));
        content.addView(qrCard, topMargin(dp(14)));

        LinearLayout progressCard = card(CARD, 16);
        progressCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        progressCard.addView(text("ランクアップ", 15, GOLD, true), matchWrap());
        progressCard.addView(text(rank.nextText, 21, TEXT, true), topMargin(dp(7)));
        progressCard.addView(text("粋 0　雅 3,000　匠 10,000　別邸 30,000", 12, MUTED, false), topMargin(dp(10)));
        content.addView(progressCard, topMargin(dp(14)));
    }

    private void showCoupon() {
        content.removeAllViews();
        selectNav(couponNav);

        int cumulative = prefs.getInt("cumulative", 0);
        RankInfo rank = rankInfo(cumulative);
        boolean used = prefs.getBoolean("coupon_used", false);

        content.addView(sectionTitle("月1回 会員クーポン"), matchWrap());
        LinearLayout coupon = card(used ? Color.rgb(42, 39, 35) : Color.rgb(61, 43, 28), 20);
        coupon.setPadding(dp(20), dp(20), dp(20), dp(20));
        coupon.addView(text(currentMonth() + "月 会員様限定", 14, GOLD, true), matchWrap());
        coupon.addView(text(rank.name + " 会員", 28, TEXT, true), topMargin(dp(8)));
        coupon.addView(text(couponText(rank.name), 21, used ? MUTED : GOLD, true), topMargin(dp(10)));
        addDivider(coupon, dp(16));
        coupon.addView(text(used ? "使用済み" : "未使用", 19, used ? MUTED : GOLD, true), topMargin(dp(13)));
        coupon.addView(text("有効期限：" + currentMonth() + "月末まで\n利用回数：月1回\n※使用処理はスタッフ画面から行います", 14, TEXT, false), topMargin(dp(10)));
        content.addView(coupon, topMargin(dp(12)));

        LinearLayout ranks = card(CARD, 16);
        ranks.setPadding(dp(18), dp(16), dp(18), dp(16));
        ranks.addView(text("ランク連動", 16, GOLD, true), matchWrap());
        ranks.addView(text("粋　　通常会員クーポン\n雅　　ビール・焼酎・ハイボールから1杯\n匠　　日本酒を含む対象ドリンク1杯\n別邸　対象ドリンク＋季節の一品", 14, TEXT, false), topMargin(dp(12)));
        content.addView(ranks, topMargin(dp(14)));
    }

    private void showStaff() {
        content.removeAllViews();
        selectNav(staffNav);
        content.addView(sectionTitle("スタッフ管理"), matchWrap());

        if (!staffUnlocked) {
            LinearLayout login = card(CARD, 18);
            login.setPadding(dp(18), dp(18), dp(18), dp(18));
            login.addView(text("スタッフPIN", 16, GOLD, true), matchWrap());
            login.addView(text("試作PIN：1188", 12, MUTED, false), topMargin(dp(5)));
            EditText pin = input("PINを入力", true);
            login.addView(pin, topMargin(dp(12)));
            Button open = actionButton("管理画面を開く");
            open.setOnClickListener(v -> {
                if (STAFF_PIN.equals(pin.getText().toString().trim())) {
                    staffUnlocked = true;
                    showStaff();
                } else {
                    Toast.makeText(this, "PINが違います", Toast.LENGTH_SHORT).show();
                }
            });
            login.addView(open, topMargin(dp(12)));
            content.addView(login, topMargin(dp(12)));
            return;
        }

        int available = prefs.getInt("available", 0);
        int cumulative = prefs.getInt("cumulative", 0);
        RankInfo rank = rankInfo(cumulative);

        LinearLayout member = card(CARD_ALT, 18);
        member.setPadding(dp(18), dp(16), dp(18), dp(16));
        member.addView(text("会員ID  " + MEMBER_ID, 13, MUTED, false), matchWrap());
        member.addView(text(rank.name + " 会員", 24, GOLD, true), topMargin(dp(5)));
        member.addView(text("利用可能 " + available + " pt　／　累計 " + cumulative + " pt", 14, TEXT, false), topMargin(dp(8)));
        member.addView(text("最終操作：" + prefs.getString("last_action", "なし"), 12, MUTED, false), topMargin(dp(8)));
        content.addView(member, topMargin(dp(12)));

        LinearLayout addCard = card(CARD, 16);
        addCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        addCard.addView(text("ポイント付与", 16, GOLD, true), matchWrap());
        addCard.addView(text("会計金額を入力すると、100円＝1ptで付与します", 12, MUTED, false), topMargin(dp(5)));
        EditText amount = input("例：12800", false);
        addCard.addView(amount, topMargin(dp(10)));
        Button add = actionButton("会計金額からポイント付与");
        add.setOnClickListener(v -> {
            int yen = parseInt(amount.getText().toString());
            if (yen < 100) {
                toast("100円以上を入力してください");
                return;
            }
            int pts = yen / 100;
            prefs.edit()
                    .putInt("available", prefs.getInt("available", 0) + pts)
                    .putInt("cumulative", prefs.getInt("cumulative", 0) + pts)
                    .putString("last_action", yen + "円会計 → " + pts + "pt付与")
                    .apply();
            toast(pts + "pt付与しました");
            showStaff();
        });
        addCard.addView(add, topMargin(dp(10)));
        content.addView(addCard, topMargin(dp(14)));

        LinearLayout useCard = card(CARD, 16);
        useCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        useCard.addView(text("ポイント使用", 16, GOLD, true), matchWrap());
        EditText usePts = input("使用ポイント数", false);
        useCard.addView(usePts, topMargin(dp(10)));
        Button use = actionButton("ポイントを使用");
        use.setOnClickListener(v -> {
            int pts = parseInt(usePts.getText().toString());
            int now = prefs.getInt("available", 0);
            if (pts <= 0 || pts > now) {
                toast("利用可能ポイント以内で入力してください");
                return;
            }
            prefs.edit()
                    .putInt("available", now - pts)
                    .putString("last_action", pts + "pt使用")
                    .apply();
            toast(pts + "pt使用しました");
            showStaff();
        });
        useCard.addView(use, topMargin(dp(10)));
        content.addView(useCard, topMargin(dp(14)));

        LinearLayout cpCard = card(CARD, 16);
        cpCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        cpCard.addView(text("今月のクーポン", 16, GOLD, true), matchWrap());
        boolean used = prefs.getBoolean("coupon_used", false);
        cpCard.addView(text(used ? "現在：使用済み" : "現在：未使用", 14, used ? MUTED : TEXT, true), topMargin(dp(7)));
        Button useCoupon = actionButton(used ? "クーポンは使用済みです" : "クーポンを使用済みにする");
        useCoupon.setEnabled(!used);
        useCoupon.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("クーポン使用確認")
                .setMessage("この会員の今月クーポンを使用済みにしますか？")
                .setNegativeButton("戻る", null)
                .setPositiveButton("使用する", (d, w) -> {
                    prefs.edit().putBoolean("coupon_used", true).putString("last_action", currentMonth() + "月クーポン使用").apply();
                    toast("クーポンを使用済みにしました");
                    showStaff();
                }).show());
        cpCard.addView(useCoupon, topMargin(dp(10)));
        content.addView(cpCard, topMargin(dp(14)));

        Button reset = actionButton("試作データを初期状態へ戻す");
        reset.setBackground(roundRect(DANGER, 12));
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("試作データをリセット")
                .setMessage("ポイントとクーポン状態を初期値へ戻します。")
                .setNegativeButton("やめる", null)
                .setPositiveButton("リセット", (d, w) -> {
                    prefs.edit().clear().apply();
                    ensureDefaults();
                    toast("初期状態へ戻しました");
                    showStaff();
                }).show());
        content.addView(reset, topMargin(dp(16)));
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
                for (int x = 0; x < size; x++) {
                    pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
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

    private EditText input(String hint, boolean password) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.rgb(135, 125, 112));
        e.setTextColor(TEXT);
        e.setTextSize(17);
        e.setSingleLine(true);
        e.setPadding(dp(14), dp(11), dp(14), dp(11));
        e.setBackground(roundRect(Color.rgb(29, 25, 21), 12));
        e.setInputType(password ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD : InputType.TYPE_CLASS_NUMBER);
        return e;
    }

    private Button actionButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.rgb(29, 24, 18));
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setPadding(dp(14), dp(10), dp(14), dp(10));
        b.setBackground(roundRect(GOLD, 12));
        return b;
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

    private void selectNav(Button selected) {
        Button[] all = {homeNav, memberNav, couponNav, staffNav};
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

    private int parseInt(String value) {
        try { return Integer.parseInt(value.trim()); } catch (Exception e) { return 0; }
    }

    private int currentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
