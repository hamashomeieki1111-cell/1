package jp.hamasho.bettei;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;

public class MainActivity extends Activity {

    private static final int BG = Color.rgb(23, 19, 15);
    private static final int CARD = Color.rgb(38, 32, 26);
    private static final int CARD_ALT = Color.rgb(48, 40, 31);
    private static final int GOLD = Color.rgb(214, 176, 96);
    private static final int GOLD_DARK = Color.rgb(153, 119, 56);
    private static final int TEXT = Color.rgb(246, 241, 231);
    private static final int MUTED = Color.rgb(188, 175, 154);
    private static final int DIVIDER = Color.rgb(79, 67, 53);

    private LinearLayout content;
    private Button homeNav;
    private Button memberNav;
    private Button couponNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildShell();
        showHome();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(18), dp(20), dp(14));
        header.setBackgroundColor(Color.rgb(18, 15, 12));

        TextView brand = text("濱匠別邸", 28, GOLD, true);
        brand.setGravity(Gravity.CENTER);
        header.addView(brand, matchWrap());

        TextView tagline = text("蕎麦と酒を、粋に愉しむ。", 12, MUTED, false);
        tagline.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tagLp = matchWrap();
        tagLp.topMargin = dp(3);
        header.addView(tagline, tagLp);
        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(26));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(scroll, scrollLp);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(8), dp(7), dp(8), dp(9));
        nav.setBackgroundColor(Color.rgb(18, 15, 12));

        homeNav = navButton("ホーム");
        memberNav = navButton("会員証");
        couponNav = navButton("クーポン");
        Button infoNav = navButton("店舗");

        nav.addView(homeNav, weighted());
        nav.addView(memberNav, weighted());
        nav.addView(couponNav, weighted());
        nav.addView(infoNav, weighted());

        homeNav.setOnClickListener(v -> showHome());
        memberNav.setOnClickListener(v -> showMember());
        couponNav.setOnClickListener(v -> showCoupon());
        infoNav.setOnClickListener(v -> showStore());

        root.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
    }

    private void showHome() {
        content.removeAllViews();
        selectNav(homeNav);

        LinearLayout memberCard = card(CARD_ALT, 18);
        memberCard.setPadding(dp(18), dp(16), dp(18), dp(16));

        LinearLayout memberTop = new LinearLayout(this);
        memberTop.setOrientation(LinearLayout.HORIZONTAL);
        memberTop.setGravity(Gravity.CENTER_VERTICAL);
        TextView memberTitle = text("HAMASHO MEMBER", 13, MUTED, true);
        memberTop.addView(memberTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView rank = badge("雅");
        memberTop.addView(rank);
        memberCard.addView(memberTop, matchWrap());

        TextView points = text("428 pt", 32, GOLD, true);
        LinearLayout.LayoutParams ptsLp = matchWrap();
        ptsLp.topMargin = dp(10);
        memberCard.addView(points, ptsLp);

        TextView pointsLabel = text("利用可能ポイント", 12, MUTED, false);
        memberCard.addView(pointsLabel, matchWrap());

        TextView cumulative = text("累計 8,750 pt　／　あと 1,250 pt で「匠」", 13, TEXT, false);
        LinearLayout.LayoutParams cumLp = matchWrap();
        cumLp.topMargin = dp(12);
        memberCard.addView(cumulative, cumLp);

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(10000);
        progress.setProgress(8750);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(DIVIDER));
        LinearLayout.LayoutParams progLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7));
        progLp.topMargin = dp(8);
        memberCard.addView(progress, progLp);

        memberCard.setOnClickListener(v -> showMember());
        content.addView(memberCard, matchWrap());

        TextView monthly = sectionTitle("今月の会員特典");
        content.addView(monthly, topMargin(dp(22)));

        LinearLayout coupon = card(Color.rgb(59, 42, 28), 16);
        coupon.setPadding(dp(18), dp(16), dp(18), dp(16));
        TextView couponTag = text(currentMonth() + "月限定クーポン", 13, GOLD, true);
        coupon.addView(couponTag, matchWrap());
        TextView couponMain = text("雅会員 特典\nビール・焼酎・ハイボールから 1杯", 20, TEXT, true);
        LinearLayout.LayoutParams cpLp = matchWrap();
        cpLp.topMargin = dp(7);
        coupon.addView(couponMain, cpLp);
        TextView couponNote = text("月1回・当月末まで　※試作内容", 12, MUTED, false);
        LinearLayout.LayoutParams cnLp = matchWrap();
        cnLp.topMargin = dp(8);
        coupon.addView(couponNote, cnLp);
        coupon.setOnClickListener(v -> showCoupon());
        content.addView(coupon, topMargin(dp(9)));

        TextView menuTitle = sectionTitle("濱匠別邸を愉しむ");
        content.addView(menuTitle, topMargin(dp(22)));

        LinearLayout row1 = tileRow("季節のおすすめ", "旬の食材・限定料理", "宴会・接待", "コース・お席のご案内");
        content.addView(row1, topMargin(dp(9)));
        LinearLayout row2 = tileRow("日本酒", "季節酒・おすすめ銘柄", "おすすめドリンク", "ビール・焼酎・ハイボール");
        content.addView(row2, topMargin(dp(10)));

        TextView sample = text("※ 現在は動作確認用の試作データです", 11, MUTED, false);
        sample.setGravity(Gravity.CENTER);
        content.addView(sample, topMargin(dp(20)));
    }

    private void showMember() {
        content.removeAllViews();
        selectNav(memberNav);

        TextView title = sectionTitle("会員証・ポイント");
        content.addView(title, matchWrap());

        LinearLayout card = card(CARD_ALT, 20);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView member = text("濱匠別邸 MEMBER", 15, MUTED, true);
        card.addView(member, matchWrap());

        LinearLayout rankLine = new LinearLayout(this);
        rankLine.setOrientation(LinearLayout.HORIZONTAL);
        rankLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView rankName = text("雅 会員", 30, GOLD, true);
        rankLine.addView(rankName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        rankLine.addView(badge("雅"));
        card.addView(rankLine, topMargin(dp(8)));

        addDivider(card, dp(16));

        LinearLayout ptsRow = new LinearLayout(this);
        ptsRow.setOrientation(LinearLayout.HORIZONTAL);
        ptsRow.addView(stat("428 pt", "利用可能ポイント"), weighted());
        ptsRow.addView(stat("8,750 pt", "累計ポイント"), weighted());
        card.addView(ptsRow, topMargin(dp(14)));

        content.addView(card, topMargin(dp(12)));

        LinearLayout progressCard = card(CARD, 16);
        progressCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        progressCard.addView(text("次のランクまで", 14, MUTED, true), matchWrap());
        progressCard.addView(text("あと 1,250 pt で「匠」", 22, TEXT, true), topMargin(dp(7)));
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(10000);
        progress.setProgress(8750);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(DIVIDER));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        pp.topMargin = dp(12);
        progressCard.addView(progress, pp);
        progressCard.addView(text("粋 0　　雅 3,000　　匠 10,000　　別邸 30,000", 12, MUTED, false), topMargin(dp(9)));
        content.addView(progressCard, topMargin(dp(14)));

        LinearLayout ruleCard = card(Color.rgb(31, 27, 23), 16);
        ruleCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        ruleCard.addView(text("ポイント仕様", 16, GOLD, true), matchWrap());
        ruleCard.addView(text("100円（税込）＝ 1ポイント\n\n利用可能ポイントと累計ポイントは別管理。\nポイントを使っても累計ポイントは減らないため、会員ランクは維持されます。", 14, TEXT, false), topMargin(dp(10)));
        content.addView(ruleCard, topMargin(dp(14)));

        Button qrPreview = actionButton("会員QR（次の版で追加）");
        qrPreview.setEnabled(false);
        content.addView(qrPreview, topMargin(dp(16)));
    }

    private void showCoupon() {
        content.removeAllViews();
        selectNav(couponNav);

        TextView title = sectionTitle("月1回 会員クーポン");
        content.addView(title, matchWrap());

        LinearLayout coupon = card(Color.rgb(61, 43, 28), 20);
        coupon.setPadding(dp(20), dp(20), dp(20), dp(20));
        coupon.addView(text(currentMonth() + "月 会員様限定", 14, GOLD, true), matchWrap());
        coupon.addView(text("雅 会員", 28, TEXT, true), topMargin(dp(8)));
        coupon.addView(text("ビール・焼酎・ハイボールから\nお好きな1杯サービス", 21, GOLD, true), topMargin(dp(10)));
        addDivider(coupon, dp(16));
        coupon.addView(text("有効期限：" + currentMonth() + "月末まで\n利用回数：月1回\nランクに応じて特典内容が変わります", 14, TEXT, false), topMargin(dp(14)));
        content.addView(coupon, topMargin(dp(12)));

        LinearLayout ranks = card(CARD, 16);
        ranks.setPadding(dp(18), dp(16), dp(18), dp(16));
        ranks.addView(text("ランク連動（試作例）", 16, GOLD, true), matchWrap());
        ranks.addView(text("粋　　通常会員クーポン\n雅　　ビール・焼酎・ハイボールから1杯\n匠　　日本酒を含む対象ドリンク1杯\n別邸　対象ドリンク＋季節の一品", 14, TEXT, false), topMargin(dp(12)));
        content.addView(ranks, topMargin(dp(14)));

        Button use = actionButton("クーポンを使用する（次の版）");
        use.setEnabled(false);
        content.addView(use, topMargin(dp(16)));
        content.addView(text("店側の確認後に使用済みになる仕組みを次の版で入れます。", 12, MUTED, false), topMargin(dp(8)));
    }

    private void showStore() {
        content.removeAllViews();
        selectNav(null);
        content.addView(sectionTitle("店舗・予約"), matchWrap());

        LinearLayout card = card(CARD, 18);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.addView(text("濱匠別邸", 25, GOLD, true), matchWrap());
        card.addView(text("店舗情報・電話・ネット予約は次の段階で実際のリンクを設定します。", 15, TEXT, false), topMargin(dp(12)));
        content.addView(card, topMargin(dp(12)));
    }

    private LinearLayout tileRow(String aTitle, String aSub, String bTitle, String bSub) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setWeightSum(2f);

        LinearLayout a = tile(aTitle, aSub);
        LinearLayout b = tile(bTitle, bSub);
        LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(0, dp(118), 1f);
        lpA.rightMargin = dp(5);
        LinearLayout.LayoutParams lpB = new LinearLayout.LayoutParams(0, dp(118), 1f);
        lpB.leftMargin = dp(5);
        row.addView(a, lpA);
        row.addView(b, lpB);
        return row;
    }

    private LinearLayout tile(String title, String sub) {
        LinearLayout box = card(CARD, 16);
        box.setGravity(Gravity.BOTTOM);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        TextView t = text(title, 17, TEXT, true);
        box.addView(t, matchWrap());
        TextView s = text(sub, 11, MUTED, false);
        box.addView(s, topMargin(dp(4)));
        return box;
    }

    private LinearLayout stat(String value, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.addView(text(value, 22, GOLD, true), matchWrap());
        box.addView(text(label, 11, MUTED, false), topMargin(dp(4)));
        return box;
    }

    private TextView sectionTitle(String value) {
        return text(value, 19, GOLD, true);
    }

    private TextView badge(String value) {
        TextView v = text(value, 16, Color.rgb(31, 24, 15), true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(12), dp(7), dp(12), dp(7));
        v.setBackground(roundRect(GOLD, 99, GOLD));
        return v;
    }

    private Button navButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11);
        b.setTextColor(MUTED);
        b.setAllCaps(false);
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setMinHeight(dp(46));
        b.setMinWidth(0);
        b.setBackgroundColor(Color.TRANSPARENT);
        return b;
    }

    private Button actionButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(15);
        b.setTextColor(Color.rgb(30, 24, 17));
        b.setAllCaps(false);
        b.setBackground(roundRect(GOLD, 14, GOLD));
        b.setPadding(dp(12), dp(10), dp(12), dp(10));
        return b;
    }

    private void selectNav(Button selected) {
        Button[] buttons = new Button[]{homeNav, memberNav, couponNav};
        for (Button b : buttons) {
            if (b == null) continue;
            b.setTextColor(b == selected ? GOLD : MUTED);
            b.setTypeface(null, b == selected ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private LinearLayout card(int color, int radiusDp) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(roundRect(color, radiusDp, DIVIDER));
        return l;
    }

    private GradientDrawable roundRect(int fillColor, int radiusDp, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fillColor);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), strokeColor);
        return d;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.12f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void addDivider(LinearLayout parent, int top) {
        View d = new View(this);
        d.setBackgroundColor(DIVIDER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.topMargin = top;
        parent.addView(d, lp);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams topMargin(int marginPx) {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = marginPx;
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int currentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }
}
