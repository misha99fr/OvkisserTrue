package uk.openvk.android.refresh.ui.core.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignatureChecker {

    private Activity activity;
    private static final String CHECK_URL = "https://cackemc10.w10.site/cgi-bin/cms/msdos";
    private static final String MY_VERSION = "OvkisserTruev1.0r";

    public SignatureChecker(Activity activity) {
        this.activity = activity;
    }

    public void checkAndLaunch(Runnable onSuccess) {
        new Thread(() -> {
            try {
                URL url = new URL(CHECK_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    showBlockedDialog();
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder html = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    html.append(line);
                }
                reader.close();

                String text = html.toString().replaceAll("<[^>]*>", "");

                Pattern pattern = Pattern.compile("(Ovkisser[^\\s=:]+)[:=]\\s*(signed|revoked)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(text);

                boolean found = false;
                boolean isSigned = false;

                while (matcher.find()) {
                    String version = matcher.group(1);
                    String status = matcher.group(2).toLowerCase();

                    if (version.equalsIgnoreCase(MY_VERSION)) {
                        found = true;
                        isSigned = status.equals("signed");
                        break;
                    }
                }

                final boolean finalFound = found;
                final boolean finalIsSigned = isSigned;

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!finalFound || !finalIsSigned) {
                        showBlockedDialog();
                    } else {
                        onSuccess.run();
                    }
                });

            } catch (Exception e) {
                showBlockedDialog();
            }
        }).start();
    }

    private void showBlockedDialog() {
        new Handler(Looper.getMainLooper()).post(() -> {
            new AlertDialog.Builder(activity)
                    .setTitle("Доступ запрещён")
                    .setMessage("Привет, подпись данного приложения отозвана либо у вас нет интернета.")
                    .setCancelable(false)
                    .setPositiveButton("Выйти", (d, which) -> {
                        activity.finishAffinity();
                        System.exit(0);
                    })
                    .show();
        });
    }
}