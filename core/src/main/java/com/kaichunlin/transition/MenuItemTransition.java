package com.kaichunlin.transition;

import android.app.Activity;
import android.support.annotation.CheckResult;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.kaichunlin.transition.internal.TransitionController;
import com.kaichunlin.transition.internal.TransitionControllerManager;
import com.kaichunlin.transition.util.TransitionStateLogger;
import com.kaichunlin.transition.util.TransitionUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides transition effects to a MenuItem.
 */
public class MenuItemTransition extends AbstractTransition<MenuItemTransition, MenuItemTransition.Setup> {
    private List<TransitionControllerManager> mTransittingMenuItems = new ArrayList<>();
    private final Toolbar mToolbar;
    private boolean mStarted;
    private boolean mSetVisibleOnStartTransition;
    private boolean mInvalidateOptionOnStopTransition;
    private WeakReference<Activity> mActivityRef;
    private int mMenuId;
    private List<MenuItem> menuItemList;

    /**
     * Creates transition effect for a Toolbar's {@link MenuItem}s.
     *
     * @param toolbar
     * @return
     */
    public MenuItemTransition(@NonNull Toolbar toolbar) {
        this(null, toolbar, null);
    }

    /**
     * Creates transition effect for a Toolbar's {@link MenuItem}s.
     *
     * @param id      Same as calling {@link #setId(String)}, set an ID for debugging purpose.
     * @param toolbar
     * @return
     */
    public MenuItemTransition(@Nullable String id, @NonNull Toolbar toolbar) {
        this(id, toolbar, null);
    }

    /**
     * Creates transition effect for a Toolbar's {@link MenuItem}s.
     *
     * @param toolbar
     * @param view    The custom View to display when the transition is active.
     * @return
     */
    public MenuItemTransition(@NonNull Toolbar toolbar, @Nullable View view) {
        this(null, toolbar, view);
    }

    /**
     * Creates transition effect for a Toolbar's {@link MenuItem}s.
     *
     * @param id      Same as calling {@link #setId(String)}, set an ID for debugging purpose.
     * @param toolbar
     * @param view    The custom View to display when the transition is active.
     * @return
     */
    public MenuItemTransition(@Nullable String id, @NonNull Toolbar toolbar, @Nullable View view) {
        super(id);
        this.mToolbar = toolbar;
        this.mTarget = view;
    }

    public MenuItemTransition setMenuId(@IdRes int menuId) {
        mMenuId = menuId;
        return self();
    }

    public int getMenuId() {
        return mMenuId;
    }

    /**
     * @param visible Sets whether the visibility of the target MenuItem be forcibly set when
     *                {@link #startTransition()} is called
     * @return
     */
    public MenuItemTransition setVisibleOnStartAnimation(boolean visible) {
        this.mSetVisibleOnStartTransition = visible;
        return self();
    }

    @Override
    public MenuItemTransition reverse() {
        super.reverse();
        mTransittingMenuItems.clear();
        return self();
    }

    @Override
    public boolean startTransition() {
        if (mStarted) {
            return false;
        }
        if (mSetVisibleOnStartTransition) {
            mToolbar.getMenu().setGroupVisible(0, true);
        }
        //only retrieve MenuItem from mToolbar when run for the first time
        if (mTransittingMenuItems.size() == 0) {
            if (mMenuId == 0) {
                menuItemList = TransitionUtil.getVisibleMenuItemList(mToolbar);
            } else { //this transition only applies to a specific MenuItem
                menuItemList = new ArrayList<>();
                MenuItem menuItem = TransitionUtil.getMenuItem(mToolbar, mMenuId);
                if (menuItem == null) {
                    return false;
                }
                menuItemList.add(menuItem);
            }
            LayoutInflater layoutInflater = LayoutInflater.from(mToolbar.getContext());
            int j;
            int setupSize;
            for (int i = 0, size = menuItemList.size(); i < size; i++) {
                MenuItem menuItem = menuItemList.get(i);
                TransitionControllerManager transitionControllerManager = new TransitionControllerManager(getId());
                if (mInterpolator != null) {
                    transitionControllerManager.setInterpolator(mInterpolator);
                }
                setupSize = mSetupList.size();
                for (j = 0; j < setupSize; j++) {
                    mSetupList.get(j).setupAnimation(menuItem, transitionControllerManager, i, size);
                }
                View view;
                if (mTarget == null) { //uses default view with original menu icon
                    view = layoutInflater.inflate(R.layout.menu_animation, null).findViewById(R.id.menu_animation);
                    ((ImageView) view).setImageDrawable(menuItem.getIcon());
                } else {
                    view = mTarget;
                }
                if (TransitionConfig.isDebug()) {
                    view.setTag(R.id.debug_id, new TransitionStateLogger(getId()));
                }
                if (mReverse) {
                    transitionControllerManager.reverse();
                }
                menuItem.setActionView(view);
                transitionControllerManager.setTarget(view);
                transitionControllerManager.start();
                mTransittingMenuItems.add(transitionControllerManager);
            }
        }
        mStarted = true;
        return true;
    }

