//
// C++ Implementation: hoxReferee
//
// Description: The Referee.
//
// Author: Huy Phan, (C) 2008-2009
//
// Created: 04/16/2009
//

#include "hoxReferee.h"
#include "hoxLog.h"
#include "hoxDebug.h"
#include <list>
#include <algorithm>  // std::find

//************************************************************
//                          BLACK
//        0     1    2    3    4    5    6    7    8
//
//      +--------------+==============+--------------+
//  0   |  0 |  1 |  2 #  3 |  4 |  5 #  6 |  7 |  8 |
//      |--------------#--------------#--------------|
//  1   |  9 | 10 | 11 # 12 | 13 | 14 # 15 | 16 | 17 | 
//      |--------------#--------------#--------------|
//  2   | 18 | 19 | 20 # 21 | 22 | 23 # 24 | 25 | 26 | 
//      |--------------+==============+--------------|
//  3   | 27 | 28 | 29 | 30 | 31 | 32 | 33 | 34 | 35 | 
//      |--------------------------------------------|
//  4   | 36 | 37 | 38 | 39 | 40 | 41 | 42 | 43 | 44 |
//      |============================================| <-- The River 
//  5   | 45 | 46 | 47 | 48 | 49 | 50 | 51 | 52 | 53 |
//      |--------------------------------------------|
//  6   | 54 | 55 | 56 | 57 | 58 | 59 | 60 | 61 | 62 | 
//      |--------------+==============+--------------|
//  7   | 63 | 64 | 65 # 66 | 67 | 68 # 69 | 70 | 71 | 
//      |--------------#--------------#--------------|
//  8   | 72 | 73 | 74 # 75 | 76 | 77 # 78 | 79 | 80 | 
//      |--------------#--------------#--------------|
//  9   | 81 | 82 | 83 # 84 | 85 | 86 # 87 | 88 | 89 | 
//      +--------------+==============+--------------+
//
//        0     1    2    3    4    5    6    7    8
//                           RED
//************************************************************

namespace BoardInfoAPI
{
    /* Forward declarations */

    class Board;
    class Piece;

    /* Typedefs */

    typedef std::list<hoxPosition *> PositionList;
    typedef std::list<Piece* >       PieceList;

    /* ----- */

    /**
     * The generic Piece from which other specific Pieces (King, Pawn,...)
     * base on.  
     */
    class Piece
    {
    public:
        Piece() : m_info( hoxPIECE_INVALID, hoxCOLOR_NONE,
                          hoxPosition(-1, -1) ) { }
        Piece(hoxPieceType t) : m_info( t, hoxCOLOR_NONE, 
                                        hoxPosition(-1, -1) ) { }
        Piece(hoxPieceType t, hoxColor c) 
                              : m_info( t, c, hoxPosition(-1, -1) ) { }
        Piece(hoxPieceType t, hoxColor c, const hoxPosition& p)
                              : m_info( t, c, p ) { }
        virtual ~Piece() {}

        hoxPieceInfo  GetInfo()     const { return m_info; }

        hoxPieceType  GetType()     const { return m_info.type; }
        hoxColor GetColor()    const { return m_info.color; }
        hoxPosition   GetPosition() const { return m_info.position; }

        void SetPosition( const hoxPosition& pos ) { m_info.position = pos; }
        void SetBoard(Board* board) { m_board = board; }

        virtual bool IsValidMove(const hoxPosition& newPos) const;
        virtual bool DoesNextMoveExist() const;

        bool HasColor( hoxColor c ) const { return (m_info.color == c); }
        bool HasType(  hoxPieceType  t ) const { return (m_info.type  == t); }
        bool HasSameColumnAs( const Piece& other ) const
            { return (m_info.position.x == other.m_info.position.x); }

    protected:
        virtual bool CanMoveTo(const hoxPosition& newPos) const = 0;
        virtual void GetPotentialNextPositions(PositionList& positions) const {} // FIXME

    public: /* Static Public API */ 
        static bool Is_Inside_Palace( hoxColor color, const hoxPosition& position );

    protected:
        hoxPieceInfo  m_info;
        Board*        m_board;
    };

    /** 
     * KING Piece 
     */
    class KingPiece : public Piece
    {
    public:
        KingPiece(hoxColor c, const hoxPosition& p) 
            : Piece(hoxPIECE_KING, c, p) {}
        virtual bool CanMoveTo(const hoxPosition& newPos) const;
        virtual void GetPotentialNextPositions(PositionList& positions) const;
    };

    /** 
     * ADVISOR Piece 
     */
    class AdvisorPiece : public Piece
    {
    public:
        AdvisorPiece(hoxColor c, const hoxPosition& p) 
            : Piece(hoxPIECE_ADVISOR, c, p) {}
        virtual bool CanMoveTo(const hoxPosition& newPos) const;
        virtual void GetPotentialNextPositions(PositionList& positions) const;
    };

    /** 
     * ELEPHANT Piece 
     */
    class ElephantPiece : public Piece
    {
    public:
        ElephantPiece(hoxColor c, const hoxPosition& p) 
            : Piece(hoxPIECE_ELEPHANT, c, p) {}
        virtual bool CanMoveTo(const hoxPosition& newPos) const;
        virtual void GetPotentialNextPositions(PositionList& positions) const;
    };

    /** 
     * CHARIOT Piece 
     */
    class ChariotPiece : public Piece
    {
    public:
        ChariotPiece(hoxColor c, const hoxPosition& p) 
            : Piece(hoxPIECE_CHARIOT, c, p) {}
        virtual bool CanMoveTo(const hoxPosition& newPos) const;
        virtual void GetPotentialNextPositions(PositionList& positions) const;
    };

