package io.digibyte.presenter.activities.settings;

import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import io.digibyte.R;
import io.digibyte.databinding.ActivityRestoreBinding;
import io.digibyte.presenter.activities.InputWordsActivity;
import io.digibyte.presenter.activities.callbacks.ActivityWipeCallback;
import io.digibyte.presenter.activities.util.BRActivity;


public class WipeActivity extends BRActivity {

    private ActivityWipeCallback callback = () -> InputWordsActivity.open(WipeActivity.this,
            InputWordsActivity.Type.WIPE);

    public static void show(AppCompatActivity activity) {
        Intent intent = new Intent(activity, WipeActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRestoreBinding binding = DataBindingUtil.setContentView(this,
                R.layout.activity_restore);
        binding.setCallback(callback);
        setupToolbar();
        setToolbarTitle(R.string.Settings_wipe);
    }
}