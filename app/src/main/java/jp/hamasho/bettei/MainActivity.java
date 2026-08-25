package jp.hamasho.bettei;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(12,10,8);
    private static final int BLACK = Color.rgb(7,6,5);
    private static final int CARD = Color.rgb(27,22,18);
    private static final int CARD2 = Color.rgb(42,32,24);
    private static final int GOLD = Color.rgb(202,165,84);
    private static final int GOLD2 = Color.rgb(236,211,151);
    private static final int TEXT = Color.rgb(249,244,235);
    private static final int MUTED = Color.rgb(185,170,146);
    private static final int DIV = Color.rgb(78,63,45);

    private static final String MEMBER_ID = "HMB-000001";
    private static final String SUPABASE_URL = "https://sedprfuiymcgbhatofwb.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_BIJQSq4IQRxgwwqWd3YmTQ_etujBnSj";

    private static final int HOME=0, MEMBER=1, COUPON=2, STORE=3;
    private LinearLayout content;
    private Button navHome, navMember, navCoupon, navStore;
    private SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private int page = HOME;
    private boolean syncing = false;
    private String syncText = "会員情報を確認中…";
    private Bitmap sprite;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BLACK);
        getWindow().setNavigationBarColor(BLACK);
        prefs=getSharedPreferences("hamasho_trial",MODE_PRIVATE);
        if(!prefs.contains("available")) prefs.edit().putInt("available",428).putInt("cumulative",8750).putBoolean("coupon_used",false).apply();
        sprite=BitmapFactory.decodeResource(getResources(),R.drawable.home_sprite_v09);
        buildShell(); showHome();
    }
    @Override protected void onResume(){ super.onResume(); sync(); }
    @Override protected void onDestroy(){ io.shutdownNow(); if(sprite!=null&&!sprite.isRecycled()) sprite.recycle(); super.onDestroy(); }

    private void buildShell(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setFitsSystemWindows(true);

        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(16),dp(8),dp(16),dp(8)); top.setBackgroundColor(BLACK);
        TextView t=text("濱匠",20,GOLD2,true); t.setTypeface(Typeface.SERIF,Typeface.BOLD); top.addView(t,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        TextView sub=text("名駅別邸",11,MUTED,true); top.addView(sub);
        root.addView(top,matchWrap());

        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(12),dp(10),dp(12),dp(28));
        scroll.addView(content,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        LinearLayout nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setPadding(dp(4),dp(4),dp(4),dp(6)); nav.setBackgroundColor(BLACK); nav.setFitsSystemWindows(true);
        navHome=navButton("⌂\nホーム"); navMember=navButton("▣\n会員証"); navCoupon=navButton("◇\nクーポン"); navStore=navButton("◎\n店舗情報");
        nav.addView(navHome,weighted()); nav.addView(navMember,weighted()); nav.addView(navCoupon,weighted()); nav.addView(navStore,weighted());
        navHome.setOnClickListener(v->showHome()); navMember.setOnClickListener(v->showMember()); navCoupon.setOnClickListener(v->showCoupon()); navStore.setOnClickListener(v->showStore());
        root.addView(nav,matchWrap()); setContentView(root);
    }

    private void showHome(){
        page=HOME; content.removeAllViews(); select(navHome);
        addHero();
        int a=prefs.getInt("available",0), c=prefs.getInt("cumulative",0); Rank r=rank(c);
        LinearLayout card=gradientCard(new int[]{Color.rgb(55,42,29),Color.rgb(20,17,14)},20,GOLD); card.setPadding(dp(18),dp(16),dp(18),dp(16)); card.setElevation(dp(3));
        LinearLayout line=new LinearLayout(this); line.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=text("濱匠 MEMBER",12,TEXT,true); label.setTypeface(Typeface.SERIF,Typeface.BOLD); line.addView(label,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f)); line.addView(badge(r.name)); card.addView(line,matchWrap());
        LinearLayout stats=new LinearLayout(this); stats.setOrientation(LinearLayout.HORIZONTAL); stats.setPadding(0,dp(13),0,0);
        stats.addView(stat(fmt(a)+" pt","現在ポイント",true),weighted()); stats.addView(stat(fmt(c)+" pt","累計ポイント",false),weighted()); card.addView(stats,matchWrap());
        addDivider(card,dp(12)); card.addView(text(r.nextText,11,GOLD2,true),top(dp(10)));
        ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); p.setMax(r.max); p.setProgress(r.value); p.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD)); p.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(DIV));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(6)); pp.topMargin=dp(8); card.addView(p,pp); card.setOnClickListener(v->showMember()); content.addView(card,top(dp(12)));
        addSyncRow();

        content.addView(section("今月のおもてなし","MEMBER BENEFIT"),top(dp(16)));
        boolean used=prefs.getBoolean("coupon_used",false); LinearLayout cp=gradientCard(used?new int[]{Color.rgb(42,39,35),Color.rgb(27,25,22)}:new int[]{Color.rgb(91,56,31),Color.rgb(45,31,22)},17,used?DIV:GOLD); cp.setPadding(dp(17),dp(15),dp(17),dp(15));
        cp.addView(text(month()+"月限定  ｜  "+r.name+" 会員",10,GOLD2,true)); cp.addView(text(couponText(r.name),19,used?MUTED:TEXT,true),top(dp(6))); cp.addView(text(used?"ご利用済み":"月1回ご利用いただけます",10,used?MUTED:GOLD2,true),top(dp(9))); cp.setOnClickListener(v->showCoupon()); content.addView(cp,top(dp(8)));

        content.addView(section("濱匠を愉しむ","DISCOVER"),top(dp(20)));
        content.addView(tileRow(0,380,"季節のおすすめ","旬の味覚・限定料理",400,380,"宴会・接待","大切なお席に"),top(dp(8)));
        content.addView(tileRow(0,660,"日本酒","厳選した地酒をご用意",400,660,"おすすめドリンク","焼酎・ビール・ハイボール"),top(dp(8)));

        LinearLayout quick=new LinearLayout(this); quick.setOrientation(LinearLayout.HORIZONTAL); quick.setPadding(0,dp(14),0,0);
        quick.addView(quickBox("クーポン","今月の特典",v->showCoupon()),weightedGap(true)); quick.addView(quickBox("会員証","QR・ポイント",v->showMember()),weightedGap(false)); content.addView(quick,matchWrap());
        TextView note=text("社長確認用 DEMO  ｜  v0.9 REAL PHOTO",8,Color.rgb(103,92,77),false); note.setGravity(Gravity.CENTER); content.addView(note,top(dp(16)));
    }

    private void addHero(){
        FrameLayout hero=new FrameLayout(this); hero.setBackground(roundRect(Color.rgb(28,22,17),20)); hero.setClipToOutline(true);
        ImageView im=spriteView(0,0,800,380); hero.addView(im,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(220)));
        View shade=new View(this); shade.setBackground(gradient(new int[]{Color.argb(0,0,0,0),Color.argb(150,0,0,0)},0,0)); hero.addView(shade,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(220)));
        LinearLayout copy=new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); copy.setPadding(dp(18),dp(12),dp(18),dp(16));
        FrameLayout.LayoutParams clp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.BOTTOM);
        copy.addView(text("名駅別邸",11,GOLD2,true)); copy.addView(text("四季の恵みを、濱匠で。",20,Color.WHITE,true),top(dp(4))); hero.addView(copy,clp); content.addView(hero,matchWrap());
    }

    private LinearLayout tileRow(int x1,int y1,String t1,String s1,int x2,int y2,String t2,String s2){
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(150),1f); a.rightMargin=dp(4); LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,dp(150),1f); b.leftMargin=dp(4);
        row.addView(tile(x1,y1,t1,s1),a); row.addView(tile(x2,y2,t2,s2),b); return row;
    }
    private FrameLayout tile(int x,int y,String title,String sub){
        FrameLayout f=new FrameLayout(this); f.setBackground(roundRect(CARD,16)); f.setClipToOutline(true);
        f.addView(spriteView(x,y,400,280),new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        View shade=new View(this); shade.setBackground(gradient(new int[]{Color.argb(0,0,0,0),Color.argb(205,0,0,0)},0,0)); f.addView(shade,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(13),dp(8),dp(13),dp(11)); FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.BOTTOM);
        c.addView(text(title,16,Color.WHITE,true)); c.addView(text(sub,10,Color.rgb(229,218,199),false),top(dp(2))); f.addView(c,lp); return f;
    }
    private ImageView spriteView(int x,int y,int w,int h){
        ImageView v=new ImageView(this); v.setScaleType(ImageView.ScaleType.CENTER_CROP); if(sprite!=null){ Bitmap part=Bitmap.createBitmap(sprite,x,y,w,h); v.setImageBitmap(part); } return v;
    }

    private void showMember(){
        page=MEMBER; content.removeAllViews(); select(navMember); content.addView(pageTitle("会員証","MEMBERS CARD"));
        int a=prefs.getInt("available",0), c=prefs.getInt("cumulative",0); Rank r=rank(c);
        LinearLayout card=gradientCard(new int[]{Color.rgb(72,53,36),Color.rgb(28,23,19)},22,GOLD); card.setPadding(dp(20),dp(19),dp(20),dp(19));
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.addView(text("濱匠 MEMBER",12,TEXT,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f)); row.addView(badge(r.name)); card.addView(row);
        card.addView(text(r.name+" 会員",29,GOLD2,true),top(dp(12))); card.addView(text("MEMBER ID  "+MEMBER_ID,10,MUTED,false),top(dp(3))); addDivider(card,dp(14));
        LinearLayout st=new LinearLayout(this); st.setPadding(0,dp(14),0,0); st.addView(stat(fmt(a)+" pt","利用可能",true),weighted()); st.addView(stat(fmt(c)+" pt","累計",false),weighted()); card.addView(st); content.addView(card,top(dp(12)));
        LinearLayout qouter=card(Color.rgb(35,29,24),18); qouter.setGravity(Gravity.CENTER); qouter.setPadding(dp(16),dp(16),dp(16),dp(16)); LinearLayout white=card(Color.WHITE,14); white.setGravity(Gravity.CENTER); white.setPadding(dp(12),dp(12),dp(12),dp(12));
        ImageView qr=new ImageView(this); Bitmap qb=makeQr("HAMASHO_MEMBER:"+MEMBER_ID,720); if(qb!=null) qr.setImageBitmap(qb); white.addView(qr,new LinearLayout.LayoutParams(dp(220),dp(220))); qouter.addView(white,new LinearLayout.LayoutParams(dp(248),dp(248))); TextView ql=text("会計時にこのQRをスタッフへご提示ください",12,TEXT,true); ql.setGravity(Gravity.CENTER); qouter.addView(ql,top(dp(12))); content.addView(qouter,top(dp(14)));
        addSyncRow();
        LinearLayout rankBox=gradientCard(new int[]{CARD2,CARD},17,DIV); rankBox.setPadding(dp(17),dp(16),dp(17),dp(16)); rankBox.addView(text("次のランクまで",11,GOLD,true)); rankBox.addView(text(r.nextText,18,TEXT,true),top(dp(5))); rankBox.addView(text("粋 0 ｜ 雅 3,000 ｜ 匠 10,000 ｜ 別邸 30,000",10,MUTED,false),top(dp(8))); content.addView(rankBox,top(dp(13)));
    }

    private void showCoupon(){
        page=COUPON; content.removeAllViews(); select(navCoupon); content.addView(pageTitle("会員クーポン","MONTHLY BENEFIT"));
        Rank r=rank(prefs.getInt("cumulative",0)); boolean used=prefs.getBoolean("coupon_used",false); LinearLayout cp=gradientCard(used?new int[]{Color.rgb(45,42,38),Color.rgb(29,27,24)}:new int[]{Color.rgb(103,63,32),Color.rgb(49,34,23)},22,used?DIV:GOLD); cp.setPadding(dp(20),dp(20),dp(20),dp(20));
        cp.addView(text(month()+"月  MEMBER'S SPECIAL",11,GOLD2,true)); cp.addView(text(r.name+" 会員様限定",25,TEXT,true),top(dp(7))); cp.addView(text(couponText(r.name),21,used?MUTED:GOLD2,true),top(dp(10))); addDivider(cp,dp(15)); cp.addView(text(used?"✓ 使用済み":"● ご利用いただけます",16,used?MUTED:TEXT,true),top(dp(12))); cp.addView(text("有効期限："+month()+"月末まで\n利用回数：月1回\n※使用確定は店舗スタッフが行います",13,MUTED,false),top(dp(9))); content.addView(cp,top(dp(12))); addSyncRow();
    }

    private void showStore(){
        page=STORE; content.removeAllViews(); select(navStore); content.addView(pageTitle("店舗情報","HAMASHO MEIEKI"));
        FrameLayout photo=new FrameLayout(this); photo.setBackground(roundRect(CARD,18)); photo.setClipToOutline(true); photo.addView(spriteView(0,0,800,380),new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(190))); content.addView(photo,top(dp(10)));
        LinearLayout info=gradientCard(new int[]{CARD2,CARD},18,DIV); info.setPadding(dp(18),dp(17),dp(18),dp(17)); info.addView(text("濱匠 名駅別邸",24,GOLD2,true)); info.addView(text("愛知県名古屋市中村区名駅2-41-3\nサンエスケービル1階\n\nJR名古屋駅 徒歩5分\n月〜金 11:30〜14:00\n月〜土 17:00〜23:00\n定休日：日曜（不定休あり）",13,TEXT,false),top(dp(10))); content.addView(info,top(dp(12)));
        LinearLayout point=gradientCard(new int[]{Color.rgb(49,36,25),CARD},17,DIV); point.setPadding(dp(17),dp(15),dp(17),dp(15)); point.addView(text("ポイントのご利用",16,GOLD2,true)); point.addView(text("100円（税込）＝1ポイント\n1ポイント＝1円\n300ポイントからご利用いただけます。",13,MUTED,false),top(dp(8))); content.addView(point,top(dp(12)));
    }

    private void addSyncRow(){
        LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(3),dp(7),dp(3),0); r.addView(text(syncing?"会員情報を更新しています…":syncText,9,Color.rgb(132,119,99),false),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f)); Button b=miniButton(syncing?"更新中":"更新"); b.setEnabled(!syncing); b.setOnClickListener(v->sync()); r.addView(b,new LinearLayout.LayoutParams(dp(68),dp(34))); content.addView(r,matchWrap());
    }
    private void sync(){ if(syncing)return; syncing=true; render(); io.execute(()->{ try{ String id=URLEncoder.encode(MEMBER_ID,StandardCharsets.UTF_8.name()); String ep=SUPABASE_URL+"/rest/v1/demo_members?member_id=eq."+id+"&select=available_points,cumulative_points,coupon_used"; HttpURLConnection c=(HttpURLConnection)new URL(ep).openConnection(); c.setRequestMethod("GET"); c.setRequestProperty("apikey",SUPABASE_KEY); c.setRequestProperty("Accept","application/json"); c.setConnectTimeout(10000); c.setReadTimeout(10000); int code=c.getResponseCode(); InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream(); String body=readAll(in); c.disconnect(); if(code<200||code>=300)throw new Exception("HTTP "+code); JSONArray rows=new JSONArray(body); if(rows.length()==0)throw new Exception("会員が見つかりません"); JSONObject o=rows.getJSONObject(0); int a=o.getInt("available_points"), total=o.getInt("cumulative_points"); boolean used=o.getBoolean("coupon_used"); prefs.edit().putInt("available",a).putInt("cumulative",total).putBoolean("coupon_used",used).apply(); runOnUiThread(()->{syncing=false;syncText="会員情報を更新しました";render();}); }catch(Exception e){runOnUiThread(()->{syncing=false;syncText="更新できません";render();});} }); }
    private String readAll(InputStream in)throws Exception{ if(in==null)return""; BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8)); StringBuilder s=new StringBuilder(); String l; while((l=r.readLine())!=null)s.append(l); r.close(); return s.toString(); }
    private void render(){ if(content==null)return; if(page==MEMBER)showMember(); else if(page==COUPON)showCoupon(); else if(page==STORE)showStore(); else showHome(); }

    private Rank rank(int c){ if(c>=30000)return new Rank("別邸","最高ランクです",30000,30000); if(c>=10000)return new Rank("匠","あと "+fmt(30000-c)+" pt で「別邸」",c-10000,20000); if(c>=3000)return new Rank("雅","あと "+fmt(10000-c)+" pt で「匠」",c-3000,7000); return new Rank("粋","あと "+fmt(3000-c)+" pt で「雅」",c,3000); }
    private String couponText(String r){ if("雅".equals(r))return"ビール・焼酎・ハイボールから\nお好きな1杯サービス"; if("匠".equals(r))return"日本酒を含む対象ドリンク\nお好きな1杯サービス"; if("別邸".equals(r))return"対象ドリンク1杯 ＋\n季節の一品サービス"; return"今月の会員限定サービス"; }
    private Bitmap makeQr(String value,int size){ try{ BitMatrix m=new MultiFormatWriter().encode(value,BarcodeFormat.QR_CODE,size,size); int[] px=new int[size*size]; for(int y=0;y<size;y++){int off=y*size;for(int x=0;x<size;x++)px[off+x]=m.get(x,y)?Color.BLACK:Color.WHITE;} Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.RGB_565);b.setPixels(px,0,size,0,0,size,size);return b;}catch(Exception e){return null;} }

    private LinearLayout quickBox(String title,String sub,View.OnClickListener l){ LinearLayout b=gradientCard(new int[]{CARD2,CARD},16,DIV); b.setPadding(dp(14),dp(13),dp(14),dp(13)); b.addView(text(title,15,GOLD2,true)); b.addView(text(sub,10,MUTED,false),top(dp(3))); b.setOnClickListener(l); return b; }
    private LinearLayout.LayoutParams weightedGap(boolean left){ LinearLayout.LayoutParams p=weighted(); if(left)p.rightMargin=dp(4); else p.leftMargin=dp(4); return p; }
    private LinearLayout section(String jp,String en){ LinearLayout b=new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL); TextView e=text(en,8,Color.rgb(149,125,80),true); e.setLetterSpacing(.15f); b.addView(e); b.addView(text(jp,18,GOLD2,true),top(dp(2))); return b; }
    private LinearLayout pageTitle(String jp,String en){ LinearLayout b=new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL); TextView e=text(en,9,GOLD,true); e.setLetterSpacing(.15f); b.addView(e); b.addView(text(jp,24,TEXT,true),top(dp(2))); return b; }
    private LinearLayout stat(String v,String l,boolean gold){ LinearLayout b=new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL); b.setGravity(Gravity.CENTER); TextView vv=text(v,21,gold?GOLD2:TEXT,true); vv.setGravity(Gravity.CENTER); b.addView(vv); TextView ll=text(l,10,MUTED,false); ll.setGravity(Gravity.CENTER); b.addView(ll,top(dp(3))); return b; }
    private TextView badge(String s){ TextView v=text(s,13,Color.rgb(34,26,17),true); v.setGravity(Gravity.CENTER); v.setPadding(dp(12),dp(5),dp(12),dp(5)); v.setBackground(gradient(new int[]{GOLD2,GOLD},18,0)); return v; }
    private Button navButton(String s){ Button b=new Button(this); b.setText(s); b.setTextSize(10); b.setAllCaps(false); b.setTextColor(MUTED); b.setBackgroundColor(Color.TRANSPARENT); b.setPadding(0,dp(6),0,dp(6)); b.setStateListAnimator(null); return b; }
    private Button miniButton(String s){ Button b=new Button(this); b.setText(s); b.setTextSize(10); b.setAllCaps(false); b.setTextColor(GOLD2); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setBackground(outline(Color.rgb(28,23,19),GOLD,11)); b.setStateListAnimator(null); return b; }
    private void select(Button s){ Button[] a={navHome,navMember,navCoupon,navStore}; for(Button b:a){b.setTextColor(b==s?GOLD2:MUTED);b.setTypeface(Typeface.DEFAULT,b==s?Typeface.BOLD:Typeface.NORMAL);} }
    private TextView text(String s,float z,int c,boolean bold){ TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setLineSpacing(0,1.12f);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v; }
    private LinearLayout card(int color,int radius){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setBackground(roundRect(color,radius));return b;}
    private LinearLayout gradientCard(int[] colors,int radius,int stroke){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setBackground(gradient(colors,radius,stroke));return b;}
    private GradientDrawable gradient(int[] colors,int radius,int stroke){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,colors);if(radius>0)g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}
    private GradientDrawable roundRect(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable outline(int color,int stroke,int radius){GradientDrawable g=roundRect(color,radius);g.setStroke(dp(1),stroke);return g;}
    private void addDivider(LinearLayout p,int m){View v=new View(this);v.setBackgroundColor(DIV);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(1));lp.topMargin=m;p.addView(v,lp);}
    private LinearLayout.LayoutParams matchWrap(){return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);}
    private LinearLayout.LayoutParams top(int m){LinearLayout.LayoutParams p=matchWrap();p.topMargin=m;return p;}
    private LinearLayout.LayoutParams weighted(){return new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    private int month(){return Calendar.getInstance().get(Calendar.MONTH)+1;}
    private static String fmt(int v){return String.format("%,d",v);}
    private static class Rank{final String name,nextText;final int value,max;Rank(String n,String t,int v,int m){name=n;nextText=t;value=Math.max(0,v);max=Math.max(1,m);}}
}