    /** 
     * HORSE Piece 
     */
    class HorsePiece : public Piece
    {
    public:
        HorsePiece(hoxColor c, const hoxPosition& p) 
            : Piece(hoxPIECE_HORSE, c, p) {}
        virtual bool CanMoveTo(const hoxPosition& newPos) const;
        virtual void GetPotentialNextPositions(PositionList& positions) const;
    };

    /** 
     * CANNON Piece 
     */
    class CannonPiece : public Piece
    {
    public:
        CannonPiece(hoxColor c, const hoxPosition& p) 
            : Piece(hoxPIECE_CANNON, c, p) {}
        virtual bool CanMoveTo(const hoxPosition& newPos) const;
        virtual void GetPotentialNextPositions(PositionList& positions) const;
    };

    /** 
     * PAWN Piece 
     */
    class PawnPiece : public Piece
    {
    public:
        PawnPiece(hoxColor c, const hoxPosition& p) 
            : Piece(hoxPIECE_PAWN, c, p) {}
        virtual bool CanMoveTo(const hoxPosition& newPos) const;
        virtual void GetPotentialNextPositions(PositionList& positions) const;
    };

    /** 
     * A single Cell in the 9x10 Xiangqi Board.
     */
    class Cell
    {
    public:
        hoxPosition  position;
        
        Piece*       pPiece;
            /* The piece that currrently occupies the cell.
             * NULL indicates that this cell is empty.
             */

        Cell() : position(-1, -1), pPiece( NULL ) { }
        bool IsEmpty() { return ( pPiece == NULL ); }    
    };

    /** 
     * The none-UI Board helping the referee to keep the game's state. 
     */
    class Board
    {
    public:
        Board( hoxColor nextColor = hoxCOLOR_NONE );
        ~Board();

        // ------------ Main Public API -------
        bool ValidateMove( hoxMove&       move,
                           hoxGameStatus& status );

        bool IsLastMoveCheck() const;

        void GetGameState( hoxPieceInfoList& pieceInfoList,
                           hoxColor&    nextColor );

        hoxColor GetNextColor() const { return m_nextColor; }

        bool GetPieceAtPosition( const hoxPosition& position, 
                                 hoxPieceInfo&      pieceInfo ) const;

        // ------------ Other Public API -------
        bool   Simulation_IsValidMove(const hoxMove& move);
        Piece* GetPieceAt( const hoxPosition& position ) const;
        bool   HasPieceAt( const hoxPosition& position ) const;

    private:
        void         _CreateNewGame();
        void         _AddNewPiece(Piece* piece);

        void         _SetPiece( Piece* piece );
        void         _UnsetPiece( Piece* piece );
        void         _CapturePiece( Piece* piece );

        void         _MovePieceTo( Piece* piece, const hoxPosition& newPos );
        void         _AddPiece(Piece* piece);
        void         _PutbackPiece(Piece* piece);

        Piece*       _RecordMove(const hoxMove& move);
        void         _UndoMove( const hoxMove& move, Piece* pCaptured );

        const Piece* _GetKing(hoxColor color) const;
        bool         _IsKingBeingChecked(hoxColor color) const;
        bool         _IsKingFaceKing() const;

        bool         _DoesNextMoveExist() const;

    private:
        PieceList      m_pieces;       // ACTIVE pieces
        PieceList      m_deadPieces;   // INACTIVE (dead) pieces
        Cell           m_cells[9][10];

        hoxColor       m_nextColor;
            /* Which side (RED or BLACK) will move next? */
    };

    /*********************
     * Other utility API *
     *********************/

    /** 
     * NOTE: This function is an example where we may want to consider using
     *       the 'smart' shared pointer (specifically, a shared-array pointer)
     *       such as that from Boost Libraries.
     */
    void PositionList_Clear( PositionList& positions );

} // namespace BoardInfoAPI



/* Import namespaces */

using namespace BoardInfoAPI;


//-----------------------------------------------------------------------------
// Board
//-----------------------------------------------------------------------------


Board::Board( hoxColor nextColor /* = hoxCOLOR_NONE */ )
        : m_nextColor( nextColor )
{
    /* Initialize Piece-Cells. */
    for ( int x = 0; x <= 8; ++x ) // horizontal
    {
        for ( int y = 0; y <= 9; ++y ) // vertical
        {
            m_cells[x][y].pPiece = NULL;
            m_cells[x][y].position = hoxPosition(-1,-1);
        }
    }

    /* Initialize Board. */
    _CreateNewGame();
}

Board::~Board()
{
    PieceList::const_iterator it;

    for ( it = m_pieces.begin(); it != m_pieces.end(); ++it )
    {
        delete (*it);
    }

    for ( it = m_deadPieces.begin(); it != m_deadPieces.end(); ++it )
    {
        delete (*it);
    }
}

/**
 * Create a brand new game by specifying the info of ALL pieces initially.
 */
