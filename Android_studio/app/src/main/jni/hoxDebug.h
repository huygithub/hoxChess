//
// C++ Interface: hoxDebug
//
// Description: 
//
//
// Author: Huy Phan, (C) 2008-2009
//
// Copyright: See COPYING file that comes with this distribution
//
//

/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! *
 *
 * CREDITS: This module is based on the implementation from wxWidgets.
 *
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! */

#ifndef __INCLUDED_HOX_DEBUG_H__
#define __INCLUDED_HOX_DEBUG_H__

#include <cstdio>        // NULL definition.

/**
 * This function is called whenever one of debugging macros fails (i.e.
 * condition is false in an assertion).
 *
 * Parameters:
 *  szFile and nLine - file name and line number of the ASSERT
 *  szFunc           - function name of the ASSERT, may be NULL (NB: ASCII)
 *  szCond           - text form of the condition which failed
 *  szMsg            - optional message explaining the reason
 */
void
hoxOnAssert( const char *szFile,
             int        nLine,
             const char *szFunc,
             const char *szCond,
             const char *szMsg = NULL );

/******************************************************************************
 *
 *    Debug Macros
 *
 ******************************************************************************/

/*  assert with additional message explaining its cause */

/*  compilers can give a warning (such as "possible unwanted ;") when using */
/*  the default definition of hoxASSERT_MSG so we provide an alternative */

#define hoxASSERT_MSG(cond, msg)                                           \
    if ( cond )                                                             \
        ;                                                                   \
    else                                                                    \
        hoxOnAssert(__FILE__, __LINE__, __FUNCTION__, #cond, msg)

/*  special form of assert: always triggers it (in debug mode) */
#define hoxFAIL hoxFAIL_MSG(NULL)

/*  FAIL with some message */
#define hoxFAIL_MSG(msg) hoxFAIL_COND_MSG("hoxAssertFailure", msg)

/*  FAIL with some message and a condition */
#define hoxFAIL_COND_MSG(cond, msg)                                          \
    hoxOnAssert(__FILE__, __LINE__,  __FUNCTION__, cond, msg)

/*  NB: the following macros also work in release mode! */

/*
  These macros must be used only in invalid situation: for example, an
  invalid parameter (e.g. a NULL pointer) is passed to a function. Instead of
  dereferencing it and causing core dump the function might try using
  CHECK( p != NULL ) or CHECK( p != NULL, return LogError("p is NULL!!") )
*/

/*  check that expression is true, "return" if not (also FAILs in debug mode) */
#define hoxCHECK(cond, rc)            hoxCHECK_MSG(cond, rc, NULL)

/*  as hoxCHECK but with a message explaining why we fail */
#define hoxCHECK_MSG(cond, rc, msg)   hoxCHECK2_MSG(cond, return rc, msg)

/*  check that expression is true, perform op if not */
#define hoxCHECK2(cond, op)           hoxCHECK2_MSG(cond, op, NULL)

/*  as hoxCHECK2 but with a message explaining why we fail */
#define hoxCHECK2_MSG(cond, op, msg)                                       \
    if ( cond )                                                           \
        ;                                                                 \
    else                                                                  \
    {                                                                     \
        hoxFAIL_COND_MSG(#cond, msg);                                      \
        op;                                                               \
    }                                                                     \
    struct hoxDummyCheckStruct /* just to force a semicolon */

/*  special form of hoxCHECK2: as hoxCHECK, but for use in void functions */
/*  */
/*  NB: there is only one form (with msg parameter) and it's intentional: */
/*      there is no other way to tell the caller what exactly went wrong */
/*      from the void function (of course, the function shouldn't be void */
/*      to begin with...) */
#define hoxCHECK_RET(cond, msg)       hoxCHECK2_MSG(cond, return, msg)


#endif /* __INCLUDED_HOX_DEBUG_H__ */
