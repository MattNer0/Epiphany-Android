package com.neromatt.epiphany.ui.Navigation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.appbar.AppBarLayout;
import com.neromatt.epiphany.model.Adapters.RackAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.ui.R;

import java.lang.reflect.Array;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
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

    private Toolbar toolbar;
    private TextView toolbar_title;
    private FabSpeedDial fab_menu;

    //private ActionBarDrawerToggle drawer_toggle;
    private DrawerLayout drawer_layout;
    private ImageView drawer_close_arrow;

    public NavigationLayoutFactory(boolean includeToolbar, boolean includeBreadCrumbs, boolean includeFab, boolean includeDrawer, LayoutFactory origin) {
        this.includeToolbar = includeToolbar || includeDrawer;
        this.includeBreadCrumbs = includeBreadCrumbs;
        this.includeFab = includeFab;
        this.includeDrawer = includeDrawer;
        this.origin = origin;
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
        }

        if (includeToolbar) {
            inflater.inflate(R.layout.layout_with_toolbar, parent);
        }

        if (includeBreadCrumbs) {
            inflater.inflate(R.layout.layout_with_breadcrumbs, parent);
        }

        parent.addView(child, childParams);

        if (includeFab) {
            inflater.inflate(R.layout.layout_with_fab, parent);
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
        this.toolbar_title = view.findViewById(R.id.toolbar_title);
        this.fab_menu = view.findViewById(R.id.notebookFab);

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

        return this;
    }

    public void hideDrawer() {
        if (includeDrawer) {
            //drawer_toggle.setDrawerIndicatorEnabled(false);
            if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                drawer_layout.closeDrawer(GravityCompat.START, true);
            }
        }
    }

    public void showDrawer() {
        if (includeDrawer) {
            //drawer_toggle.setDrawerIndicatorEnabled(true);
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

            final ActionBar mActionBar = activity.getSupportActionBar();
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