void
Board::_CreateNewGame()
{
    hoxColor color;        // The current color.
    int      i;

    // --------- BLACK

    color = hoxCOLOR_BLACK;

    _AddNewPiece( new KingPiece(     color, hoxPosition(4, 0) ) );
    _AddNewPiece( new AdvisorPiece(  color, hoxPosition(3, 0) ) );
    _AddNewPiece( new AdvisorPiece(  color, hoxPosition(5, 0) ) );
    _AddNewPiece( new ElephantPiece( color, hoxPosition(2, 0) ) );
    _AddNewPiece( new ElephantPiece( color, hoxPosition(6, 0) ) );
    _AddNewPiece( new HorsePiece(    color, hoxPosition(1, 0) ) );
    _AddNewPiece( new HorsePiece(    color, hoxPosition(7, 0) ) );
    _AddNewPiece( new ChariotPiece(  color, hoxPosition(0, 0) ) );
    _AddNewPiece( new ChariotPiece(  color, hoxPosition(8, 0) ) );
    _AddNewPiece( new CannonPiece(   color, hoxPosition(1, 2) ) );
    _AddNewPiece( new CannonPiece(   color, hoxPosition(7, 2) ) );
    for ( i = 0; i < 10; i += 2 ) // 5 Pawns.
    {
        _AddNewPiece( new PawnPiece(   color, hoxPosition(i, 3) ) );
    }

    // --------- RED

    color = hoxCOLOR_RED;

    _AddNewPiece( new KingPiece(     color, hoxPosition(4, 9) ) );
    _AddNewPiece( new AdvisorPiece(  color, hoxPosition(3, 9) ) );
    _AddNewPiece( new AdvisorPiece(  color, hoxPosition(5, 9) ) );
    _AddNewPiece( new ElephantPiece( color, hoxPosition(2, 9) ) );
    _AddNewPiece( new ElephantPiece( color, hoxPosition(6, 9) ) );
    _AddNewPiece( new HorsePiece(    color, hoxPosition(1, 9) ) );
    _AddNewPiece( new HorsePiece(    color, hoxPosition(7, 9) ) );
    _AddNewPiece( new ChariotPiece(  color, hoxPosition(0, 9) ) );
    _AddNewPiece( new ChariotPiece(  color, hoxPosition(8, 9) ) );
    _AddNewPiece( new CannonPiece(   color, hoxPosition(1, 7) ) );
    _AddNewPiece( new CannonPiece(   color, hoxPosition(7, 7) ) );
    for ( i = 0; i < 10; i += 2 ) // 5 Pawns.
    {
        _AddNewPiece( new PawnPiece(   color, hoxPosition(i, 6) ) );
    }
}

void 
Board::GetGameState( hoxPieceInfoList& pieceInfoList,
                     hoxColor&    nextColor )
{
    pieceInfoList.clear();    // Clear the old info, if exists.

    /* Return all the ACTIVE Pieces. */
    for ( PieceList::const_iterator it = m_pieces.begin();
                                    it != m_pieces.end(); ++it )
    {
        pieceInfoList.push_back( hoxPieceInfo( (*it)->GetType(), 
                                               (*it)->GetColor(), 
                                               (*it)->GetPosition() ) );
    }

    /* Return the Next Color */
    nextColor = m_nextColor;
}

bool 
Board::GetPieceAtPosition( const hoxPosition& position, 
                           hoxPieceInfo&      pieceInfo ) const
{
    Piece* piece = this->GetPieceAt( position );
    if ( piece == NULL )
        return false;

    pieceInfo.type     = piece->GetType();
    pieceInfo.color    = piece->GetColor();
    pieceInfo.position = piece->GetPosition();

    return true;
}

/**
 * Put a given Piece on Board according to its position.
 */
void
Board::_SetPiece( Piece* piece )
{
    hoxCHECK_RET(piece, "Piece is NULL.");

    hoxPosition pos = piece->GetPosition();  // The piece's position.

    hoxCHECK_RET(pos.isValid(), "The piece's position is not valid.");

    hoxCHECK_RET( m_cells[pos.x][pos.y].pPiece == NULL, 
                 "The destination cell is not empty." );
    m_cells[pos.x][pos.y].pPiece = piece;
    m_cells[pos.x][pos.y].position = pos;
}

/**
 * Unset an existing piece from Board.
 */
void
Board::_UnsetPiece( Piece* piece )
{
    hoxCHECK_RET(piece, "Piece is NULL.");

    hoxPosition curPos = piece->GetPosition();  // The current position.

    hoxCHECK_RET(curPos.isValid(), "The current position is not valid.");
    hoxCHECK_RET(m_cells[curPos.x][curPos.y].pPiece == piece, 
                "There is no such as piece on Board.");

    /* 'Undo' old position, if any */
    m_cells[curPos.x][curPos.y].pPiece = NULL;
    m_cells[curPos.x][curPos.y].position = hoxPosition(-1,-1);
}

/**
 * Carry out the 'capture' action toward a given piece:
 *   + Unset the piece from the Board.
 *   + Move the piece from the ACTIVE list to the INACTIVE list.
 */
void 
Board::_CapturePiece( Piece* piece )
{
    _UnsetPiece( piece );

    PieceList::iterator found;

    found = std::find( m_pieces.begin(), m_pieces.end(), piece );
    hoxCHECK_RET( found != m_pieces.end(), "The piece is not ACTIVE." );

    found = std::find( m_deadPieces.begin(), m_deadPieces.end(), piece );
    hoxCHECK_RET( found == m_deadPieces.end(), "The piece is ready INACTIVE." );

    m_pieces.remove( piece );
    m_deadPieces.push_back( piece );
}

/**
 * Move an existing piece from its current position to a new position.
 *
 * @side-affects: The piece's position is modified as well.
 */
