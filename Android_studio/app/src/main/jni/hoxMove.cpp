//
// C++ Implementation: hoxMove
//
// Description: The Move
//
// Author: Huy Phan, (C) 2008-2009
//
// Created: 04/16/2009
//

#include "hoxMove.h"


/**************************************************************************
 *
 *                         hoxPosition
 *
 *************************************************************************/

hoxPosition::hoxPosition(const hoxPosition& pos)
{
    if ( &pos != this )
    {
        x = pos.x;
        y = pos.y;
    }
}

hoxPosition::~hoxPosition()
{
    // Doing nothing
}

hoxPosition& 
hoxPosition::operator=(const hoxPosition& pos)
{
    x = pos.x;
    y = pos.y;
    return *this;
}

bool
hoxPosition::operator==(const hoxPosition& pos) const
{
    return (x == pos.x && y == pos.y);
}

bool
hoxPosition::operator!=(const hoxPosition& pos) const
{
    return (x != pos.x || y != pos.y);
}

bool 
hoxPosition::isValid() const 
{ 
    return (x >= 0 && x <= 8 && y >= 0 && y <= 9); 
}

bool 
hoxPosition::isInsidePalace(hoxColor color) const 
{ 
    if (color == hoxCOLOR_BLACK)
    {
        return (x >= 3 && x <= 5 && y >= 0 && y <= 2); 
    }
    else  // Red?
    {
        return (x >= 3 && x <= 5 && y >= 7 && y <= 9); 
    }
}

// Is inside one's country (not yet cross the river)?
bool 
hoxPosition::isInsideCountry(hoxColor color) const 
{ 
    if (color == hoxCOLOR_BLACK)  return (y >= 0 && y <= 4);
    else  /* Red? */              return (y >= 5 && y <= 9);
}


/******************* END OF FILE *********************************************/
