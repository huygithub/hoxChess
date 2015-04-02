//
// C++ Interface: hoxLog
//
// Description: The Log module.
//
// Author: Huy Phan, (C) 2008-2009
//
// Created: 04/14/2009
//

#ifndef __INCLUDED_HOX_LOG_H__
#define __INCLUDED_HOX_LOG_H__

#include <android/log.h>

#define  LOG_TAG    "libReferee"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

enum hoxLogLevel
{
    /* NOTE: Do not change the constants here as they are referred to
     *       by outside systems.
     */

    LOG_MIN        = 0,      // BEGIN ---

    LOG_FATAL      = LOG_MIN,
    LOG_SYS_FATAL  = 1,  // with error > 0
    LOG_ERROR      = 2,
    LOG_SYS_ERROR  = 3,  // with error > 0
    LOG_WARN       = 4,
    LOG_SYS_WARN   = 5,   // with error > 0
    LOG_INFO       = 6,
    LOG_DEBUG      = 7,

    LOG_MAX        = LOG_DEBUG // END ---
};

void
hoxLog(enum hoxLogLevel level, const char *fmt, ...);


#endif /* __INCLUDED_HOX_LOG_H__ */