void
Board::_MovePieceTo( Piece*             piece, 
                     const hoxPosition& newPos )
{
    hoxCHECK_RET(piece, "Piece is NULL.");

    hoxPosition curPos = piece->GetPosition();  // The current position.

    hoxCHECK_RET(curPos.isValid(), "The current position is not valid.");
    hoxCHECK_RET(newPos.isValid(), "The new position is not valid.");

    _UnsetPiece( piece );
    piece->SetPosition( newPos );  // Adjust piece's position.
    _SetPiece( piece );
}

Piece* 
Board::GetPieceAt( const hoxPosition& position ) const
{
    if ( ! position.isValid() )
        return NULL;

    return m_cells[position.x][position.y].pPiece;
}

bool
Board::HasPieceAt( const hoxPosition& position ) const
{
    return ( NULL != this->GetPieceAt( position ) );
}

/**
 * Put a brand new piece on Board.
 */
void
Board::_AddNewPiece( Piece* piece )
{
    hoxCHECK_RET(piece, "Piece is NULL.");

    piece->SetBoard( this );
    this->_AddPiece( piece );
}

/**
 * Put a piece on Board.
 */
void
Board::_AddPiece( Piece* piece )
{
    hoxCHECK_RET(piece, "Piece is NULL.");

    m_pieces.push_back( piece );
    _SetPiece( piece );
}

void 
Board::_PutbackPiece( Piece* piece )
{
    PieceList::iterator found =
        std::find( m_deadPieces.begin(), m_deadPieces.end(), piece );
    hoxCHECK_RET( found != m_deadPieces.end(), "Dead piece should be found." );
    
    m_deadPieces.remove( piece );
    this->_AddPiece( piece );
}

/**
 * Record a valid Move.
 */
Piece*
Board::_RecordMove( const hoxMove& move )
{
    // Remove captured piece, if any.
    Piece* pCaptured = this->GetPieceAt(move.newPosition);
    if (pCaptured != NULL)
    {
        _CapturePiece( pCaptured );
    }

    // Move the piece to the new position.
    Piece* piece = this->GetPieceAt(move.piece.position);
    hoxASSERT_MSG(piece != NULL, 
                 "Piece must not be NULL after being validated");
    _MovePieceTo( piece, move.newPosition );

    return pCaptured;
}

void
Board::_UndoMove( const hoxMove& move,
                  Piece*         pCaptured )
{
    // Return the piece from its NEW position to its ORIGINAL position.
    Piece* piece = this->GetPieceAt(move.newPosition);
    hoxCHECK_RET(piece != NULL, "Piece must not be NULL after being validated");
    hoxCHECK_RET(piece->GetPosition() == move.newPosition,  // TODO: Move inside GetPieceAt()
                "The positions do not match.");
    _MovePieceTo( piece, move.piece.position );

    // "Un-capture" the captured piece, if any.
    if ( pCaptured != NULL )
    {
        pCaptured->SetPosition( move.newPosition );
        this->_PutbackPiece( pCaptured );
    }
}

const Piece*
Board::_GetKing( hoxColor color ) const
{
    for ( PieceList::const_iterator it = m_pieces.begin(); 
                                    it != m_pieces.end(); 
                                  ++it )
    {
        if (    (*it)->HasType( hoxPIECE_KING ) 
             && (*it)->HasColor( color ) )
        {
            return (*it);
        }
    }

    hoxFAIL_MSG( "A King of any color should exist." );
    return NULL;
}

/**
 * Check if a King (of a given color) is in CHECK position (being "checked").
 * @return true if the King is being checked.
 *         false, otherwise.
 */
bool 
Board::_IsKingBeingChecked( hoxColor color ) const
{
    /* Check if a King (of a given color) is being checked:.
     * This is done as follows:
     *  + For each piece of the 'enemy', check if the king's position
     *    is one of its valid move.
     */

    const Piece* pKing = _GetKing( color );
    hoxASSERT_MSG( pKing != NULL, "King must not be NULL" );

    for ( PieceList::const_iterator it = m_pieces.begin(); 
                                    it != m_pieces.end(); ++it )
    {
        if (    ( ! (*it)->HasColor( color ) )  // enemy?
             && (*it)->IsValidMove( pKing->GetPosition() ) )
        {
            return true;
        }
    }

    return false;  // Not in "checked" position.
}

// Check if one king is facing another.
bool 
Board::_IsKingFaceKing() const
{
    const Piece* blackKing = _GetKing( hoxCOLOR_BLACK );
    hoxASSERT_MSG( blackKing != NULL, "Black King must not be NULL" );

    const Piece* redKing = _GetKing( hoxCOLOR_RED );
    hoxASSERT_MSG( redKing != NULL, "Red King must not be NULL" );

    if ( ! blackKing->HasSameColumnAs( *redKing ) ) // not the same column.
        return false;  // Not facing

    // If they are in the same column, check if there is any piece in between.
    for ( PieceList::const_iterator it = m_pieces.begin(); 
                                    it != m_pieces.end(); 
                                  ++it )
    {
        if ( (*it) == blackKing || (*it) == redKing )
            continue;

        if ( (*it)->HasSameColumnAs( *redKing ) )  // in between Kings?
            return false;  // Not facing
    }

    return true;  // Facing
}

