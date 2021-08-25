package io.digibyte.presenter.activities.intro;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.digibyte.R;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.security.PostAuth;
import io.digibyte.wallet.BRWalletManager;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        PostAuth.instance.onCanaryCheck(this, false);
        BRWalletManager wallet = BRWalletManager.getInstance();
        if (wallet.noWallet(this)) {
            IntroActivity.open(this);
        } else {
            BRAnimator.startBreadActivity(this, true);
        }
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
