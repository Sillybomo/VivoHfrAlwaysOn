package com.bomo.vivohfr;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 最小状态页：LSPosed Manager 只列带 launcher activity 的模块（实机验证坑），
 * 同时给用户提供"模块已安装 + 如何启用/回滚"的直观确认。
 *
 * <p>v1.0.1 修复：文案对齐最终 getXmlSettings 换段设计（v1.0 无排除名单功能，
 * 旧文案的 vivohfr_exclude.txt 系早期方案残留）；Android 16 edge-to-edge 下
 * 正文被状态栏遮挡，改为 ScrollView + insets 内边距。</p>
 *
 * @author bomo
 */
public class StatusActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        root.addView(text("vivo高刷全开 v1.0.1", 22, Color.BLACK, true));
        root.addView(text("状态：已安装", 15, Color.DKGRAY, false));
        root.addView(space(dp(16)));
        root.addView(text(
                "启用方法：\n"
                + "1. 打开 LSPosed 管理器 → 模块 → 启用本模块（作用域已内置为系统框架，自动勾选）\n"
                + "2. 重启手机（钩子在系统服务内，必须重启生效）",
                14, Color.DKGRAY, false));
        root.addView(space(dp(16)));
        root.addView(text(
                "生效表现：\n"
                + "设置「使用高刷新率的应用」列表不再影响任何应用，所有应用（含新装）按高刷执行；"
                + "厂商对视频等应用的兼容封顶保留。",
                14, Color.DKGRAY, false));
        root.addView(space(dp(16)));
        root.addView(text(
                "回滚：LSPosed 中禁用本模块后重启，零残留。\n"
                + "项目地址：github.com/Sillybomo/VivoHfrAlwaysOn",
                13, Color.GRAY, false));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        scroll.setFillViewport(true);
        setContentView(scroll);

        // Android 16 edge-to-edge：把系统栏 inset 转成内容 padding，防遮挡
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(
                    insets.getSystemWindowInsetLeft(),
                    insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),
                    insets.getSystemWindowInsetBottom());
            return insets;
        });
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private TextView text(String content, int sizeSp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(content);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        tv.setTextColor(color);
        if (bold) {
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return tv;
    }

    private android.view.View space(int px) {
        android.view.View v = new android.view.View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, px));
        return v;
    }
}