    @Override
    public void updateProgress(float progress) {
        for (int i = 0, size = mTransittingMenuItems.size(); i < size; i++) {
            mTransittingMenuItems.get(i).updateProgress(progress);
        }
    }

    @Override
    public void stopTransition() {
        for (int i = 0, size = mTransittingMenuItems.size(); i < size; i++) {
            mTransittingMenuItems.get(i).end();
        }
        if (menuItemList == null) {
            return;
        }
        for (int i = 0, size = menuItemList.size(); i < size; i++) {
            menuItemList.get(i).setActionView(null);
        }
        mTransittingMenuItems.clear();
        if (mInvalidateOptionOnStopTransition) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
            }
        }
        mStarted = false;
    }

    private Activity getActivity() {
        return mActivityRef == null ? null : mActivityRef.get();
    }

    /**
     * @return Should {@link Activity#invalidateOptionsMenu()} be called when transition stops.
     */
    public boolean isInvalidateOptionOnStopTransition() {
        return mInvalidateOptionOnStopTransition;
    }

    /**
     * Sets whether or not to call {@link Activity#invalidateOptionsMenu()} after a transition stops.
     *
     * @param activity   Activity that should have its {@link Activity#invalidateOptionsMenu()} method
     *                   called, or null if invalidate parameter is false.
     * @param invalidate
     * @return
     */
    public MenuItemTransition setInvalidateOptionOnStopTransition(@NonNull Activity activity, boolean invalidate) {
        this.mActivityRef = new WeakReference<>(activity);
        this.mInvalidateOptionOnStopTransition = invalidate;
        return self();
    }

    @CheckResult
    @Override
    public MenuItemTransition clone() {
        MenuItemTransition newCopy = (MenuItemTransition) super.clone();
        newCopy.mTransittingMenuItems = new ArrayList<>(mTransittingMenuItems.size());
        for (int i = 0, size = mTransittingMenuItems.size(); i < size; i++) {
            newCopy.mTransittingMenuItems.add(mTransittingMenuItems.get(i).clone());
        }
        return newCopy;
    }

    @Override
    protected void invalidate() {
    }

    @Override
    public boolean isCompatible(AbstractTransition another) {
        if (super.isCompatible(another) && mMenuId == ((MenuItemTransition) another).mMenuId) {
            return true;
        }
        return false;
    }

    @Override
    public boolean merge(AbstractTransition another) {
        if (super.merge(another)) {
            MenuItemTransition mit = (MenuItemTransition) another;
            mSetVisibleOnStartTransition |= mit.mSetVisibleOnStartTransition;
            mInvalidateOptionOnStopTransition |= mit.mInvalidateOptionOnStopTransition;
            return true;
        }
        return false;
    }

    @Override
    protected MenuItemTransition self() {
        return this;
    }

    /**
     * Represents an object that will create {@link TransitionController}s to be added to a
     * {@link TransitionControllerManager}.
     */
    public interface Setup extends AbstractTransition.Setup {
        /**
         * Create one or more {@link TransitionController} for each {@link android.view.MenuItem} and add
         * them to TransitionControllerManager.
         *
         * @param mMenuItem
         * @param manager
         * @param itemIndex Position of the MenuItem.
         * @param menuCount Total number of MenuItem's.
         */
        void setupAnimation(MenuItem mMenuItem, TransitionControllerManager manager, int itemIndex, int menuCount);
    }
}
