package __PACKAGE__;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Bridge between the web app and the notification listener. */
@CapacitorPlugin(name = "KharchaInbox")
public class NotificationReaderPlugin extends Plugin {

    private static volatile NotificationReaderPlugin instance;

    @Override
    public void load() {
        instance = this;
    }

    /** Tells the web app that a new message is waiting. Safe to call when the app is closed. */
    static void ping() {
        final NotificationReaderPlugin p = instance;
        if (p == null) return;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    p.notifyListeners("inbox", new JSObject());
                } catch (Throwable ignored) {
                    // The web view may be gone. The message stays queued.
                }
            }
        });
    }

    @PluginMethod
    public void isEnabled(PluginCall call) {
        boolean on = false;
        try {
            String flat = Settings.Secure.getString(getContext().getContentResolver(), "enabled_notification_listeners");
            on = flat != null && flat.contains(getContext().getPackageName() + "/");
        } catch (Throwable ignored) {
            // Treat any failure as off.
        }
        JSObject ret = new JSObject();
        ret.put("enabled", on);
        call.resolve(ret);
    }

    @PluginMethod
    public void openSettings(PluginCall call) {
        try {
            Intent i = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Throwable t) {
            call.reject("Could not open notification access settings");
        }
    }

    @PluginMethod
    public void openAppInfo(PluginCall call) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.fromParts("package", getContext().getPackageName(), null));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Throwable t) {
            call.reject("Could not open app info");
        }
    }

    @PluginMethod
    public void drain(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("json", KharchaListener.drainQueue(getContext()));
        call.resolve(ret);
    }
}
