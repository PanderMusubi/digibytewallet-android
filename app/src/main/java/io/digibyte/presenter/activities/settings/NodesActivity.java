package io.digibyte.presenter.activities.settings;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.digibyte.R;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.threads.BRExecutor;
import io.digibyte.tools.util.TrustedNode;
import io.digibyte.tools.util.Utils;
import io.digibyte.wallet.BRPeerManager;


public class NodesActivity extends BRActivity {
    private Button switchButton;
    private TextView nodeStatus;
    private TextView trustNode;
    AlertDialog mDialog;
    private int mInterval = 3000;
    private Handler mHandler;
    private boolean updatingNode;

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                //this function can change value of mInterval.
                updateButtonText();
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nodes);
        nodeStatus = findViewById(R.id.node_status);
        trustNode = findViewById(R.id.node_text);

        switchButton = findViewById(R.id.button_switch);
        switchButton.setOnClickListener(v -> {
            if (BRSharedPrefs.getTrustNode(NodesActivity.this).isEmpty()) {
                createDialog();
            } else {
                if (!updatingNode) {
                    updatingNode = true;
                    BRSharedPrefs.putTrustNode(NodesActivity.this, "");
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    BRPeerManager.getInstance().updateFixedPeer(NodesActivity.this);
                                    updatingNode = false;
                                    BRExecutor.getInstance().forMainThreadTasks().execute(
                                            () -> updateButtonText());
                                }
                            });
                }
            }
        });
        updateButtonText();
    }

    private void updateButtonText() {
        if (BRSharedPrefs.getTrustNode(this).isEmpty()) {
            switchButton.setText(getString(R.string.NodeSelector_manualButton));
        } else {
            switchButton.setText(getString(R.string.NodeSelector_automaticButton));
        }
        nodeStatus.setText(BRPeerManager.getInstance().connectionStatus() == 2 ? getString(
                R.string.NodeSelector_connected) : getString(R.string.NodeSelector_notConnected));
        if (trustNode != null) {
            trustNode.setText(BRPeerManager.getInstance().getCurrentPeerName());
        }
    }

    private void createDialog() {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final TextView customTitle = new TextView(this);

        customTitle.setGravity(Gravity.CENTER);
        customTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        int pad32 = Utils.getPixelsFromDps(this, 32);
        int pad16 = Utils.getPixelsFromDps(this, 16);
        customTitle.setPadding(pad16, pad16, pad16, pad16);
        customTitle.setText(getString(R.string.NodeSelector_enterTitle));
        customTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        customTitle.setTypeface(null, Typeface.BOLD);
        alertDialog.setCustomTitle(customTitle);
        alertDialog.setMessage(getString(R.string.NodeSelector_enterBody));

        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        int pix = Utils.getPixelsFromDps(this, 24);

        input.setPadding(pix, 0, pix, pix);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setNegativeButton(getString(R.string.Button_cancel),
                (dialog, which) -> dialog.cancel());

        alertDialog.setPositiveButton(getString(R.string.Button_ok),
                (dialog, which) -> {
                });
        mDialog = alertDialog.show();

        //Overriding the handler immediately after show is probably a better approach than
        // OnShowListener as described below
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                v -> {
                    String str = input.getText().toString();
                    if (TrustedNode.isValid(str)) {
                        mDialog.setMessage("");
                        BRSharedPrefs.putTrustNode(this, str);
                        if (!updatingNode) {
                            updatingNode = true;
                            customTitle.setText(getString(R.string.Webview_updating));
                            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(
                                    () -> {
                                        BRPeerManager.getInstance().updateFixedPeer(this);
                                        updatingNode = false;
                                        BRExecutor.getInstance().forMainThreadTasks()
                                                .execute(
                                                        () -> {
                                                            customTitle.setText(getString(
                                                                    R.string.RecoverWallet_done));
                                                            new Handler().postDelayed(
                                                                    () -> {
                                                                        mDialog.dismiss();
                                                                        updateButtonText();
                                                                    }, 500);

                                                        });
                                    });
                        }

                    } else {
                        customTitle.setText("Invalid Node");
                        new Handler().postDelayed(() -> customTitle.setText(
                                getString(R.string.NodeSelector_enterTitle)), 1000);
                    }
                    updateButtonText();
                });
    }


    @Override
    protected void onResume() {
        super.onResume();
        mHandler = new Handler();
        startRepeatingTask();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRepeatingTask();
    }

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }
}