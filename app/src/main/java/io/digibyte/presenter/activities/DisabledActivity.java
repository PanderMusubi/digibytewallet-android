package io.digibyte.presenter.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

import io.digibyte.R;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.animation.SpringAnimator;
import io.digibyte.tools.security.AuthManager;


public class DisabledActivity extends BRActivity {
    private TextView untilLabel;
    private TextView disabled;
    private ConstraintLayout layout;
    private Button resetButton;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disabled);
        untilLabel = findViewById(R.id.until_label);
        layout = findViewById(R.id.layout);
        disabled = findViewById(R.id.disabled);
        resetButton = findViewById(R.id.reset_button);
        resetButton.setEnabled(false);
        resetButton.setOnClickListener(v -> {
            InputWordsActivity.open(DisabledActivity.this, InputWordsActivity.Type.RESET_PIN);
        });
        layout.setOnClickListener(v -> refresh());
        untilLabel.setText("");
    }

    private void refresh() {
        if (AuthManager.getInstance().isWalletDisabled(DisabledActivity.this)) {
            SpringAnimator.failShakeAnimation(DisabledActivity.this, disabled);
        } else {
            BRAnimator.startBreadActivity(DisabledActivity.this, true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        long disabledUntil = AuthManager.getInstance().disabledUntil(this);
        long disabledTime = disabledUntil - System.currentTimeMillis();
        int seconds = (int) disabledTime / 1000;
        timer = new CountDownTimer(seconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long durationSeconds = (millisUntilFinished / 1000);
                untilLabel.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", durationSeconds / 3600,
                        (durationSeconds % 3600) / 60, (durationSeconds % 60)));
            }

            @Override
            public void onFinish() {
                new Handler().postDelayed(() -> refresh(), 2000);
                long durationSeconds = 0;
                resetButton.setEnabled(true);
                untilLabel.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", durationSeconds / 3600,
                        (durationSeconds % 3600) / 60, (durationSeconds % 60)));
            }
        }.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.cancel();

    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}