package com.neromatt.epiphany.ui.Navigation;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;
import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.SlideAnimationHelper;
import com.neromatt.epiphany.model.Adapters.BreadcrumbAdapter;
import com.neromatt.epiphany.model.Adapters.MainAdapter;
import com.neromatt.epiphany.model.Adapters.MovingNotesAdapter;
import com.neromatt.epiphany.model.Adapters.RackAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.ui.MainActivity;
import com.neromatt.epiphany.ui.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.github.kobakei.materialfabspeeddial.FabSpeedDial;
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu;

public final class NavigationLayoutFactory implements LayoutFactory {

    private final LayoutFactory origin;

    private final boolean includeToolbar;
    private final boolean includeBreadCrumbs;
    private final boolean includeFab;
    private final boolean includeDrawer;
    private final boolean includeInputQuickNote;

    private boolean search_opened;

    private Toolbar toolbar;
    private TextView toolbar_title;
    private SearchView toolbar_search;
    private ImageView toolbar_search_clear;
    private EditText toolbar_search_text;
    private RecyclerView toolbar_breadcrumbs_list;
    private FabSpeedDial fab_menu;

    private OnSearchViewListener toolbar_search_listener;
    private DrawerLayout drawer_layout;
    private ImageView drawer_close_arrow;
    private EditText quick_note_edit_text;

    private ActionBar mActionBar;

    private BreadcrumbAdapter breadcrumb_adapter;

    private LinearLayout moving_notes_container;
    private ImageView moving_notes_close;
    private RecyclerView moving_notes_view;
    private TextView moving_notes_info;
    private LinearLayout moving_notes_submit;

    private OnMovingNoteListener moving_notes_submit_listener;

    private MainAdapter<MainModel> moving_notes_adapter;
    private ArrayList<MainModel> moving_notes;

    private MenuItem action_list_layout;
    private MenuItem action_staggered_layout;
    private MenuItem action_search_view;

    public NavigationLayoutFactory(boolean includeToolbar, boolean includeBreadCrumbs, boolean includeFab, boolean includeDrawer, boolean inputQuickNote, LayoutFactory origin) {
        this.includeToolbar = includeToolbar || includeDrawer;
        this.includeBreadCrumbs = includeBreadCrumbs;
        this.includeFab = includeFab;
        this.includeDrawer = includeDrawer;
        this.includeInputQuickNote = inputQuickNote;
        this.origin = origin;

        this.search_opened = false;
        this.moving_notes = new ArrayList<>();
    }

    @Override
    public View produceLayout(LayoutInflater inflater, @Nullable ViewGroup container) {
        CoordinatorLayout parent = new CoordinatorLayout(inflater.getContext());
        parent.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        //parent.setOrientation(LinearLayout.VERTICAL);

        View child = origin.produceLayout(inflater, parent);

        CoordinatorLayout.LayoutParams childParams = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        if (includeToolbar) {
            childParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
            inflater.inflate(R.layout.layout_with_toolbar, parent);
        }

        if (includeBreadCrumbs || includeInputQuickNote) {
            LinearLayout parentLinear = new LinearLayout(inflater.getContext());
            parentLinear.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            parentLinear.setOrientation(LinearLayout.VERTICAL);

            if (includeBreadCrumbs) inflater.inflate(R.layout.layout_with_breadcrumbs, parentLinear);

            LinearLayout.LayoutParams childLinearParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            childLinearParams.weight = 1;

            parentLinear.addView(child, childLinearParams);
            parent.addView(parentLinear, childParams);

            if (includeInputQuickNote) inflater.inflate(R.layout.layout_with_input_quick_note, parentLinear);

        } else {
            parent.addView(child, childParams);
        }

        if (includeFab) {
            inflater.inflate(R.layout.layout_with_fab, parent);
            inflater.inflate(R.layout.layout_with_moving_notes, parent);
        }

        /*if (includeBottomBar) {
            int height = (int) dp(parent.getContext(), 56);
            AHBottomNavigation bottomNavigation = new AmruBottomNavigation(parent.getContext());
            bottomNavigation.setId(R.id.bottomNavigation);
            parent.addView(bottomNavigation, new LinearLayout.LayoutParams(MATCH_PARENT, height));
        }*/

        return parent;
    }

