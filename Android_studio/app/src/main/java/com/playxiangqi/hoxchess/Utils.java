/**
 *  Copyright 2015 Huy Phan <huyphan@playxiangqi.com>
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

import android.annotation.SuppressLint;

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

}
