package com.neromatt.epiphany.ui.Navigation;

public interface OnSearchViewListener {
    boolean onQueryTextSubmit(String query);
    void onSearchClosed();
}
