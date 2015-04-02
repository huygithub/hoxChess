//
// C++ Interface: hoxEnums
//
// Description: Containing common constants used throughout the application.
//
// Author: Huy Phan, (C) 2008-2009
//
// Created: 04/16/2009
//

#ifndef __INCLUDED_HOX_ENUMS_H__
#define __INCLUDED_HOX_ENUMS_H__

/******************************************************************
 * Useful macros
 */

#define SEC2USEC(s)               ((s)*1000000LL)

/******************************************************************
 * Various constants defined the server's behaviors.
 */

#define hoxPERPETUAL_CHECKS_MAX   6
        /* The number of consecutive checks allowed. */

#define hoxEFFECTIVE_MOVES_MAX    200
        /* The number of 'effective' (i.e., non-check) Moves allowed before
         * the Game is declared to be a Draw.
         */

/******************************************************************/
/******************************************************************/

/**
 * Results (... Return-Code)
 */
enum hoxResult
{
    hoxRC_UNKNOWN = -1,

    hoxRC_OK = 0,
    hoxRC_ERR,          // A generic error.

    hoxRC_TIMEOUT,      // A timeout error.
    hoxRC_EINTR,        // A interrupted error (e.g., caused by st_thread_interrupt).
    hoxRC_HANDLED,      // Something (request, event,...) has been handled.
    hoxRC_CLOSED,       // Something (socket,...) has been closed.
    hoxRC_NOT_VALID,    // Something is not valid.
    hoxRC_NOT_FOUND,    // Something is not found.
    hoxRC_NOT_ALLOWED,  // Some action is not allowed.
    hoxRC_NOT_SUPPORTED // Something is not supported.
};

/**
 * Request types comming from the remote Players.
 */
enum hoxRequestType
{
    hoxREQUEST_UNKNOWN = -1,

    hoxREQUEST_HELLO,
        /* Get server's info */

    hoxREQUEST_REGISTER,
        /* Register (create) a new account */

    hoxREQUEST_LOGIN,
    hoxREQUEST_LOGOUT,
    hoxREQUEST_SHUTDOWN,
    hoxREQUEST_POLL,
    hoxREQUEST_MOVE,
    hoxREQUEST_LIST,
    hoxREQUEST_NEW,
    hoxREQUEST_JOIN,
    hoxREQUEST_LEAVE,
    hoxREQUEST_UPDATE,

    hoxREQUEST_RESIGN,
        /* Resign the current game */

    hoxREQUEST_DRAW,
        /* Offer a Draw request the current game */

    hoxREQUEST_RESET,
        /* Reset the Table */

    hoxREQUEST_E_JOIN,
        /* Event generated from a Table that a new Player just joined */

    hoxREQUEST_E_END,
        /* Event generated from a Table that the game has ended. */

    hoxREQUEST_E_SCORE,
        /* Event generated to inform of a player's new Score. */

    hoxREQUEST_I_PLAYERS,
        /* Info about the list of Players */

    hoxREQUEST_I_TABLE,
        /* Info about a Table */

    hoxREQUEST_I_MOVES,
        /* Info about the "past/history" Moves */

    hoxREQUEST_INVITE,
        /* Invite request for a given Player */

    hoxREQUEST_PLAYER_INFO,
        /* Info request for a given Player */

    hoxREQUEST_PLAYER_STATUS,
        /* Event generated from a Player when his Status is changed. */

    hoxREQUEST_MSG,
        /* Message generated (incoming) from a physical Table */

    hoxREQUEST_PING,
        /* Keep-Alive message: do nothing but keep the session alive */

    hoxREQUEST_DB_PLAYER_PUT,
        /* Put (create) into Database a new Player's info */

    hoxREQUEST_DB_PLAYER_GET,
        /* Get Database Player's info */

    hoxREQUEST_DB_PLAYER_SET,
        /* Set Database Player's info */

    hoxREQUEST_DB_PASSWORD_SET,
        /* Set Database Player's NEW password */

          /* HTTP requests */
    hoxREQUEST_HTTP_GET,
    hoxREQUEST_HTTP_POST,

    hoxREQUEST_LOG,
        /* Log a message remotely to DBAgent */
};

/**
 * Player Types.
 */
enum hoxPlayerType
{
    hoxPLAYER_TYPE_NORMAL,
    hoxPLAYER_TYPE_GUEST
};

/**
 * Client Types.
 */
enum hoxClientType
{
    hoxCLIENT_TYPE_HOXCHESS,
    hoxCLIENT_TYPE_HTTP,     // Ajax
    hoxCLIENT_TYPE_FLASH,    // ChessWhiz
    hoxCLIENT_TYPE_TEST      // hoxTest
};

/**
 * Session Types.
 */
enum hoxSessionType
{
    hoxSESSION_TYPE_PERSISTENT,
    hoxSESSION_TYPE_FLASH,
    hoxSESSION_TYPE_POLLING
};

/**
 * Session States.
 */
enum hoxSessionState
{
    hoxSESSION_STATE_ACTIVE,
    hoxSESSION_STATE_DISCONNECT,
    hoxSESSION_STATE_SHUTDOWN
};

/**
 * Game's Group.
 */
enum hoxGameGroup
{
    hoxGAME_GROUP_PUBLIC,
    hoxGAME_GROUP_PRIVATE
};

/**
 * Game's Type.
 */
enum hoxGameType
{
    hoxGAME_TYPE_UNKNOWN = -1,

    hoxGAME_TYPE_RATED,
    hoxGAME_TYPE_NONRATED,
    hoxGAME_TYPE_SOLO     // vs. COMPUTER
};

/**
 * Color for both Piece and Role.
 */
enum hoxColor
{
    hoxCOLOR_UNKNOWN = -1,
       /* This type indicates the absense of Color or Role.
        * For example, it is used to indicate the player is not even
        * at the table.
        */

    hoxCOLOR_RED,   // RED
    hoxCOLOR_BLACK, // BLACK

    hoxCOLOR_NONE
       /* NOTE: This type actually does not make sense for 'Piece',
        *       only for "Player". It is used to indicate the role
        *       of a player who is currently only observing the game,
        *       not playing.
        */
};

/**
 * Game's status.
 */
enum hoxGameStatus
{
    hoxGAME_STATUS_UNKNOWN = -1,

    hoxGAME_STATUS_OPEN = 0,    // Open but not enough Player.
    hoxGAME_STATUS_READY,       // Enough (2) players, waiting for 1st Move.
    hoxGAME_STATUS_IN_PROGRESS, // At least 1 Move has been made.
    hoxGAME_STATUS_RED_WIN,     // Game Over: Red won.
    hoxGAME_STATUS_BLACK_WIN,   // Game Over: Black won.
    hoxGAME_STATUS_DRAWN        // Game Over: Drawn.
};

/**
 * Network constants.
 */
enum hoxNetworkContant
{
    /*
     * !!! Do not change the values nor orders of the following !!!
     */

    SESSION_EXPIRY           = (10 * 60),   // 10-minute

    PERSIST_READ_TIMEOUT     = (60 * 60),   // 60-minute
    POLL_READ_TIMEOUT        = (10 * 60),   // 10-minute

    hoxNETWORK_MAX_MSG_SIZE  = ( 4 * 1024 ), // 4-KByte buffer
};

#endif /* __INCLUDED_HOX_ENUMS_H__ */