bool
Board::ValidateMove( hoxMove&       move,
                     hoxGameStatus& status )
{
    const char* FNAME = "Board::ValidateMove";

    /* Check for 'turn' */

    if ( move.piece.color != m_nextColor )
        return false; // Error! Wrong turn.

    /* Perform a basic validation */

    Piece* piece = this->GetPieceAt( move.piece.position );
    if ( piece == NULL )
        return false;

    if ( ! piece->IsValidMove( move.newPosition ) )
        return false;

    /* At this point, the Move is valid.
     * Record this move (to validate future Moves).
     */

    Piece* pCaptured = _RecordMove( move );

    /* If the Move results in its own check-mate OR
     * there is a KING-face-KING problem...
     * then it is invalid and must be undone.
     */
    if (   _IsKingBeingChecked( move.piece.color )
        || _IsKingFaceKing() )
    {
        _UndoMove(move, pCaptured);
        return false;
    }

    /* Return the captured-piece, if any */
    move.setCapturedPiece( pCaptured != NULL ? pCaptured->GetInfo() 
                                             : hoxPieceInfo() /* 'Empty' piece */ );

    /* Set the next-turn. */
    m_nextColor = ( m_nextColor == hoxCOLOR_RED ? hoxCOLOR_BLACK
                                                : hoxCOLOR_RED );

    /* Check for end game:
     * ------------------
     *   Checking if this Move makes the Move's Player
     *   the winner of the game. The step is done by checking to see if the
     *   opponent can make ANY valid Move at all.
     *   If not, then the opponent has just lost the game.
     */

    if ( ! _DoesNextMoveExist() )
    {
        hoxLog(LOG_DEBUG, "%s: The game is over.", FNAME);
        status = (  m_nextColor == hoxCOLOR_BLACK ? hoxGAME_STATUS_RED_WIN
                                                  : hoxGAME_STATUS_BLACK_WIN );
    }
    else
    {
        status = hoxGAME_STATUS_IN_PROGRESS;
    }

    return true;
}

bool
Board::IsLastMoveCheck() const
{
    return _IsKingBeingChecked( m_nextColor );
}

bool
Board::Simulation_IsValidMove( const hoxMove& move )
{
    Piece* piece = this->GetPieceAt( move.piece.position );
    if ( piece == NULL )
        return false;

    if ( ! piece->IsValidMove( move.newPosition ) )
        return false;

    /* Simulate the Move. */

    Piece* pCaptured = _RecordMove( move );

    /* If the Move ends up results in its own check-mate,
     * then it is invalid and must be undone.
     */
    bool beingChecked = _IsKingBeingChecked( piece->GetColor() );
    bool areKingsFacing = _IsKingFaceKing();

    _UndoMove( move, pCaptured );

    if ( beingChecked || areKingsFacing )
    {
        return false;  // Not a good move. Process the next move.
    }

    return true;  // It is a valid Move.
}

bool 
Board::_DoesNextMoveExist() const
{
    /* Go through all Pieces of the 'next' color.
     * If any piece can move 'next', then Board can as well.
     */

    for ( PieceList::const_iterator it = m_pieces.begin(); 
                                    it != m_pieces.end(); ++it )
    {
        if (    (*it)->HasColor( m_nextColor )
             && (*it)->DoesNextMoveExist() )
        {
                return true;
        }
    }

    return false;
}

//-----------------------------------------------------------------------------
// Piece
//-----------------------------------------------------------------------------

bool 
Piece::IsValidMove( const hoxPosition& newPos ) const
{
    if ( ! this->CanMoveTo( newPos ) )
        return false;

    /* If this is an Capture-Move, make sure the captured piece 
     * is an 'enemy'. 
     */
    const Piece* capturedPiece = m_board->GetPieceAt( newPos );
    if (   capturedPiece != NULL 
        && capturedPiece->HasColor( m_info.color ) )
    {
        return false; // Capture your OWN piece! Not legal.
    }

    return true;
}

bool 
Piece::DoesNextMoveExist() const
{
    bool  nextMoveExits = false;

    /* Generate all potential 'next' positions. */

    PositionList positions;  // all potential 'next' positions.

    this->GetPotentialNextPositions( positions );

    /* For each potential 'next' position, check if this piece can 
     * actually move there. 
     */

    hoxMove move;
    move.piece = this->m_info;

    for ( PositionList::const_iterator it = positions.begin();
                                       it != positions.end(); ++it )
    {
        if ( ! (*it)->isValid() ) continue;

        move.newPosition = *(*it);
        
        /* Ask the Board to validate this Move in Simulation mode. */
        if ( m_board->Simulation_IsValidMove( move ) )
        {
            nextMoveExits = true;
            break;
        }
    }

    PositionList_Clear( positions ); // Release memory.

    return nextMoveExits;
}

//-----------------------------------------------------------------------------
// KingPiece
//-----------------------------------------------------------------------------

bool 
KingPiece::CanMoveTo( const hoxPosition& newPos ) const
{
    const hoxColor myColor = m_info.color;
    const hoxPosition   curPos = m_info.position;

    // Within the palace...?
    if ( !newPos.isInsidePalace(myColor) ) 
        return false;

    // Is a 1-cell horizontal or vertical move?
    if ( ! (   (abs(newPos.x - curPos.x) == 1 && newPos.y == curPos.y)
            || (newPos.x == curPos.x && abs(newPos.y - curPos.y) == 1)) )
    {
        return false;
    }

    // NOTE: The caller will check if the captured piece (if any) is allowed.

    return true;
}

void 
KingPiece::GetPotentialNextPositions(PositionList& positions) const
{
    const hoxPosition p = this->GetPosition();
    
    positions.clear();

    // ... Simply use the 4 possible positions.
    positions.push_back( new hoxPosition(p.x, p.y-1) );
    positions.push_back( new hoxPosition(p.x, p.y+1) );
    positions.push_back( new hoxPosition(p.x-1, p.y) );
    positions.push_back( new hoxPosition(p.x+1, p.y) );
}

