package io.digibyte.presenter.activities.models;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import io.digibyte.BR;

public class InputWordsViewModel extends BaseObservable {

    private String word1;
    private String word2;
    private String word3;
    private String word4;
    private String word5;
    private String word6;
    private String word7;
    private String word8;
    private String word9;
    private String word10;
    private String word11;
    private String word12;

    @Bindable
    public String getWord1() {
        return word1;
    }

    public void setWord1(String word1) {
        this.word1 = word1;
        notifyPropertyChanged(BR.word1);
    }

    @Bindable
    public String getWord2() {
        return word2;
    }

    public void setWord2(String word2) {
        this.word2 = word2;
        notifyPropertyChanged(BR.word2);
    }

    @Bindable
    public String getWord3() {
        return word3;
    }

    public void setWord3(String word3) {
        this.word3 = word3;
        notifyPropertyChanged(BR.word3);
    }

    @Bindable
    public String getWord4() {
        return word4;
    }

    public void setWord4(String word4) {
        this.word4 = word4;
        notifyPropertyChanged(BR.word4);
    }

    @Bindable
    public String getWord5() {
        return word5;
    }

    public void setWord5(String word5) {
        this.word5 = word5;
        notifyPropertyChanged(BR.word5);
    }

    @Bindable
    public String getWord6() {
        return word6;
    }

    public void setWord6(String word6) {
        this.word6 = word6;
        notifyPropertyChanged(BR.word6);
    }

    @Bindable
    public String getWord7() {
        return word7;
    }

    public void setWord7(String word7) {
        this.word7 = word7;
        notifyPropertyChanged(BR.word7);
    }

    @Bindable
    public String getWord8() {
        return word8;
    }

    public void setWord8(String word8) {
        this.word8 = word8;
        notifyPropertyChanged(BR.word8);
    }

    @Bindable
    public String getWord9() {
        return word9;
    }

    public void setWord9(String word9) {
        this.word9 = word9;
        notifyPropertyChanged(BR.word9);
    }

    @Bindable
    public String getWord10() {
        return word10;
    }

    public void setWord10(String word10) {
        this.word10 = word10;
        notifyPropertyChanged(BR.word10);
    }

    @Bindable
    public String getWord11() {
        return word11;
    }

    public void setWord11(String word11) {
        this.word11 = word11;
        notifyPropertyChanged(BR.word11);
    }

    @Bindable
    public String getWord12() {
        return word12;
    }

    public void setWord12(String word12) {
        this.word12 = word12;
        notifyPropertyChanged(BR.word12);
    }
}