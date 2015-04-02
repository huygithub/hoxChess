//
// C++ Interface: hoxReferee
//
// Description: The Referee.
//
// Author: Huy Phan, (C) 2008-2009
//
// Created: 04/16/2009
//

#ifndef __INCLUDED_HOX_REFEREE_H__
#define __INCLUDED_HOX_REFEREE_H__

#include <string>
#include "hoxMove.h"

namespace BoardInfoAPI
{
   class Board;
}

/**
 * The main Referee of this Application.
 */
class hoxReferee
{
public:
    hoxReferee();
    ~hoxReferee();

    void resetGame();
    bool validateMove( hoxMove&       move,
                       hoxGameStatus& status );

    bool isLastMoveCheck() const;

    void getGameState( hoxPieceInfoList& pieceInfoList,
                       hoxColor&         nextColor ) const;
    
    hoxColor getNextColor() const;

    hoxMove stringToMove( const std::string& sMove ) const;

/*private:*/
    bool _getPieceAtPosition( const hoxPosition& position, 
                              hoxPieceInfo&      pieceInfo ) const;

private:
    BoardInfoAPI::Board*  _board;  // Board-Info.
};

#endif /* __INCLUDED_HOX_REFEREE_H__ */