//-----------------------------------------------------------------------------
// AdvisorPiece
//-----------------------------------------------------------------------------

bool 
AdvisorPiece::CanMoveTo( const hoxPosition& newPos ) const
{
    const hoxColor myColor = m_info.color;
    const hoxPosition   curPos = m_info.position;

    // Within the palace...?
    if ( !newPos.isInsidePalace(myColor) ) 
        return false;

    // Is a 1-cell diagonal move?
    if (! (abs(newPos.x - curPos.x) == 1 && abs(newPos.y - curPos.y) == 1) )
        return false;

    // NOTE: The caller will check if the captured piece (if any) is allowed.

    return true;

}

void 
AdvisorPiece::GetPotentialNextPositions(PositionList& positions) const
{
    const hoxPosition p = this->GetPosition();
    
    positions.clear();

    // ... Simply use the 4 possible positions.
    positions.push_back(new hoxPosition(p.x-1, p.y-1));
    positions.push_back(new hoxPosition(p.x-1, p.y+1));
    positions.push_back(new hoxPosition(p.x+1, p.y-1));
    positions.push_back(new hoxPosition(p.x+1, p.y+1));
}

//-----------------------------------------------------------------------------
// ElephantPiece
//-----------------------------------------------------------------------------

bool 
ElephantPiece::CanMoveTo( const hoxPosition& newPos ) const
{
    const hoxColor myColor = m_info.color;
    const hoxPosition curPos = m_info.position;

    // Within the country...?
    if ( !newPos.isInsideCountry(myColor) ) // crossed the river/border?
        return false;

    // Is a 2-cell diagonal move?
    if (! (abs(newPos.x - curPos.x) == 2 && abs(newPos.y - curPos.y) == 2) )
        return false;

    // ... and there is no piece on the diagonal (to hinder the move)
    hoxPosition centerPos((curPos.x+newPos.x)/2, (curPos.y+newPos.y)/2);
    if ( m_board->HasPieceAt(centerPos) )
        return false;

    // NOTE: The caller will check if the captured piece (if any) is allowed.

    return true;
}

void 
ElephantPiece::GetPotentialNextPositions(PositionList& positions) const
{
    const hoxPosition p = this->GetPosition();
    
    positions.clear();

    // ... Simply use the 4 possible positions.
    positions.push_back(new hoxPosition(p.x-2, p.y-2));
    positions.push_back(new hoxPosition(p.x-2, p.y+2));
    positions.push_back(new hoxPosition(p.x+2, p.y-2));
    positions.push_back(new hoxPosition(p.x+2, p.y+2));
}

//-----------------------------------------------------------------------------
// ChariotPiece
//-----------------------------------------------------------------------------

bool 
ChariotPiece::CanMoveTo( const hoxPosition& newPos ) const
{
    const hoxPosition   curPos = m_info.position;

    bool bIsValidMove = false;

    // Is a horizontal or vertical move?
    if (! (  (newPos.x != curPos.x && newPos.y == curPos.y)
          || (newPos.x == curPos.x && newPos.y != curPos.y) ) )
    {
        return false;
    }

    // Make sure there is no piece that hinders the move from the current
    // position to the new.
    //
    //          top
    //           ^
    //           |
    //  left <-- +  ---> right
    //           |
    //           v
    //         bottom
    // 

    PositionList middlePieces;
    hoxPosition* pPos = 0;
    int i;

    // If the new position is on TOP.
    if (newPos.y < curPos.y)
    {
        for (i = newPos.y+1; i < curPos.y; ++i)
        {
            pPos = new hoxPosition(curPos.x, i);
            middlePieces.push_back( pPos );
        }
    }
    // If the new position is on the RIGHT.
    else if (newPos.x > curPos.x)
    {
        for (i = curPos.x+1; i < newPos.x; ++i)
        {
            pPos = new hoxPosition(i, curPos.y);
            middlePieces.push_back(pPos);
        }
    }
    // If the new position is at the BOTTOM.
    else if (newPos.y > curPos.y)
    {
        for (i = curPos.y+1; i < newPos.y; ++i)
        {
            pPos = new hoxPosition(curPos.x, i);
            middlePieces.push_back(pPos);
        }
    }
    // If the new position is on the LEFT.
    else if (newPos.x < curPos.x)
    {
        for (i = newPos.x+1; i < curPos.x; ++i)
        {
            pPos = new hoxPosition(i, curPos.y);
            middlePieces.push_back(pPos);
        }
    }

    // Check that no middle pieces exist from the new to the current position.
    for ( PositionList::const_iterator it = middlePieces.begin();
                                       it != middlePieces.end();
                                     ++it )
    {
        if ( m_board->HasPieceAt( *(*it) ) )
        {
            goto cleanup;  // return with 'invalid' move
        }
    }

    // NOTE: The caller will check if the captured piece (if any) is allowed.

    // Finally, return 'valid' move.
    bIsValidMove = true;

cleanup:
    PositionList_Clear( middlePieces ); // Release memory.

    return bIsValidMove;
}

void 
ChariotPiece::GetPotentialNextPositions(PositionList& positions) const
{
    const hoxPosition p = this->GetPosition();
    
    positions.clear();

    // ... Horizontally.
    for ( int x = 0; x <= 8; ++x )
    {
        if ( x == p.x ) continue;
        positions.push_back(new hoxPosition(x, p.y));

    }

    // ... Vertically
    for ( int y = 0; y <= 9; ++y )
    {
        if ( y == p.y ) continue;
        positions.push_back(new hoxPosition(p.x, y));

    }
}

