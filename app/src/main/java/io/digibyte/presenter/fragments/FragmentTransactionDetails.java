package io.digibyte.presenter.fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import io.digibyte.R;
import io.digibyte.databinding.FragmentTransactionDetailsBinding;
import io.digibyte.presenter.fragments.interfaces.OnBackPressListener;
import io.digibyte.tools.adapter.TransactionPagerAdapter;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.list.items.ListItemTransactionData;
import io.digibyte.tools.util.Utils;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentTransactionDetails extends Fragment implements OnBackPressListener {

    private static final String TRANSACTIONS_ARRAY = "FragmentTransactionDetails:TransactionsArray";
    private static final String TRANSACTION_NUMBER = "FragmentTransactionDetails:TransactionNumber";
    private FragmentTransactionDetailsBinding binding;

    public static void show(AppCompatActivity context, ArrayList<ListItemTransactionData> items,
            int transactionNumber) {
        FragmentTransactionDetails fragmentTransactionDetails = new FragmentTransactionDetails();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(TRANSACTIONS_ARRAY, items);
        bundle.putInt(TRANSACTION_NUMBER, transactionNumber);
        fragmentTransactionDetails.setArguments(bundle);
        FragmentTransaction fragmentTransaction =
                context.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom,
                R.animator.from_bottom, R.animator.to_bottom);
        fragmentTransaction.add(android.R.id.content, fragmentTransactionDetails,
                FragmentTransactionDetails.class.getName());
        fragmentTransaction.addToBackStack(FragmentTransactionDetails.class.getName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentTransactionDetailsBinding.inflate(inflater);
        try {
            binding.setAdapter(new TransactionPagerAdapter(getChildFragmentManager(),
                    getArguments().getParcelableArrayList(TRANSACTIONS_ARRAY)));
        } catch (Throwable e) {
            fadeOutRemove();
        }
        binding.setTransactionNumber(getArguments().getInt(TRANSACTION_NUMBER, 0));
        binding.txListPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                    int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Utils.hideKeyboard(getActivity());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.backgroundLayout, false,
                null);
        colorFade.setStartDelay(350);
        colorFade.setDuration(500);
        colorFade.start();
    }

    private void fadeOutRemove() {
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.backgroundLayout, true,
                () -> {
                    Animator animator = AnimatorInflater.loadAnimator(getContext(),
                            R.animator.to_bottom);
                    animator.setTarget(binding.getRoot());
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (getActivity() != null) {
                                try {
                                    getActivity().getSupportFragmentManager().popBackStack();
                                    Utils.hideKeyboard(getActivity());
                                } catch (IllegalStateException e) {
                                    //Edge cases regarding activity destruction
                                } catch (NullPointerException e) {
                                    //Null activity references or a null fragment manager
                                }
                            }
                        }
                    });
                    animator.start();
                });
        colorFade.start();

    }

    public void onBackPressed() {
        fadeOutRemove();
    }
}