package com.bomo.vivohfr;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * vivo 高刷全开 LSPosed 模块入口。
 *
 * <p>用途：让 OriginOS "使用高刷新率的应用" 列表对所有应用（含新装）默认按高刷执行，
 * 且用户在该列表里把某应用关掉（锁 60）也不生效。</p>
 *
 * <p>机制（vivo V2419A / OriginOS16 实机反编译 + dump 定位）：每应用锁帧的真实执行点是
 * {@code WindowRequestManager.getXmlSettings(sEffectedSetting)} 返回的 XML 设置段。固定模式
 * (promotion disabled) 下框架取 {@code DisablePromotionSettings} 段，被关的应用在该段里
 * {@code reqFps=60}；而 {@code FPS_XML_SETTINGS}（"Settings:"）段里同一应用是 {@code reqFps=120}。
 * 本模块把 getXmlSettings 强制返回 {@code mFpsXmlSettings}，从而绕过用户开关的 60 锁，
 * 同时保留厂商在 FPS_XML_SETTINGS 段里的基础封顶（如视频类应用仍按其自身 reqFps）。</p>
 *
 * <p>作用域：system_server。改后需重启框架才生效。</p>
 *
 * @author bomo
 */
@SuppressLint({"PrivateApi", "BlockedPrivateApi"})
public class MainModule extends XposedModule {

    private static final String TAG = "VivoHfrAlwaysOn";
    private static final String TARGET_CLASS = "com.vivo.services.rrm.WindowRequestManager";
    private static final String GET_XML_SETTINGS = "getXmlSettings";
    private static final String FIELD_FPS_XML = "mFpsXmlSettings";

    @Override
    public void onModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
        Log.i(TAG, "onModuleLoaded process=" + param.getProcessName() + " systemServer=" + param.isSystemServer());
    }

    @Override
    public void onSystemServerStarting(@NonNull XposedModuleInterface.SystemServerStartingParam param) {
        Log.i(TAG, "onSystemServerStarting entered");
        final ClassLoader loader = param.getClassLoader();
        try {
            final Class<?> clazz = loader.loadClass(TARGET_CLASS);
            // 取静态字段 mFpsXmlSettings 实例（"Settings:" 段，含各应用的 reqFps=120 值）。
            final Field field = clazz.getDeclaredField(FIELD_FPS_XML);
            field.setAccessible(true);
            final Object fpsXmlSettings = field.get(null);
            if (fpsXmlSettings == null) {
                Log.w(TAG, "mFpsXmlSettings is null, skip");
                return;
            }
            final Method method = clazz.getDeclaredMethod(GET_XML_SETTINGS, String.class);
            hook(method)
                    .setPriority(XposedInterface.PRIORITY_HIGHEST)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(new ForceHighHooker(fpsXmlSettings));
            Log.i(TAG, "hook installed on " + TARGET_CLASS + "#" + GET_XML_SETTINGS);
        } catch (Throwable t) {
            // 非 vivo 设备或框架变更时静默跳过，不影响系统。
            Log.w(TAG, "hook install skipped: " + t);
        }
    }

    /**
     * 强制 getXmlSettings 始终返回 mFpsXmlSettings（"Settings:" 段），
     * 使固定模式下也忽略 DisablePromotionSettings 段里的 60 锁。
     */
    private static final class ForceHighHooker implements XposedInterface.Hooker {
        private final Object mFpsXml;
        private int mLogged = 0;

        ForceHighHooker(Object fpsXml) {
            this.mFpsXml = fpsXml;
        }

        @Override
        public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
            if (mLogged < 3) {
                Log.i(TAG, "getXmlSettings forced to FPS_XML_SETTINGS (arg=" + chain.getArg(0) + ")");
                mLogged++;
            }
            return mFpsXml;
        }
    }
}