//-----------------------------------------------------------------------------
// HorsePiece
//-----------------------------------------------------------------------------

bool 
HorsePiece::CanMoveTo( const hoxPosition& newPos ) const
{
    const hoxPosition   curPos = m_info.position;

    /* Is a 2-1-rectangle move? */

    // Make sure there is no piece that hinders the move if the move
    // start from one of the four 'neighbors' below.
    //
    //          top
    //           ^
    //           |
    //  left <-- +  ---> right
    //           |
    //           v
    //         bottom
    // 

    hoxPosition neighbor;

    // If the new position is on TOP.
    if ( (curPos.y - 2) == newPos.y && abs(newPos.x - curPos.x) == 1 )
    {
        neighbor = hoxPosition(curPos.x, curPos.y-1); 
    }
    // If the new position is at the BOTTOM.
    else if ( (curPos.y + 2) == newPos.y && abs(newPos.x - curPos.x) == 1 )
    {
        neighbor = hoxPosition(curPos.x, curPos.y+1); 
    }
    // If the new position is on the RIGHT.
    else if ( (curPos.x + 2) == newPos.x && abs(newPos.y - curPos.y) == 1 )
    {
        neighbor = hoxPosition(curPos.x+1, curPos.y); 
    }
    // If the new position is on the LEFT.
    else if ( (curPos.x - 2) == newPos.x && abs(newPos.y - curPos.y) == 1 )
    {
        neighbor = hoxPosition(curPos.x-1, curPos.y); 
    }
    else
    {
        return false;
    }

    // If the neighbor exists, then the move is invalid.
    if ( m_board->HasPieceAt( neighbor ) ) 
        return false;

    // NOTE: The caller will check if the captured piece (if any) is allowed.

    return true;
}

void 
HorsePiece::GetPotentialNextPositions(PositionList& positions) const
{
    const hoxPosition p = this->GetPosition();
    
    positions.clear();

    // ... Check for the 8 possible positions.
    positions.push_back(new hoxPosition(p.x-1, p.y-2));
    positions.push_back(new hoxPosition(p.x-1, p.y+2));
    positions.push_back(new hoxPosition(p.x-2, p.y-1));
    positions.push_back(new hoxPosition(p.x-2, p.y+1));
    positions.push_back(new hoxPosition(p.x+1, p.y-2));
    positions.push_back(new hoxPosition(p.x+1, p.y+2));
    positions.push_back(new hoxPosition(p.x+2, p.y-1));
    positions.push_back(new hoxPosition(p.x+2, p.y+1));
}

//-----------------------------------------------------------------------------
// CannonPiece
//-----------------------------------------------------------------------------

bool 
CannonPiece::CanMoveTo( const hoxPosition& newPos ) const
{
    const hoxColor myColor = m_info.color;
    const hoxPosition   curPos = m_info.position;

    bool bIsValidMove = false;

    // Is a horizontal or vertical move?
    if (! (  (newPos.x != curPos.x && newPos.y == curPos.y)
          || (newPos.x == curPos.x && newPos.y != curPos.y) ) )
    {
        return false;
    }

    // Make sure there is no piece that hinders the move from the current
    // position to the new.
    //
    //          top
    //           ^
    //           |
    //  left <-- +  ---> right
    //           |
    //           v
    //         bottom
    // 

    PositionList middlePieces;
    hoxPosition* pPos = 0;
    int i;

    // If the new position is on TOP.
    if (newPos.y < curPos.y)
    {
        for (i = newPos.y+1; i < curPos.y; ++i)
        {
            pPos = new hoxPosition(curPos.x, i);
            middlePieces.push_back(pPos);
        }
    }
    // If the new position is on the RIGHT.
    else if (newPos.x > curPos.x)
    {
        for (i = curPos.x+1; i < newPos.x; ++i)
        {
            pPos = new hoxPosition(i, curPos.y);
            middlePieces.push_back(pPos);
        }
    }
    // If the new position is at the BOTTOM.
    else if (newPos.y > curPos.y)
    {
        for (i = curPos.y+1; i < newPos.y; ++i)
        {
            pPos = new hoxPosition(curPos.x, i);
            middlePieces.push_back(pPos);
        }
    }
    // If the new position is on the LEFT.
    else if (newPos.x < curPos.x)
    {
        for (i = newPos.x+1; i < curPos.x; ++i)
        {
            pPos = new hoxPosition(i, curPos.y);
            middlePieces.push_back(pPos);
        }
    }

    // Check to see how many middle pieces exist from the 
    // new to the current position.
    int numMiddle = 0;
    for ( PositionList::const_iterator it = middlePieces.begin();
                                       it != middlePieces.end(); ++it )
    {
        if ( m_board->HasPieceAt( *(*it)) )
            ++numMiddle;
    }

    // If there are more than 1 middle piece, return 'invalid'.
    if (numMiddle > 1)
    {
        goto cleanup;  // return with 'invalid'
    }
    // If there is exactly 1 middle pieces, this must be a Capture Move.
    else if (numMiddle == 1)
    {
        const Piece* capturedPiece = m_board->GetPieceAt(newPos);
        if ( !capturedPiece || capturedPiece->GetColor() == myColor) 
        {
            goto cleanup;  // return with 'invalid'
        }
    }
    // If there is no middle piece, make sure that no piece is captured.
    else   // numMiddle = 0
    {
        if ( m_board->HasPieceAt(newPos) ) 
        {
            goto cleanup;  // return with 'invalid'
        }
    }

    // Finally, return 'valid' move.
    bIsValidMove = true;

cleanup:
    PositionList_Clear( middlePieces ); // Release memory.

    return bIsValidMove;
}

