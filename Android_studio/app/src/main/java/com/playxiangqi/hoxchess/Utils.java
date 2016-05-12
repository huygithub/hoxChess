/**
 *  Copyright 2016 Huy Phan <huyphan@playxiangqi.com>
 *
 *  This file is part of HOXChess.
 *
 *  HOXChess is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  HOXChess is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with HOXChess.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.playxiangqi.hoxchess;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.support.v4.view.ViewPager;
import android.view.animation.AccelerateInterpolator;

import java.util.Random;

public class Utils {

    /**
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * Reference;
     *   http://stackoverflow.com/questions/363681/generating-random-integers-in-a-range-with-java
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int min, int max) {
        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        return rand.nextInt((max - min) + 1) + min;
    }

    @SuppressLint("DefaultLocale")
    public static String generateGuestPid() {
        final int randNum = randInt(1, Enums.MAX_GUEST_ID);
        // Note: The portion "an" below stands for "Android".
        return String.format("%san%d", Enums.HC_GUEST_PREFIX, randNum);
    }

    /**
     * Converts a string of a "network" color to an enum.
     */
    public static Enums.ColorEnum stringToPlayerColor(String color) {
        if ("Red".equals(color)) return Enums.ColorEnum.COLOR_RED;
        if ("Black".equals(color)) return Enums.ColorEnum.COLOR_BLACK;
        if ("None".equals(color)) return Enums.ColorEnum.COLOR_NONE;
        return Enums.ColorEnum.COLOR_UNKNOWN;
    }

    /**
     * Converts a Orientation enum to a string.
     */
    public static String orientationToString(int orientation) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) { return "ORIENTATION_LANDSCAPE"; }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) { return "ORIENTATION_PORTRAIT"; }
        return "ORIENTATION_UNDEFINED";
    }

    public static String gameStatusToString(Enums.GameStatus gameStatus) {
        switch (gameStatus) {
            case GAME_STATUS_UNKNOWN:     return "Unknown";
            case GAME_STATUS_IN_PROGRESS: return "Progress";
            case GAME_STATUS_RED_WIN:     return "Red_win";
            case GAME_STATUS_BLACK_WIN:   return "Black_win";
            case GAME_STATUS_DRAWN:       return "Drawn";
            default: return "__BUG_Not_Supported_Game_Status__:" + gameStatus;
        }
    }

    /**
     * Move the ViewPager to a direction (forward or backward) in certain speed.
     *
     * NOTE: This API is copied from:
     *  http://stackoverflow.com/questions/8155257/slowing-speed-of-viewpager-controller-in-android
     */
    public static void animatePagerTransition(final ViewPager viewPager, final boolean forward,
                                              long animationDurationInMillSecs) {
        // The move factor, which was set to 1.0 in the original version of the function.
        // In this app, I have to reduce it to avoid moving to 2 pages (instead of 1).
        final float MOVE_FACTOR = 0.6f;

        int moveX = viewPager.getWidth() - (forward ? viewPager.getPaddingLeft() : viewPager.getPaddingRight());
        moveX = (int) (moveX * MOVE_FACTOR);
        ValueAnimator animator = ValueAnimator.ofInt(0, moveX);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                viewPager.endFakeDrag();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                viewPager.endFakeDrag();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });

        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            private int oldDragPosition = 0;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int dragPosition = (Integer) animation.getAnimatedValue();
                int dragOffset = dragPosition - oldDragPosition;
                oldDragPosition = dragPosition;
                viewPager.fakeDragBy(dragOffset * (forward ? -1 : 1));
            }
        });

        animator.setDuration(animationDurationInMillSecs);
        viewPager.beginFakeDrag();
        animator.start();
    }
}
