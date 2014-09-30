//
// C++ Interface: hoxMove
//
// Description: The Move.
//
// Author: Huy Phan, (C) 2008-2009
//
// Created: 04/16/2009
//

#ifndef __INCLUDED_HOX_MOVE_H_
#define __INCLUDED_HOX_MOVE_H_

#include <string>
#include <list>
#include <cstdio>
#include "hoxEnums.h"

/**
 * Piece's Type.
 *
 *  King (K), Advisor (A), Elephant (E), chaRiot (R), Horse (H), 
 *  Cannons (C), Pawns (P).
 */
enum hoxPieceType
{
    hoxPIECE_INVALID = 0,
    hoxPIECE_KING,             // King (or General)
    hoxPIECE_ADVISOR,          // Advisor (or Guard, or Mandarin)
    hoxPIECE_ELEPHANT,         // Elephant (or Ministers)
    hoxPIECE_CHARIOT,          // Chariot ( Rook, or Car)
    hoxPIECE_HORSE,            // Horse ( Knight )
    hoxPIECE_CANNON,           // Canon
    hoxPIECE_PAWN              // Pawn (or Soldier)
};

/**
 * A Position of a Piece.
 */
class hoxPosition
{
public:
    char x;
    char y;

public:
    hoxPosition(char xx = -1, char yy = -1) : x(xx), y(yy) {}
    hoxPosition(const hoxPosition& pos);
    ~hoxPosition();

    hoxPosition& operator=(const hoxPosition& pos);
    bool operator==(const hoxPosition& pos) const;
    bool operator!=(const hoxPosition& pos) const;

    bool isValid() const;
    bool isInsidePalace(hoxColor color) const;
    bool isInsideCountry(hoxColor color) const;
};

/**
 * Representing a piece's info.
 */
class hoxPieceInfo
{
public:
    hoxPieceType   type;       // What type? (Canon, Soldier, ...)
    hoxColor       color;      // What color? (RED or BLACK)
    hoxPosition    position;   // Position on the Board.

    /**
     * NOTE: We do not store the "active/inactive" state since it is only
     *       needed by the referee who can maintain its own data-type... 
     */

    hoxPieceInfo()
         : type( hoxPIECE_INVALID )
         , color( hoxCOLOR_NONE )
         , position( -1, -1 )
        { }

    hoxPieceInfo(hoxPieceType t,
                 hoxColor     c,
                 hoxPosition  p)
         : type( t )
         , color( c )
         , position( p )
        { }

    hoxPieceInfo( const hoxPieceInfo& other )
        : type( other.type )
        , color( other.color )
        , position( other.position )
        { }

    bool isValid() const { return type != hoxPIECE_INVALID; }
};

typedef std::list<hoxPieceInfo> hoxPieceInfoList;

/**
 * Representing a MOVE.
 */
class hoxMove
{
public:
    hoxPieceInfo    piece;        // The Piece that moves.
    hoxPosition     newPosition;  // Position on the Board.

    hoxPieceInfo    capturedPiece; 
        /* The Piece being captured as a result of this Moves. 
         * This information is currently filled in by the Referee.
         */

    hoxMove() {}
    bool isValid() const { return piece.isValid(); }
    void setCapturedPiece( const hoxPieceInfo& captured ) 
        { capturedPiece = captured; }
    bool isAPieceCaptured() const { return capturedPiece.isValid(); }

    const std::string toString() const
    {
        char szBuffer[5];
        snprintf(szBuffer, sizeof(szBuffer), "%d%d%d%d", 
            piece.position.x, piece.position.y, newPosition.x, newPosition.y);
        return std::string(szBuffer);
    }
};

#endif /* __INCLUDED_HOX_MOVE_H_ */