void 
CannonPiece::GetPotentialNextPositions(PositionList& positions) const
{
    const hoxPosition p = this->GetPosition();
    
    positions.clear();

    // ... Horizontally.
    for ( int x = 0; x <= 8; ++x )
    {
        if ( x == p.x ) continue;
        positions.push_back(new hoxPosition(x, p.y));

    }

    // ... Vertically
    for ( int y = 0; y <= 9; ++y )
    {
        if ( y == p.y ) continue;
        positions.push_back(new hoxPosition(p.x, y));

    }
}

//-----------------------------------------------------------------------------
// PawnPiece
//-----------------------------------------------------------------------------

bool 
PawnPiece::CanMoveTo( const hoxPosition& newPos ) const
{
    const hoxColor myColor = m_info.color;
    const hoxPosition   curPos = m_info.position;

    bool bMoveValid = false;

    // Within the country...?
    if ( newPos.isInsideCountry(myColor) ) 
    {
        // Can only move up.
        if ( newPos.x == curPos.x && 
           (   (myColor == hoxCOLOR_BLACK && newPos.y == curPos.y+1)
             || (myColor == hoxCOLOR_RED && newPos.y == curPos.y-1)) )
        {
            bMoveValid = true;
        }
    }
    // Outside the country (alread crossed the 'river')
    else
    {
        // Only horizontally (LEFT or RIGHT)
        // ... or 1-way-UP vertically.
        if ( ( newPos.y == curPos.y 
                    && abs(newPos.x - curPos.x) == 1 )
            || (newPos.x == curPos.x
                  && ( (myColor == hoxCOLOR_BLACK && newPos.y == curPos.y+1)
                    || (myColor == hoxCOLOR_RED && newPos.y == curPos.y-1))) )
        {
            bMoveValid = true;
        }
    }

    // *** If move is still invalid, return 'invalid'.
    if ( ! bMoveValid )
        return false;

    // NOTE: The caller will check if the captured piece (if any) is allowed.

    return true;
}

void 
PawnPiece::GetPotentialNextPositions(PositionList& positions) const
{
    const hoxPosition p = this->GetPosition();
    
    positions.clear();

    // ... Simply use the 4 possible positions.
    positions.push_back(new hoxPosition(p.x, p.y-1));
    positions.push_back(new hoxPosition(p.x, p.y+1));
    positions.push_back(new hoxPosition(p.x-1, p.y));
    positions.push_back(new hoxPosition(p.x+1, p.y));
}

//-----------------------------------------------------------------------------
// Other utility API
//-----------------------------------------------------------------------------

void
BoardInfoAPI::PositionList_Clear( PositionList& positions )
{
    for ( PositionList::const_iterator it = positions.begin();
                                       it != positions.end(); ++it )
    {
        delete (*it);
    }
}


/**************************************************************************
 *
 *                         hoxReferee
 *
 *************************************************************************/

hoxReferee::hoxReferee()
        : _board( NULL )
{
    this->resetGame();
}

hoxReferee::~hoxReferee()
{
    delete _board;
}

void
hoxReferee::resetGame()
{
    delete _board;   // Delete the old Board, if exists.

    _board = new Board( hoxCOLOR_RED /* next-color */ );
}

bool 
hoxReferee::validateMove( hoxMove&       move,
                          hoxGameStatus& status )
{
    hoxCHECK_MSG(_board, false, "The Board is NULL.");
    return _board->ValidateMove( move, status );
}

bool
hoxReferee::isLastMoveCheck() const
{
    return _board->IsLastMoveCheck();
}

void 
hoxReferee::getGameState( hoxPieceInfoList& pieceInfoList,
                          hoxColor&         nextColor ) const
{
    hoxCHECK_RET(_board, "The Board is NULL.");
    return _board->GetGameState( pieceInfoList, nextColor );
}

hoxColor 
hoxReferee::getNextColor() const
{
    hoxCHECK_MSG(_board, hoxCOLOR_UNKNOWN, "The Board is NULL.");
    return _board->GetNextColor();
}

hoxMove
hoxReferee::stringToMove( const std::string& sMove ) const
{
    const char* FNAME = "hoxReferee::stringToMove";
    hoxMove move;

    /* NOTE: Move-string has the format of "xyXY" */

    if ( sMove.size() != 4 )
    {
        return hoxMove();  // Error: return an invalid Move.
    }

    move.piece.position.x = sMove[0] - '0';
    move.piece.position.y = sMove[1] - '0';
    move.newPosition.x    = sMove[2] - '0';
    move.newPosition.y    = sMove[3] - '0';

    /* Lookup a Piece based on "fromPosition". */

    if ( ! _getPieceAtPosition( move.piece.position, 
                                move.piece ) )
    {
        hoxLog(LOG_INFO, "%s: Failed to locate piece at the position.", FNAME);
        return hoxMove();  // Error: return an invalid Move.
    }

    return move;
}

bool 
hoxReferee::_getPieceAtPosition( const hoxPosition& position, 
                                 hoxPieceInfo&      pieceInfo ) const
{
    hoxCHECK_MSG(_board, false, "The Board is NULL.");
    return _board->GetPieceAtPosition( position, pieceInfo );
}

/******************* END OF FILE *********************************************/
