package jp.hamasho.bettei;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);
        root.setBackgroundColor(Color.rgb(23, 19, 15));

        TextView title = new TextView(this);
        title.setText("濱匠別邸");
        title.setTextColor(Color.rgb(214, 176, 96));
        title.setTextSize(32);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(this);
        subtitle.setText("Android 起動確認版\n\nこの画面が表示されれば、APKの起動は成功です。");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(18);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = 36;
        root.addView(subtitle, subParams);

        TextView note = new TextView(this);
        note.setText("次の版で 会員証・ポイント・月1クーポン・QR・店舗管理 を載せます。");
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(15);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = 48;
        root.addView(note, noteParams);

        setContentView(root);
    }
}
