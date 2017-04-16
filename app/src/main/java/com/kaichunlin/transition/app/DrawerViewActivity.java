package com.kaichunlin.transition.app;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.kaichunlin.transition.Cascade;
import com.kaichunlin.transition.TransitionManager;
import com.kaichunlin.transition.TransitionManagerListener;
import com.kaichunlin.transition.ViewTransition;
import com.kaichunlin.transition.ViewTransitionBuilder;
import com.kaichunlin.transition.adapter.DrawerListenerAdapter;
import com.kaichunlin.transition.animation.AnimationManager;
import com.kaichunlin.transition.internal.debug.TraceTransitionManagerListener;
import com.kaichunlin.transition.util.TransitionUtil;

import kaichunlin.transition.app.R;


public class DrawerViewActivity extends AppCompatActivity implements View.OnClickListener {

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private DrawerListenerAdapter mDrawerListenerAdapter;
    private ViewTransitionBuilder mRotateEffectBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_drawer_view);
        findViewById(R.id.rotate_android).setOnClickListener(this);
        findViewById(R.id.slide_subviews).setOnClickListener(this);
        findViewById(R.id.slide_bg).setOnClickListener(this);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,              /* host Activity */
                mDrawerLayout,    /* DrawerLayout object */
                toolbar,
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        );

        //Create an adapter that listens for ActionBarDrawerToggle state change and update transition states
        mDrawerListenerAdapter = new DrawerListenerAdapter(mDrawerToggle, R.id.drawerList);
        mDrawerListenerAdapter.setDrawerLayout(mDrawerLayout);
        mDrawerListenerAdapter.setDrawerListener(new DialogDrawerListener(this));

        //this builder is used to build both transition & animation effect
        mRotateEffectBuilder = ViewTransitionBuilder.transit(findViewById(R.id.big_icon)).rotation(0f, 360f).scaleX(1f, 0.2f).scaleY(1f, 0f).translationX(200f);

        //build the desired transition and adds to the adapter
        final ViewTransition transition = mRotateEffectBuilder.build();
        mDrawerListenerAdapter.addTransition(ViewTransitionBuilder.transit(findViewById(R.id.mainContent)).scale(0.9f));
        mDrawerListenerAdapter.addTransition(transition);

        //configure the transition after the layout is complete
        TransitionUtil.executeOnGlobalLayout(this, new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //since the start animation is the reverse of the transition, set the current view state to transition's final state
                transition.setProgress(1f);
                //init an animation and add a delay to prevent stutter, needs to be higher if animation is enabled
                final AnimationManager animationManager = new AnimationManager();
                mRotateEffectBuilder.reverse().buildAnimationFor(animationManager);

                //add fade-in animation
                Cascade cascade = new Cascade(Cascade.SEQUENTIAL, 0.9f);
                ViewTransitionBuilder.transit(findViewById(R.id.lay_buttons)).alpha(0f, 1f).visible().interpolator(new AccelerateDecelerateInterpolator()).transitViewGroup(new ViewTransitionBuilder.ViewGroupTransition() {
                    @Override
                    public void transit(ViewTransitionBuilder builder, ViewTransitionBuilder.ViewGroupTransitionConfig config) {
                        builder.buildAnimationFor(animationManager);
                    }
                }, cascade);
                animationManager.startAnimation(600);

                //this is to prevent conflict when the drawer is being opened while the above animation is still in progress
                //unfortunately there's no way to reconcile the two, so the transiting/animating View will "jump" to a new state
                //TODO evaluate if it's possible to reconcile the two states automatically, maybe if they share the same ITransition instance?
                mDrawerListenerAdapter.addTransitionListener(new TransitionManagerListener() {
                    @Override
                    public void onTransitionStart(TransitionManager manager) {
                        animationManager.endAnimation();
                    }

                    @Override
                    public void onTransitionEnd(TransitionManager manager) {
                    }
                });
            }
        });

        //debug
        mDrawerListenerAdapter.addTransitionListener(new TraceTransitionManagerListener());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mDrawerToggle.syncState();
    }

    @Override
    public void onClick(View v) {
        updateTransition(v);
    }

    public void updateTransition(View v) {
        mDrawerListenerAdapter.removeAllTransitions();
        switch (v.getId()) {
            case R.id.rotate_android:
                mDrawerListenerAdapter.addTransition(ViewTransitionBuilder.transit(findViewById(R.id.mainContent)).scale(0.9f));
                mDrawerListenerAdapter.addTransition(mRotateEffectBuilder);
                break;
            case R.id.slide_subviews:
                Cascade cascade = new Cascade(Cascade.RUN_TO_THE_END, 0.6f).cascadeStart(0.2f);
                ViewTransitionBuilder.transit(findViewById(R.id.lay_buttons)).interpolator(new AccelerateDecelerateInterpolator()).transitViewGroup(new ViewTransitionBuilder.ViewGroupTransition() {
                    @Override
                    public void transit(ViewTransitionBuilder builder, ViewTransitionBuilder.ViewGroupTransitionConfig config) {
                        builder.translationX(config.parentViewGroup.getRight()).buildFor(mDrawerListenerAdapter);
                    }
                }, cascade);
                break;
            case R.id.slide_bg:
                mDrawerListenerAdapter.addTransition(ViewTransitionBuilder.transit(findViewById(R.id.bg)).interpolator(new LinearInterpolator()).translationXAsFractionOfWidth(0.25f));
                break;
        }
    }
}