    public NavigationLayoutFactory viewCreated(AppCompatActivity activity, View view) {
        this.toolbar = view.findViewById(R.id.toolbar);
        if (includeToolbar) {
            this.toolbar_title = view.findViewById(R.id.toolbar_title);
            this.toolbar_search = view.findViewById(R.id.toolbar_search);
            this.toolbar_search_clear = toolbar_search.findViewById(R.id.search_close_btn);
            this.toolbar_search_text = toolbar_search.findViewById(R.id.search_src_text);
            this.toolbar_search.setVisibility(View.GONE);
        }

        if (includeBreadCrumbs) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.toolbar.setElevation(0);
            }
        }

        if (includeFab) {
            this.fab_menu = view.findViewById(R.id.notebookFab);
            this.moving_notes_container = view.findViewById(R.id.movingContainer);
            this.moving_notes_close = view.findViewById(R.id.movingClose);
            this.moving_notes_view = view.findViewById(R.id.movingList);
            this.moving_notes_info = view.findViewById(R.id.movingInfo);
            this.moving_notes_submit = view.findViewById(R.id.movingConfirm);
            this.moving_notes_container.setVisibility(View.GONE);
        }

        setSupportActionBar(activity);

        if (includeDrawer) {
            drawer_layout = view.getRootView().findViewById(R.id.drawer_layout);
            drawer_close_arrow = drawer_layout.findViewById(R.id.left_drawer_close);

            drawer_close_arrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    closeDrawerIfOpen();
                }
            });

            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                        drawer_layout.closeDrawer(GravityCompat.START, true);
                    } else {
                        drawer_layout.openDrawer(GravityCompat.START, true);
                    }
                }
            });
        }

        if (includeBreadCrumbs) {
            toolbar_breadcrumbs_list = view.findViewById(R.id.breadcrumbs_list);
            toolbar_breadcrumbs_list.setVisibility(View.GONE);
        }

        if (includeInputQuickNote) {
            quick_note_edit_text = view.findViewById(R.id.quickNoteEdit);
            quick_note_edit_text.clearFocus();
        }

        return this;
    }

    public ArrayList<MainModel> getMovingNotes() {
        return this.moving_notes;
    }

    public void addMovingNote(MainActivity ma, MainModel note) {
        boolean new_list = false;
        if (this.moving_notes == null || this.moving_notes.size() == 0) {
            this.moving_notes = new ArrayList<>();
            new_list = true;
        }
        this.moving_notes.add(note);

        if (new_list || moving_notes_adapter == null) {
            initMovingNotes(ma);
            SlideAnimationHelper.slideInFromRight(ma, moving_notes_container);
        } else {
            moving_notes_adapter.addItem(note);
            setInfoMovingNotes();
        }
    }

    private Point getScreenSize(MainActivity ma) {
        Display display = ma.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        try {
            display.getRealSize(size);
        } catch (NoSuchMethodError err) {
            display.getSize(size);
        }
        return size;
    }

    public void setMovingNotes(MainActivity ma, ArrayList<MainModel> list) {
        this.moving_notes = list;
        if (list != null && list.size() > 0) {
            initMovingNotes(ma);
        }
    }

    public void setMovingNoteListener(OnMovingNoteListener listener) {
        this.moving_notes_submit_listener = listener;
    }

    private void setInfoMovingNotes() {
        if (moving_notes.size() == 1) {
            String str = moving_notes.size() + " note selected";
            moving_notes_info.setText(str);
        } else if (moving_notes.size() > 1) {
            String str = moving_notes.size() + " notes selected";
            moving_notes_info.setText(str);
        } else {
            moving_notes_info.setText("");
        }
    }

    private void initMovingNotes(MainActivity ma) {
        final WeakReference<MainActivity> weakMa = new WeakReference<>(ma);
        if (includeFab && fab_menu != null) {
            fab_menu.setVisibility(View.GONE);
        }

        if (includeInputQuickNote) {
            quick_note_edit_text.setVisibility(View.GONE);
        }

        moving_notes_adapter = new MainAdapter<>(moving_notes);
        moving_notes_adapter.setSpanCount(0);
        moving_notes_view.setLayoutManager(new LinearLayoutManager(ma));
        moving_notes_view.setAdapter(moving_notes_adapter);

        setInfoMovingNotes();

        Point screenSize = getScreenSize(ma);

        moving_notes_container.setVisibility(View.VISIBLE);

        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) moving_notes_container.getLayoutParams(); //new(), LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.width = (int)(screenSize.x*0.43);
        lp.height = (int)(screenSize.y*0.37);

        moving_notes_container.setLayoutParams(lp);

        if (!includeInputQuickNote) {
            moving_notes_submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (moving_notes_submit_listener.onMovingNote(moving_notes)) {
                        closeMovingNotes(weakMa.get());
                    }
                }
            });
        }

        moving_notes_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeMovingNotes(weakMa.get());
            }
        });
    }

    private void closeMovingNotes(MainActivity ma) {
        if (ma == null) return;
        moving_notes = null;
        if (includeFab && fab_menu != null) {
            fab_menu.setVisibility(View.VISIBLE);
        }
        moving_notes_container.setVisibility(View.GONE);
        SlideAnimationHelper.slideOutToRight(ma, moving_notes_container);

        if (includeInputQuickNote) {
            quick_note_edit_text.setVisibility(View.VISIBLE);
        }
    }

    public void setQuickNoteListener(final OnQuickNoteEdit listener) {
        if (includeInputQuickNote) {
            quick_note_edit_text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String text = quick_note_edit_text.getText().toString();
                    listener.openQuickNote(text);
                }
            });
            quick_note_edit_text.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        String text = quick_note_edit_text.getText().toString();
                        listener.openQuickNote(text);
                    }
                }
            });
        }
    }

    public void setBreadcrumbs(Context context, ArrayList<Breadcrumb> breadcrumbs, BreadcrumbAdapter.OnBreadcrumbClickListener listener) {
        if (includeBreadCrumbs) {
            if (breadcrumbs.size() > 0) {
                LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                toolbar_breadcrumbs_list.setLayoutManager(layoutManager);
                toolbar_breadcrumbs_list.setVisibility(View.VISIBLE);
            }

            breadcrumb_adapter = new BreadcrumbAdapter(context, breadcrumbs, listener);
            toolbar_breadcrumbs_list.setAdapter(breadcrumb_adapter);
        }
    }

    public void onCreateMenu(Context context, Menu menu) {
        action_list_layout = menu.findItem(R.id.action_list_layout);
        action_staggered_layout = menu.findItem(R.id.action_staggered_layout);
        action_search_view = menu.findItem(R.id.action_search);

        toggleLayoutItem(context);
    }

    private void toggleLayoutItem(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_staggered_layout", false)) {
            action_list_layout.setVisible(true);
            action_staggered_layout.setVisible(false);
        } else {
            action_staggered_layout.setVisible(true);
            action_list_layout.setVisible(false);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item, MainActivity ma, OnOptionMenuListener listener) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            ma.showSettings();
            return true;
        } else if (id == R.id.action_quick_notes) {
            ma.openQuickNotes();
            return true;
        } else if (id == R.id.action_list_layout) {
            if (action_list_layout != null && action_staggered_layout != null) {
                action_staggered_layout.setVisible(true);
                action_list_layout.setVisible(false);
            }
            listener.updateLayoutList(false);
            return true;
        } else if (id == R.id.action_staggered_layout) {
            if (action_list_layout != null && action_staggered_layout != null) {
                action_list_layout.setVisible(true);
                action_staggered_layout.setVisible(false);
            }
            listener.updateLayoutList(true);
            return true;
        } else if (id == R.id.action_search) {
            if (search_opened) {
                hideSearch(ma);
            } else {
                showSearch("", true);
            }
            listener.updateSearchStatus(search_opened);
            return true;
        }

        return false;
    }

    public void setOnQueryTextListener(OnSearchViewListener listener) {
        if (toolbar_search != null) {
            toolbar_search_listener = listener;
            toolbar_search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return toolbar_search_listener.onQueryTextSubmit(query);
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
            toolbar_search_clear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (toolbar_search.getQuery().toString().isEmpty()) {
                        toolbar_search_listener.onSearchClosed();
                    } else {
                        toolbar_search_text.setText("");
                        toolbar_search.setQuery("", false);
                        toolbar_search_listener.onQueryTextSubmit("");
                    }
                }
            });
        }
    }

    public void hideDrawer() {
        if (includeDrawer) {
            if (mActionBar != null) {
                mActionBar.setHomeButtonEnabled(false);
                mActionBar.setDisplayHomeAsUpEnabled(false);
            }
            if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                drawer_layout.closeDrawer(GravityCompat.START, true);
            }
        }
    }

    public void showDrawer() {
        if (includeDrawer) {
            if (mActionBar != null) {
                mActionBar.setHomeButtonEnabled(true);
                mActionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    public void showSearch(String initial, boolean focus) {
        if (includeToolbar) {
            action_list_layout.setVisible(false);
            action_staggered_layout.setVisible(false);
            action_search_view.setVisible(false);

            toolbar_title.setVisibility(View.GONE);
            toolbar_search.setVisibility(View.VISIBLE);
            toolbar_search.setIconified(false);
            toolbar_search.setFocusable(true);
            toolbar_search.setQuery(initial, false);
            toolbar_search_text.setText(initial);
            if (focus) toolbar_search.requestFocusFromTouch();
            hideDrawer();
        }
    }

    private void hideSearch(Context context) {
        if (includeToolbar) {
            toggleLayoutItem(context);
            action_search_view.setVisible(true);

            toolbar_title.setVisibility(View.VISIBLE);
            toolbar_search.setVisibility(View.GONE);
            toolbar_search.clearFocus();
            toolbar_search.setFocusable(false);
            showDrawer();
        }
    }

    public void clearSearchFocus() {
        if (includeToolbar) {
            toolbar_search.clearFocus();
        }
    }

    public boolean closeDrawerIfOpen() {
        if (includeDrawer && drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START, true);
            return true;
        }
        return false;
    }

    public void setSupportActionBar(AppCompatActivity activity) {
        if (includeToolbar) {
            activity.setSupportActionBar(toolbar);

            mActionBar = activity.getSupportActionBar();
            if (includeDrawer) {
                mActionBar.setDisplayHomeAsUpEnabled(true);
                mActionBar.setHomeButtonEnabled(true);
                mActionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
            } else {
                mActionBar.setDisplayHomeAsUpEnabled(false);
                mActionBar.setHomeButtonEnabled(false);
            }
            mActionBar.setDisplayShowCustomEnabled(true);
            mActionBar.setDisplayShowTitleEnabled(false);
        }
    }

    public void addOnFabMenuItemClickListener(FabSpeedDial.OnMenuItemClickListener listener) {
        if (includeFab) {
            fab_menu.removeAllOnMenuItemClickListeners();
            fab_menu.addOnMenuItemClickListener(listener);
        }
    }

    public void hideFab() {
        if (includeFab) {
            fab_menu.setVisibility(View.GONE);
        }
    }

    public void showFab() {
        if (includeFab) {
            fab_menu.setVisibility(View.VISIBLE);
        }
    }

    public void setFabMenu(FabSpeedDialMenu menu) {
        if (includeFab) {
            fab_menu.setVisibility(View.VISIBLE);
            fab_menu.setMenu(menu);
        }
    }

    public Toolbar getToolbar() {
        return this.toolbar;
    }

    public void setTitle(CharSequence title) {
        if (includeToolbar) {
            toolbar_title.setText(title);
        }
    }

    public void setTitle(int resId) {
        if (includeToolbar) {
            toolbar_title.setText(resId);
        }
    }
}