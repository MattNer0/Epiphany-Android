package com.neromatt.epiphany.ui.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

public interface LayoutFactory {
    View produceLayout(LayoutInflater inflater, @Nullable ViewGroup container);
}