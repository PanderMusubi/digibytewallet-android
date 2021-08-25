package io.digibyte.presenter.activities.util;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import io.digibyte.R;
import io.digibyte.presenter.activities.BasePinActivity;
import io.digibyte.presenter.activities.DisabledActivity;
import io.digibyte.presenter.activities.InputWordsActivity;
import io.digibyte.tools.animation.BRDialog;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.threads.BRExecutor;
import io.digibyte.tools.util.BRCurrency;
import io.digibyte.tools.util.BRExchange;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 4/27/17.
 * Copyright (c) 2017 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class ActivityUTILS {

    private static final String TAG = ActivityUTILS.class.getName();
    private static Handler handler = new Handler(Looper.getMainLooper());

    //return true if the app does need to show the disabled wallet screen
    public static boolean isAppSafe(Activity app) {
        return app instanceof BasePinActivity || app instanceof InputWordsActivity;
    }

    public static void showWalletDisabled(Activity app) {
        Intent intent = new Intent(app, DisabledActivity.class);
        app.startActivity(intent);
        app.overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
        Log.e(TAG, "showWalletDisabled: " + app.getClass().getName());

    }

    public static boolean isMainThread() {
        boolean isMain = Looper.myLooper() == Looper.getMainLooper();
        if (isMain) {
            Log.e(TAG, "IS MAIN UI THREAD!");
        }
        return isMain;
    }

    public static void updateDigibyteDollarValues(Context context, TextView primary,
            TextView secondary) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            final String iso = BRSharedPrefs.getIso(context);

            //current amount in satoshis
            final BigDecimal amount = new BigDecimal(
                    BRSharedPrefs.getCatchedBalance(context));

            //amount in BTC units
            final BigDecimal btcAmount = BRExchange.getBitcoinForSatoshis(context, amount);
            float currentBtc = getCurrentAmount(primary).floatValue();
            primary.setTag(btcAmount);

            //amount in currency units
            final BigDecimal curAmount = BRExchange.getAmountFromSatoshis(context, iso, amount);
            float currentAmount = getCurrentAmount(secondary).floatValue();
            secondary.setTag(curAmount);

            float[] fiatIntervals = getIntervals(currentAmount, curAmount.floatValue());
            float[] btcIntervals = getIntervals(currentBtc, btcAmount.floatValue());

            handler.post(() -> {
                if (fiatIntervals.length == 1 || btcIntervals.length == 1) {
                    secondary.setText(BRCurrency.getFormattedCurrencyString(
                            context, iso, new BigDecimal(fiatIntervals[0])));
                    primary.setText(BRCurrency.getFormattedCurrencyString(
                            context, "DGB", new BigDecimal(btcIntervals[0])));
                    return;
                }

                ValueAnimator btcAnimator = ValueAnimator.ofFloat(btcIntervals);
                btcAnimator.addUpdateListener(animation -> {
                    float value = (float) animation.getAnimatedValue();
                    primary.setText(BRCurrency.getFormattedCurrencyString(
                            context, "DGB", new BigDecimal(value)));
                });
                btcAnimator.setDuration(1500);
                btcAnimator.setInterpolator(new DecelerateInterpolator());
                btcAnimator.start();

                ValueAnimator fiatAnimator = ValueAnimator.ofFloat(fiatIntervals);
                fiatAnimator.addUpdateListener(animation -> {
                    float value = (float) animation.getAnimatedValue();
                    secondary.setText(BRCurrency.getFormattedCurrencyString(
                            context, iso, new BigDecimal(value)));
                });
                fiatAnimator.setDuration(1500);
                fiatAnimator.setInterpolator(new DecelerateInterpolator());
                fiatAnimator.start();
            });

        });
    }

    private static float[] getIntervals(float start, float finish) {
        if (finish < start || finish == start) {
            return new float[]{finish};
        }
        float[] intervals = new float[40];
        for (int i = 1; i <= 40; i++) {
            if (i == 40) {
                intervals[39] = finish;
            } else {
                intervals[i - 1] = start + (finish - start) * ((float) i * .025f);
            }
        }
        return intervals;
    }

    private static BigDecimal getCurrentAmount(TextView amount) {
        return amount.getTag() != null ? ((BigDecimal) amount.getTag()) : BigDecimal.ZERO;
    }

    public static void showJailbrokenDialog(AppCompatActivity context) {
        BRDialog.showCustomDialog(context, context.getString(R.string.JailbreakWarnings_title),
                context.getString(R.string.JailbreakWarnings_messageWithoutBalance),
                context.getString(R.string.JailbreakWarnings_close), null,
                null, null, brDialogView -> {
                    if (context != null) {
                        context.finishAffinity();
                    }
                }, 0);
    }

    public static boolean isvm() {

        StringBuilder deviceInfo = new StringBuilder();
        deviceInfo.append("Build.PRODUCT " + Build.PRODUCT + "\n");
        deviceInfo.append("Build.FINGERPRINT " + Build.FINGERPRINT + "\n");
        deviceInfo.append("Build.MANUFACTURER " + Build.MANUFACTURER + "\n");
        deviceInfo.append("Build.MODEL " + Build.MODEL + "\n");
        deviceInfo.append("Build.BRAND " + Build.BRAND + "\n");
        deviceInfo.append("Build.DEVICE " + Build.DEVICE + "\n");
        String info = deviceInfo.toString();

        Log.i("LOB", info);

        Boolean isvm = false;
        if (
                "google_sdk".equals(Build.PRODUCT) ||
                        "sdk_google_phone_x86".equals(Build.PRODUCT) ||
                        "sdk".equals(Build.PRODUCT) ||
                        "sdk_x86".equals(Build.PRODUCT) ||
                        "vbox86p".equals(Build.PRODUCT) ||
                        Build.FINGERPRINT.contains("generic") ||
                        Build.MANUFACTURER.contains("Genymotion") ||
                        Build.MODEL.contains("Emulator") ||
                        Build.MODEL.contains("Android SDK built for x86")
                ) {
            isvm = true;
        }

        if (Build.BRAND.contains("generic") && Build.DEVICE.contains("generic")) {
            isvm = true;
        }

        return isvm;
    }

    public static char getDecimalSeparator() {
        NumberFormat nf = NumberFormat.getInstance();
        if (nf instanceof DecimalFormat) {
            DecimalFormatSymbols sym = ((DecimalFormat) nf).getDecimalFormatSymbols();
            return sym.getDecimalSeparator();
        }
        return '.';
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Locale getCurrentLocale(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return c.getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            return c.getResources().getConfiguration().locale;
        }
    }
}