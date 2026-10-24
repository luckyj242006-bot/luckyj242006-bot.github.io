// Run once after `npx cap add android`. Safe to run again.
// Adds the notification permissions, the payment notification reader and a notification icon.
import fs from 'node:fs';
import path from 'node:path';

const root = 'android/app/src/main';
const log = (m) => console.log('[patch-android] ' + m);

const config = JSON.parse(fs.readFileSync('capacitor.config.json', 'utf8'));
const appId = config.appId;
const pkgPath = appId.split('.').join('/');

// 1. Manifest: notification permission (Android 13 and newer) and the notification listener service
const manifestPath = path.join(root, 'AndroidManifest.xml');
if (fs.existsSync(manifestPath)) {
  let manifest = fs.readFileSync(manifestPath, 'utf8');
  let changed = false;

  if (!manifest.includes('POST_NOTIFICATIONS')) {
    manifest = manifest.replace(
      '</manifest>',
      '    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n</manifest>'
    );
    changed = true;
    log('added POST_NOTIFICATIONS permission');
  }

  if (!manifest.includes('KharchaListener')) {
    const service = `
        <service
            android:name=".KharchaListener"
            android:exported="true"
            android:label="Kharcha"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>
    `;
    manifest = manifest.replace('</application>', service + '</application>');
    changed = true;
    log('added notification listener service');
  }

  // Automatic backup to the user's own Google account (Android Auto Backup)
  if (!manifest.includes('dataExtractionRules')) {
    manifest = manifest.replace(/\s+android:allowBackup="[^"]*"/, '');
    manifest = manifest.replace(/<application\b/, '<application\n        android:allowBackup="true"\n        android:fullBackupContent="@xml/kharcha_backup_rules"\n        android:dataExtractionRules="@xml/kharcha_data_extraction_rules"');
    changed = true;
    log('turned on Google account backup');
  }

  if (changed) fs.writeFileSync(manifestPath, manifest);
} else {
  log('AndroidManifest.xml not found, skipped manifest changes');
}

// 2. Java sources: MainActivity registers the plugin, the other two read payment notifications
const javaDir = path.join(root, 'java', pkgPath);
if (fs.existsSync(path.join(root, 'java'))) {
  fs.mkdirSync(javaDir, { recursive: true });
  for (const name of ['MainActivity.java', 'KharchaListener.java', 'NotificationReaderPlugin.java']) {
    const src = fs.readFileSync(path.join('native', name), 'utf8').replaceAll('__PACKAGE__', appId);
    fs.writeFileSync(path.join(javaDir, name), src);
    log('wrote ' + name);
  }
} else {
  log('java folder not found, skipped Java files');
}

// 3. Small white icon for notifications
const drawableDir = path.join(root, 'res/drawable');
fs.mkdirSync(drawableDir, { recursive: true });
fs.writeFileSync(
  path.join(drawableDir, 'ic_stat_kharcha.xml'),
  `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M6,4h12v2.2h-3.4c0.5,0.6 0.9,1.4 1.1,2.3H18V10.7h-2.2C15.3,13.3 13.1,15 10,15H9.2l7.3,5h-3L6,15v-2.1h4c1.7,0 2.9,-0.8 3.3,-2.2H6V8.5h7.2C12.8,7.4 11.8,6.8 10.5,6.8H6z" />
</vector>
`
);
log('wrote notification icon');

// 4. Backup rules: back up the app data, but not the private queue of payment notifications
const xmlDir = path.join(root, 'res/xml');
fs.mkdirSync(xmlDir, { recursive: true });
fs.writeFileSync(
  path.join(xmlDir, 'kharcha_backup_rules.xml'),
  `<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <include domain="root" path="app_webview" />
    <include domain="sharedpref" path="." />
    <exclude domain="sharedpref" path="kharcha_inbox.xml" />
</full-backup-content>
`
);
const rules = (tag) => `    <${tag}>
        <include domain="root" path="app_webview" />
        <include domain="sharedpref" path="." />
        <exclude domain="sharedpref" path="kharcha_inbox.xml" />
    </${tag}>`;
fs.writeFileSync(
  path.join(xmlDir, 'kharcha_data_extraction_rules.xml'),
  `<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
${rules('cloud-backup')}
${rules('device-transfer')}
</data-extraction-rules>
`
);
log('wrote backup rules');
