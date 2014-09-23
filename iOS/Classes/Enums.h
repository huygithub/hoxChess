/***************************************************************************
 *  Copyright 2010-2014 Huy Phan <huyphan@playxiangqi.com>                 *
 *                                                                         * 
 *  This file is part of HOXChess.                                         *
 *                                                                         *
 *  HOXChess is free software: you can redistribute it and/or modify       *
 *  it under the terms of the GNU General Public License as published by   *
 *  the Free Software Foundation, either version 3 of the License, or      *
 *  (at your option) any later version.                                    *
 *                                                                         *
 *  HOXChess is distributed in the hope that it will be useful,            *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 *  GNU General Public License for more details.                           *
 *                                                                         *
 *  You should have received a copy of the GNU General Public License      *
 *  along with HOXChess.  If not, see <http://www.gnu.org/licenses/>.      *
 ***************************************************************************/

/*
 *  Enums.h
 *  Created by Huy Phan on 9/12/2010.
 *
 *  Containing the common constants that are used throughout the project.
 */


#define HC_APP_INAME             @"IOXChess" /* The internal app-name        */
#define HC_APP_IVERSION          @"1.2"      /* The internal app-version     */

///////////////////////////////////////////////////////////////////////////////
//
//    Common constants
//
///////////////////////////////////////////////////////////////////////////////

#define HC_SETTINGS_VERSION      1     /* The Settings version               */
#define HC_AI_DIFFICULTY_DEFAULT 0     /* Valid range [0, 3]                 */
#define HC_MAX_MOVES_PER_GAME    200   /* Maximum number of moves per game   */

#define HC_SOUND_PATH            @"sounds"

#define HC_TABLE_ANIMATION_DURATION 1.0 /* Table switching duration (sec)    */

///////////////////////////////////////////////////////////////////////////////
//
//    Network (PlayXiangqi server) constants
//
///////////////////////////////////////////////////////////////////////////////

#define HC_SERVER_IP              @"games.playxiangqi.com"
#define HC_SERVER_PORT            80
#define HC_GUEST_PREFIX           @"Guest#"


///////////////////////////////////////////////////////////////////////////////
//
//    Common Enums
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Color for both Piece and Role.
 */
typedef enum ColorEnum_
{
    HC_COLOR_UNKNOWN = -1,
        // This type indicates the absense of color or role.
        // For example, it is used to indicate the player is not even
        // at the table.

    HC_COLOR_RED,   // RED color.
    HC_COLOR_BLACK, // BLACK color.

    HC_COLOR_NONE
        // NOTE: This type actually does not make sense for 'Piece',
        //       only for "Player". It is used to indicate the role of a player
        //       who is currently only observing the game, not playing.

} ColorEnum;

/**
 * Game's status.
 */
typedef enum GameStatusEnum_
{
    HC_GAME_STATUS_UNKNOWN = -1,

    HC_GAME_STATUS_IN_PROGRESS,
    HC_GAME_STATUS_RED_WIN,        // Game Over. Red won.
    HC_GAME_STATUS_BLACK_WIN,      // Game Over. Black won.
    HC_GAME_STATUS_DRAWN,          // Game Over. Drawn.
    HC_GAME_STATUS_TOO_MANY_MOVES  // Game Over. Too many moves.
} GameStatusEnum;

/**
 * Piece's Type.
 *
 *  King (K), Advisor (A), Elephant (E), chaRiot (R), Horse (H), 
 *  Cannons (C), Pawns (P).
 */
typedef enum PieceEnum_
{
    HC_PIECE_INVALID = 0,
    HC_PIECE_KING,                 // King (or General)
    HC_PIECE_ADVISOR,              // Advisor (or Guard, or Mandarin)
    HC_PIECE_ELEPHANT,             // Elephant (or Ministers)
    HC_PIECE_CHARIOT,              // Chariot ( Rook, or Car)
    HC_PIECE_HORSE,                // Horse ( Knight )
    HC_PIECE_CANNON,               // Canon
    HC_PIECE_PAWN                  // Pawn (or Soldier)
} PieceEnum;

/**
 * Possible AI engines.
 */
typedef enum AIEnum_
{
    HC_AI_XQWLight,
    HC_AI_HaQiKiD
} AIEnum;

/**
 * Highlight states of a Piece/Cell.
 */
typedef enum HighlightEnum_
{
    // !!!! *** DO NOT CHANGE THE NUMERIC VALUES  ***!!! //
    HC_HL_NONE     = 0,   // No highlight at all
    HC_HL_NORMAL   = 1,   // Normal highlight 
    HC_HL_ANIMATED = 2,   // Highlight after a piece has moved
    HC_HL_CHECKED  = 3    // Highlight while a King is checked (or threatened)
} HighlightEnum;

///////////////////////////////////////////////////////////////////////////////
//
//    Common Macros
//
///////////////////////////////////////////////////////////////////////////////

#define INVALID_MOVE         (-1)
#define TOSQUARE(row, col)   (16 * ((row) + 3) + ((col) + 3))
#define COLUMN(sq)           ((sq) % 16 - 3)
#define ROW(sq)              ((sq) / 16 - 3)

#define SRC(mv)              ((mv) & 255)
#define DST(mv)              ((mv) >> 8)
#define MOVE(sqSrc, sqDst)   ((sqSrc) + (sqDst) * 256)

////////////////////// END OF FILE ////////////////////////////////////////////
