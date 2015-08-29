/*
 * Copyright (C) 2015 mutter
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mutter.com.carouselimageview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * A carousel image view, support left touch scroll<br/>
 * Created by mutter on 7/20/15.
 */
public class CarouselImageView extends FrameLayout implements Runnable {

    private static final String TAG = CarouselImageView.class.getSimpleName();
    private static final int DEFAULT_PLAY_INTERVAL = 3000;
    private static final int DEFAULT_ANIMATION_DURATION = 2000;
    private static final int DEFAULT_INDICATOR_SIZE = 20; /* dp */
    private static final int DEFAULT_INDICATOR_NORMAL_COLOR = 0xffffffff;
    private static final int DEFAULT_INDICATOR_SELECTED_COLOR = 0xffffaa00;
    private static final int DEFAULT_INDICATOR_BOTTOM_MARGIN = 100;
    private static final int DEFAULT_INDICATOR_SPACE = 10;
    private Context context;
    private int index = 0;
    private int currentSelected = 0;
    private LinearLayout indicatorLayout;

    private ImageView imageView1;
    private ImageView imageView2;

    private int indicatorNormalColor;
    private int indicatorSelectedColor;
    private int indicatorNormalSize;
    private int indicatorSelectedSize;
    private int indicatorBottomMargin;
    private int indicatorSpace;
    private ShapeDrawable indicatorSelectedDrawable;
    private ShapeDrawable indicatorNormalDrawable;
    private boolean isAutoStart;
    private int playInterval;
    private int animationDuration;

    private float startedPoint;
    private boolean isLeftScroll = false;

    private int[] imageResIds;

    private Drawable[] drawables;

    public CarouselImageView(Context context) {
        super(context);
        initView(context, null, 0);
    }

