/**
 *  Copyright 2014 Huy Phan <huyphan@playxiangqi.com>
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

#include <string.h>
#include <jni.h>

#include "hoxLog.h"
#include "hoxReferee.h"

static hoxReferee *referee_ = NULL;

extern "C" {

    JNIEXPORT jint JNICALL
    Java_com_playxiangqi_hoxchess_Referee_nativeCreateReferee(JNIEnv *env, jobject thiz)
    {
        LOGI("Create a new referee \n");
        referee_ = new hoxReferee();
        return 0;
    }

    JNIEXPORT jint JNICALL
    Java_com_playxiangqi_hoxchess_Referee_nativeResetGame(JNIEnv *env, jobject thiz)
    {
        LOGI("Reset the game \n");
        referee_->resetGame();
        return 0;
    }

    JNIEXPORT jint JNICALL
    Java_com_playxiangqi_hoxchess_Referee_nativeGetNextColor(JNIEnv *env, jobject thiz)
    {
        LOGI("get the next color... \n");
        hoxColor nextColor = referee_->getNextColor();
        return (jint) nextColor;
    }

    /*
     * @return hoxGAME_STATUS_UNKNOWN if the move is NOT valid.
     */
    JNIEXPORT jint JNICALL
    Java_com_playxiangqi_hoxchess_Referee_nativeValidateMove( JNIEnv *env, jobject thiz,
                                                              jint row1, jint col1,
                                                              jint row2, jint col2 )
    {
        LOGI("validateMove(): [RAW]: (%d, %d) => (%d, %d) \n", row1, col1, row2, col2);

        hoxMove move;
        hoxPosition fromPosition(col1, row1);
        move.newPosition = hoxPosition(col2, row2);

        bool found = referee_->_getPieceAtPosition(fromPosition,
                                                   move.piece );
        LOGI("validateMove(): ... found piece (%d) \n", found);
        if ( ! found )
        {
            LOGW("validateMove(): ... the 'from' piece not found! \n");
            return hoxGAME_STATUS_UNKNOWN;
        }

        hoxGameStatus status = hoxGAME_STATUS_UNKNOWN;
        if ( ! referee_->validateMove( move, status ))
        {
            LOGW("validateMove(): ... the move is NOT valid! \n");
            return hoxGAME_STATUS_UNKNOWN;
        }

        LOGI("validateMove(): ... the move is valid. status = %d. \n", status);
        return status;
    }

}
