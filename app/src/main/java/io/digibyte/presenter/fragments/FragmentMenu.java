package io.digibyte.presenter.fragments;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.digibyte.R;
import io.digibyte.databinding.FragmentMenuBinding;
import io.digibyte.presenter.entities.BRMenuItem;
import io.digibyte.presenter.fragments.interfaces.MenuDialogCallback;
import io.digibyte.presenter.fragments.interfaces.OnBackPressListener;
import io.digibyte.tools.animation.BRAnimator;

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

public class FragmentMenu extends Fragment implements OnBackPressListener {
    private FragmentMenuBinding binding;
    private Handler handler = new Handler(Looper.getMainLooper());

    private MenuDialogCallback mMenuDialogCallback = () -> fadeOutRemove(null, false, true);

    public static void show(AppCompatActivity activity) {
        FragmentMenu fragmentMenu = new FragmentMenu();
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom,
                R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, fragmentMenu,
                FragmentMenu.class.getName());
        transaction.addToBackStack(FragmentMenu.class.getName());
        transaction.commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentMenuBinding.inflate(inflater);
        binding.setCallback(mMenuDialogCallback);
        binding.recyclerView.setAdapter(new MenuListAdapter());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.background, false, null);
        colorFade.setStartDelay(350);
        colorFade.setDuration(500);
        colorFade.start();
    }

    private enum FragmentType {
        SEND, RECEIVE, SCAN
    }

    public class MenuListAdapter extends RecyclerView.Adapter<BRMenuItem> {

        @NonNull
        @Override
        public BRMenuItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new BRMenuItem(LayoutInflater.from(FragmentMenu.this.getContext()).inflate(
                    R.layout.menu_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull BRMenuItem holder, int position) {
            switch (position) {
                case 0:
                    holder.text.setText(R.string.Send_title);
                    holder.icon.setImageResource(R.drawable.menu_send);
                    holder.itemView.setOnClickListener(
                            v -> fadeOutRemove(FragmentType.SEND, false, false));
                    break;
                case 1:
                    holder.text.setText(R.string.Receive_title);
                    holder.icon.setImageResource(R.drawable.menu_receive);
                    holder.itemView.setOnClickListener(
                            v -> fadeOutRemove(FragmentType.RECEIVE, false, false));
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    private void fadeOutRemove(FragmentType fragmentType, boolean openLockScreen,
            boolean justClose) {
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.background, true,
                () -> {
                    final AppCompatActivity activity = (AppCompatActivity) getActivity();
                    remove();
                    if (justClose) {
                        return;
                    }
                    handler.postDelayed(() -> {
                        if (openLockScreen) {
                            BRAnimator.startBreadActivity(activity, true);
                        } else {
                            switch (fragmentType) {
                                case SEND:
                                    FragmentSend.show(activity, "");
                                    break;
                                case RECEIVE:
                                    FragmentReceive.show(activity, true);
                                    break;
                                case SCAN:
                                    BRAnimator.openScanner(getActivity());
                                    break;
                            }
                        }
                    }, 350);
                });
        colorFade.start();
    }

    private void remove() {
        if (getFragmentManager() == null) {
            return;
        }
        try {
            getFragmentManager().popBackStack();
        } catch (IllegalStateException e) {
            //Lifecycle race conditions
        }
    }

    @Override
    public void onBackPressed() {
        fadeOutRemove(null, false, true);
    }
}