    public CarouselImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs, 0);
    }

    public CarouselImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr);
    }

    private void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        this.context = context;
        TypedArray typedArray = null;
        try {
            typedArray = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.CarouselImageView, 0, 0);
            indicatorNormalColor = typedArray.getColor(R.styleable.CarouselImageView_indicatorNormalColor,
                    DEFAULT_INDICATOR_NORMAL_COLOR);
            indicatorSelectedColor = typedArray.getColor(R.styleable.CarouselImageView_indicatorSelectedColor,
                    DEFAULT_INDICATOR_SELECTED_COLOR);
            indicatorNormalSize = typedArray.getDimensionPixelSize(R.styleable.CarouselImageView_indicatorNormalSize,
                    (int) (DEFAULT_INDICATOR_SIZE * context.getResources().getDisplayMetrics().density));
            indicatorSelectedSize = typedArray.getDimensionPixelSize(R.styleable.CarouselImageView_indicatorSelectedSize,
                    (int) (DEFAULT_INDICATOR_SIZE * context.getResources().getDisplayMetrics().density));
            indicatorBottomMargin = typedArray.getDimensionPixelSize(R.styleable.CarouselImageView_indicatorBottomMargin,
                    DEFAULT_INDICATOR_BOTTOM_MARGIN);
            indicatorSpace = typedArray.getDimensionPixelSize(R.styleable.CarouselImageView_indicatorSpace,
                    DEFAULT_INDICATOR_SPACE);
            isAutoStart = typedArray.getBoolean(R.styleable.CarouselImageView_isAutoStart, true);
            playInterval = typedArray.getInt(R.styleable.CarouselImageView_playInterval, DEFAULT_PLAY_INTERVAL);
            animationDuration = typedArray.getInt(R.styleable.CarouselImageView_animationDuration, DEFAULT_ANIMATION_DURATION);
        } finally {
            if (typedArray != null) {
                typedArray.recycle();
            }
        }

        indicatorSelectedDrawable = new ShapeDrawable(new OvalShape());
        indicatorSelectedDrawable.setIntrinsicWidth(indicatorSelectedSize);
        indicatorSelectedDrawable.setIntrinsicHeight(indicatorSelectedSize);
        indicatorSelectedDrawable.getPaint().setColor(indicatorSelectedColor);

        indicatorNormalDrawable = new ShapeDrawable(new OvalShape());
        indicatorNormalDrawable.setIntrinsicWidth(indicatorNormalSize);
        indicatorNormalDrawable.setIntrinsicHeight(indicatorNormalSize);
        indicatorNormalDrawable.getPaint().setColor(indicatorNormalColor);

        LayoutInflater.from(context).inflate(R.layout.carousel_layout, this);
        imageView1 = (ImageView) findViewById(R.id.imageview1);
        imageView2 = (ImageView) findViewById(R.id.imageview2);

        FrameLayout.LayoutParams indicatorLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        indicatorLayoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        indicatorLayoutParams.bottomMargin = indicatorBottomMargin;

        indicatorLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.carousel_indicator, null);
        addView(indicatorLayout, indicatorLayoutParams);
    }

    private void setupIndicator() {
        indicatorLayout.removeAllViews();
        for (Drawable drawable : drawables) {
            ImageView imageView = new ImageView(context);
            imageView.setBackgroundDrawable(indicatorNormalDrawable);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.leftMargin = indicatorSpace;
            params.rightMargin = indicatorSpace;
            imageView.setLayoutParams(params);
            indicatorLayout.addView(imageView);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CarouselImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isAutoStart) {
            startPlay();
        }
    }

    /**
     * play next image more quickly, used by scroll left touch event
     */
    public void startPlayNextImmediately() {
        postDelayed(this, 0);
        animationDuration = 200;
    }

    public void startPlay() {
        postDelayed(this, playInterval);
    }

    public void stopPlay() {
        removeCallbacks(this);
    }

    private void startAnimation() {
        TranslateAnimation fadeOutTranslateAnimation = new TranslateAnimation(0, -getWidth(), 0, 0);
        fadeOutTranslateAnimation.setDuration(animationDuration);
        fadeOutTranslateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                shiftImageViewPosition();
                currentSelected++;
                if (currentSelected >= drawables.length) {
                    currentSelected = 0;
                }
                updateIndicator();

                animationDuration = DEFAULT_ANIMATION_DURATION;
                isLeftScroll = true;
                postDelayed(CarouselImageView.this, playInterval);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        TranslateAnimation fadeInTranslateAnimation = new TranslateAnimation(getWidth(), 0, 0, 0);
        fadeInTranslateAnimation.setDuration(animationDuration);
        getChildAt(0).startAnimation(fadeInTranslateAnimation);
        getChildAt(getChildCount() - 2).startAnimation(fadeOutTranslateAnimation);
    }

    private void updateIndicator() {
        for (int i = 0; i < indicatorLayout.getChildCount(); i++) {
            indicatorLayout.getChildAt(i).setBackgroundDrawable(indicatorNormalDrawable);
        }
        indicatorLayout.getChildAt(currentSelected).setBackgroundDrawable(indicatorSelectedDrawable);
    }

    private void shiftImageViewPosition() {
        final ImageView imageView = new ImageView(context);
        FrameLayout.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        imageView.setLayoutParams(params);
        imageView.setImageDrawable(drawables[index++]);
        if (index >= drawables.length) {
            index = 0;
        }
        removeViewAt(getChildCount() - 2);
        addView(imageView, 0);
        postInvalidate();
    }

    @Override
    public void run() {
        startAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startedPoint = event.getX();
                stopPlay();
                return true;
            case MotionEvent.ACTION_MOVE:
                isLeftScroll = event.getX() < startedPoint;
                break;
            case MotionEvent.ACTION_UP:
                if (isLeftScroll) {
                    startPlayNextImmediately();
                }
                break;
            default:
                break;

        }
        return super.onTouchEvent(event);
    }

    public int[] getImageResIds() {
        return imageResIds;
    }

    /**
     * A list of image resource ids to play
     * @param imageResIds
     */
    public void setImageResIds(int[] imageResIds) {
        this.imageResIds = imageResIds;

        if (imageResIds != null) {
            Drawable[] drawables = new Drawable[imageResIds.length];
            for (int i = 0; i < imageResIds.length; i++) {
                drawables[i] = getResources().getDrawable(imageResIds[i]);
            }

            setDrawables(drawables);
        }
    }

    public Drawable[] getDrawables() {
        return drawables;
    }

    public void setDrawables(Drawable[] drawables) {
        this.drawables = drawables;

        imageView1.setImageDrawable(drawables[1]);
        imageView2.setImageDrawable(drawables[0]);
        setupIndicator();
        index = 2;
        currentSelected = 0;

        updateIndicator();
    }

    /**
     * A list of bitmaps to set to play
     * @param bitmaps
     */
    public void setBitmaps(Bitmap[] bitmaps) {
        if (bitmaps != null) {
            final int size = bitmaps.length;
            Drawable[] drawables = new Drawable[size];
            for (int i = 0; i < size; i++) {
                drawables[i] = new BitmapDrawable(getResources(), bitmaps[i]);
            }

            setDrawables(drawables);
        }
    }
    public boolean isLeftScroll() {
        return isLeftScroll;
    }
}
