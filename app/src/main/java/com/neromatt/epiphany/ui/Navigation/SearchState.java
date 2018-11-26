package com.neromatt.epiphany.ui.Navigation;

public class SearchState {
    private String search_string;
    private Boolean search_open;

    SearchState(String search_string, Boolean search_open) {
        this.search_string = search_string;
        this.search_open = search_open;
    }

    Boolean getSearchOpen() {
        return search_open;
    }

    String getSearchString() {
        return search_string;
    }

    @Override
    public String toString() {
        return "SearchState[" + search_string +"][" + search_open + "]";
    }
}
