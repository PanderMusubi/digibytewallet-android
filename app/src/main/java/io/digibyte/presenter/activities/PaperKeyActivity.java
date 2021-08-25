package io.digibyte.presenter.activities;

import android.content.Context;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import android.util.SparseArray;
import android.view.WindowManager;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.digibyte.R;
import io.digibyte.databinding.ActivityPaperKeyBinding;
import io.digibyte.presenter.activities.callbacks.ActivityPaperKeyCallback;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.presenter.fragments.FragmentPhraseWord;
import io.digibyte.tools.animation.BRDialog;
import io.digibyte.tools.manager.BRReportsManager;
import io.digibyte.tools.security.PostAuth;


public class PaperKeyActivity extends BRActivity {
    public static final String CLEAN_PHRASE = "PaperKeyActivity:CleanPhrase";
    private SparseArray<String> wordMap;
    private ActivityPaperKeyBinding binding;

    private ActivityPaperKeyCallback callback = new ActivityPaperKeyCallback() {
        @Override
        public void onNextButtonClick() {
            updateWordView(true);
        }

        @Override
        public void onPreviousButtonClick() {
            updateWordView(false);
        }
    };

    public static void show(AppCompatActivity activity, String phrase) {
        Intent intent = new Intent(activity, PaperKeyActivity.class);
        intent.putExtra(CLEAN_PHRASE, phrase);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_paper_key);
        binding.setCallback(callback);
        setupToolbar();
        setToolbarTitle(R.string.SecurityCenter_paperKeyTitle);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        binding.setPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {

            }

            public void onPageScrolled(int position, float positionOffset,
                    int positionOffsetPixels) {

            }

            public void onPageSelected(int position) {
                if (position == 0) {
                    setButtonEnabled(false);
                } else {
                    setButtonEnabled(true);
                }
                updateItemIndexText();
            }
        });

        String cleanPhrase = getPhrase();
        wordMap = new SparseArray<>();

        List<String> wordArray = Arrays.asList(cleanPhrase.split(" "));

        if (cleanPhrase.charAt(cleanPhrase.length() - 1) == '\0') {
            BRDialog.showCustomDialog(this, getString(R.string.JailbreakWarnings_title),
                    getString(R.string.Alert_keystore_generic_android),
                    getString(R.string.Button_ok), null,
                    brDialogView -> brDialogView.dismissWithAnimation(), null, null, 0);
            BRReportsManager.reportBug(new IllegalArgumentException(
                    "Paper Key error, please contact support at breadwallet.com: "
                            + wordArray.size()), true);
        } else {
            if (wordArray.size() != 12) {
                BRReportsManager.reportBug(new IllegalArgumentException(
                        "Wrong number of paper keys: " + wordArray.size() + ", lang: "
                                + Locale.getDefault().getLanguage()), true);
            }
            WordPagerAdapter adapter = new WordPagerAdapter(getSupportFragmentManager());
            adapter.setWords(wordArray);
            binding.setAdapter(adapter);
            for (int i = 0; i < wordArray.size(); i++) {
                wordMap.append(i, wordArray.get(i));
            }
            updateItemIndexText();
        }
    }

    private String getPhrase() {
        return getIntent().getStringExtra(CLEAN_PHRASE);
    }

    private void updateWordView(boolean isNext) {
        int currentIndex = binding.phraseWordsPager.getCurrentItem();
        if (isNext) {
            setButtonEnabled(true);
            if (currentIndex >= 11) {
                PostAuth.instance.onPhraseProveAuth(this, false);
            } else {
                binding.phraseWordsPager.setCurrentItem(currentIndex + 1);
            }
        } else {
            if (currentIndex <= 1) {
                binding.phraseWordsPager.setCurrentItem(currentIndex - 1);
                setButtonEnabled(false);
            } else {
                binding.phraseWordsPager.setCurrentItem(currentIndex - 1);
            }
        }
    }

    private void setButtonEnabled(boolean b) {
        binding.buttonPrevious.setTextColor(ContextCompat.getColor(PaperKeyActivity.this, b ? R.color.white : R.color.light_gray));
    }

    private void updateItemIndexText() {
        int wordNumber = binding.phraseWordsPager.getCurrentItem() + 1;
        int totalWords = wordMap.size();
        binding.itemIndexText.setText(wordNumber + " / " + totalWords);
    }

    public class WordPagerAdapter extends FragmentStatePagerAdapter {

        private List<String> words;

        public WordPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setWords(List<String> words) {
            this.words = words;
        }

        @Override
        public Fragment getItem(int pos) {
            return FragmentPhraseWord.newInstance(words.get(pos));
        }

        @Override
        public int getCount() {
            return words == null ? 0 : words.size();
        }
    }
}