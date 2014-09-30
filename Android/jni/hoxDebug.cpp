//
// C++ Implementation: hoxDebug
//
// Description: 
//
//
// Author: Huy Phan, (C) 2008-2009
//
// Copyright: See COPYING file that comes with this distribution
//
//

#include "hoxDebug.h"
#include "hoxLog.h"

void
hoxOnAssert( const char *szFile,
             int        nLine,
             const char *szFunc,
             const char *szCond,
             const char *szMsg /* = NULL */ )
{
    if ( szMsg != NULL )
    {
        hoxLog(LOG_ERROR, "%s:%d:%s: %s assertion failed. Msg = [%s]",
            szFile, nLine, szFunc, szCond, szMsg);
    }
    else
    {
        hoxLog(LOG_ERROR, "%s:%d:%s: %s assertion failed.",
            szFile, nLine, szFunc, szCond);
    }
}

/******************* END OF FILE *********************************************/
