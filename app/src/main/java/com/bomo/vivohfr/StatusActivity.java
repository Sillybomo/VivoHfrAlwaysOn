package com.bomo.vivohfr;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 最小状态页：LSPosed Manager 只列带 launcher activity 的模块（实机验证坑），
 * 同时给用户提供"模块已安装"的直观确认与使用说明。
 *
 * @author bomo
 */
public class StatusActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 96, 48, 48);

        TextView title = new TextView(this);
        title.setText("vivo高刷全开");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView body = new TextView(this);
        body.setText("状态：已安装。\n\n"
                + "请在 LSPosed 管理器中启用本模块（作用域已静态声明为系统框架），"
                + "然后重启手机生效。\n\n"
                + "生效后所有应用（含新装）强制使用高刷新率。\n\n"
                + "可选排除名单（root 创建，每行一个包名，10 秒内热生效）：\n"
                + "/data/system/vivohfr_exclude.txt");
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        body.setTextColor(Color.DKGRAY);
        root.addView(body);

        setContentView(root);
    }
}
