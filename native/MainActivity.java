package __PACKAGE__;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(NotificationReaderPlugin.class);
        // Android 16 always draws apps edge to edge. Capacitor 8 asks the app to switch this on itself.
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
    }
}
