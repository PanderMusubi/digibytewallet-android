package io.digibyte.presenter.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import io.digibyte.R;
import io.digibyte.databinding.ActivityQrCodeBinding;
import io.digibyte.presenter.activities.callbacks.ActivityQRCodeCallback;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.qrcode.QRUtils;

public class QRCodeActivity extends BRActivity {

    private static final String QR_IMAGE_URL = "QRCodeActivity:QrImageUrl";
    private ActivityQrCodeBinding binding;

    private ActivityQRCodeCallback callback = () -> supportFinishAfterTransition();

    public static void show(AppCompatActivity activity, View view, String qrUrl) {
        Intent intent = new Intent(activity, QRCodeActivity.class);
        intent.putExtra(QR_IMAGE_URL, qrUrl);
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(activity, view, "qr_image");
        activity.startActivity(intent, options.toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportPostponeEnterTransition();
        binding = DataBindingUtil.setContentView(this,
                R.layout.activity_qr_code);
        binding.setCallback(callback);
        populateQRImage();
        supportStartPostponedEnterTransition();
        new Handler().postDelayed(() -> {
            ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.background, false, null);
            colorFade.setStartDelay(350);
            colorFade.setDuration(500);
            colorFade.start();
        }, 250);
    }

    private void populateQRImage() {
        QRUtils.generateQR(this, getIntent().getStringExtra(QR_IMAGE_URL),  binding.qrImage);
    }
}
