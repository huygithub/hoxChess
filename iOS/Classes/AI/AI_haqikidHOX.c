/*
 *  AI_haqikidHOX.c
 *  AI Module from HOXChess.
 *
 *  Created by Huy Phan on 9/18/2009.
 *  Copyright 2010 PlayXiangqi. All rights reserved.
 *
 */

/*************************************************************************/
/* HaQiKi D 0.4 plugin for HOXChess. Written by H.G. Muller.             */
/*        http://home.hccnet.nl/h.g.muller/XQhaqikid.html                */
/*************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <time.h>

// Compiler switches to control search features. Remove leading X to enable.
#define ALPHABETA
#define NULLMOVE
#define KILLERS 2   // set to 0 or 2
#define XHASH
#define DEPTHPREF
#define CHECKEXT
#define XFUTILITY
#define REPDRAW
#define XSTALEMATE
#define XTHREAT
#define XHISTORY

#define MAXPLY 63
#define INF 10000

#define IRREVERSIBLE (victim || (piece&~COLOR)>10 && (from-to>10 || to-from>10)) /* XQ */

#define EMPTY 0
#define WHITE 16
#define BLACK 32
#define COLOR (WHITE|BLACK)

// Global variables visible to engine. Normally they 
// would be replaced by the names under which these
// are known to your engine, so that they can be
// manipulated directly by the interface.

int Side;
int Post = 0;            // set to 1 to see machine thinking printed
int MaxDepth  = 60;      // must be set 2 higher than actual depth!
int MaxTime   = 1200000; // Time per session, msec
int MaxMoves  = 40;      // moves per session; 0 = entire game
int TimeInc   = 0;       // extra time per move in msec
int TimeLeft;
int MovesLeft;
int Randomize = 1;
int GamePtr;
int Ticks, tlim, tlim2;

// move stack
typedef union {
    unsigned int m;
    struct {
        unsigned char to, from, special, key;
    } u;
} MOVE;

MOVE moveStack[51200], gameMove/*, retMove*/;
int moveSP;
int path[500];

#ifdef HASH
struct _hash {
    int signature;
    short int score;
    unsigned char from;
    unsigned char to;
    unsigned char depth;
    unsigned char flags;
} *hashTable;

int hashMask = (1<<22)-1;
#endif

int history[256*256];

// repeat stack, storing hash keys of game history
int repStack[1000];
int repSP = 1000;
char repCheck[1000];

// globals that might be used to pass returned values to caller
MOVE retMove;
int  retDepth;

// killers
unsigned int killer[500][2]; // two for each level

// piece list
char spoiler[48];

int hashKeyH=729, hashKeyL=89556, stm, difEval, level, revMovCnt, nodeCnt;
int materialIndex = 1457 + (1457<<16);

// move-generator tables

char deltaVec[] = {
    0,-20, 0,-20, 0, 0, 0,  0,-20,  0, 0, 0, 0, 0, 0, 0, 0, 0,0,0,
    -1,-20, 0,-20, 1, 0, 0,  0,-20,  0,-1, 0, 1, 0, 0, 0, 0, 0,0,0,
    0,  0, 0,  0, 0, 0, 0,  0,-20,  0, 0, 0, 0, 0, 0, 0, 0, 0,0,0,
    -1, 20, 0, 20, 1, 0, 0,  0,-20,  0,-1, 0, 1, 0, 0, 0, 0, 0,0,0,
    0, 20, 0, 20, 0, 0, 0,  0,-20,  0, 0, 0, 0, 0, 0, 0, 0, 0,0,0,
    0,  0, 0,  0, 0, 0, 0,  0,-20,  0, 0, 0, 0, 0, 0, 0, 0, 0,0,0,
    0,  0, 0,  0, 0, 0, 0,  0,-20,  0, 0, 0, 0, 0, 0, 0, 0, 0,0,0,
    0,  0, 0,  0, 0, 0, 0,-20,-20,-20, 0, 0, 0, 0, 0, 0, 0, 0,0,0,
    0,  0, 0,  0, 0, 0,-1,  0,-20,  0, 1, 0, 0, 0, 0, 0, 0, 0,0,0,
    -1, -1,-1, -1,-1,-1,-1, -1,  0,  1, 1, 1, 1, 1, 1, 1, 1, 0,0,0,
    0,  0, 0,  0, 0, 0,-1,  0, 20,  0, 1, 0, 0, 0, 0, 0, 0, 0,0,0,
    0,  0, 0,  0, 0, 0, 0, 20, 20, 20, 0, 0, 0, 0, 0, 0, 0, 0,0,0,
    0,  0, 0,  0, 0, 0, 0,  0, 20,  0, 0, 0, 0, 0, 0, 0, 0, 0,0,0,
    0,  0, 0,  0, 0, 0, 0,  0, 20,  0, 0, 0, 0, 0, 0, 0, 0, 0,0,0,
    0,  0, 0,  0, 0, 0, 0,  0, 20,  0, 0, 0, 0, 0, 0, 0, 0, 0,0,0,
    0,  0, 0,  0, 0, 0, 0,  0, 20,  0, 0, 0, 0, 0, 0, 0, 0, 0,0,0,
    0,  0, 0,  0, 0, 0, 0,  0, 20,  0, 0, 0, 0, 0, 0, 0, 0, 0,0,0,
    0,  0, 0,  0, 0, 0, 0,  0, 20,  0, 0, 0, 0, 0, 0, 0, 0, 0,0,0,
    0,  0, 0,  0, 0, 0, 0,  0, 20,  0, 0, 0, 0, 0, 0, 0, 0
};

#define H 16
#define D 32

char captCode[] = {
    0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,H,1,H,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,H,D,1,D,H,0,0,0,0,0,0,   0,0,0,
    8,8,8,8,8,8,8,8,0,4,4,4,4,4,4,4,4,   0,0,0,
    0,0,0,0,0,0,H,D,2,D,H,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,H,2,H,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,0,   0,0,0,
    0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,0
};

int mval[48] = { // material index value of pieces
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    0,81,81,27,27,9,9, 3<<16,3<<16,1<<16,1<<16, 243,243,243,243,243,
    0,81<<16,81<<16,27<<16,27<<16,9<<16,9<<16, 3,3,1,1, 243<<16,243<<16,243<<16,243<<16,243<<16,
};

char materialTable[1458];


// board (14x20 mailbox with 2-wide guard band)
unsigned char bord[] = {
    48,48,48,48,48,48,48,48,48,48,48,48,48,          0,0,0,0,0,0,0,
    48,48,48,48,48,48,48,48,48,48,48,48,48,          0,0,0,0,0,0,0,
    48,48, 0, 0,25, 0,16,24, 0, 0,18,48,48,          0,0,0,0,0,0,0,
    48,48, 0, 0, 0,17,23, 0, 0, 0, 0,48,48,          0,0,0,0,0,0,0,
    48,48, 0,19,21, 0,26, 0,22,20, 0,48,48,          0,0,0,0,0,0,0,
    48,48,27, 0,28, 0,29, 0,30, 0,31,48,48,          0,0,0,0,0,0,0,
    48,48, 0, 0, 0, 0, 0, 0, 0, 0, 0,48,48,          0,0,0,0,0,0,0,
    48,48, 0, 0, 0, 0, 0, 0, 0, 0, 0,48,48,          0,0,0,0,0,0,0,
    48,48,43, 0,44, 0,45, 0,46, 0,47,48,48,          0,0,0,0,0,0,0,
    48,48, 0,35,37, 0,41, 0,38,36, 0,48,48,          0,0,0,0,0,0,0,
    48,48, 0, 0, 0, 0,40,34, 0, 0, 0,48,48,          0,0,0,0,0,0,0,
    48,48,33, 0, 0,39,32, 0,42, 0, 0,48,48,          0,0,0,0,0,0,0,
    48,48,48,48,48,48,48,48,48,48,48,48,48,          0,0,0,0,0,0,0,
    48,48,48,48,48,48,48,48,48,48,48,48,48
};

#define board (bord+42)

unsigned char pos[48] = {
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    4,23,8,41,47,42,46, 24,5,2,44, 60,62,64,66,68,
    184,180,165,141,147,142,146, 183,164,144,186, 120,122,124,126,128
};

unsigned char code[48] ={
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    0,0xF,0xF,0x4F,0x4F,H,H, 0,0,0,0, 0,0,0,0,0,
    0,0xF,0xF,0x4F,0x4F,H,H, 0,0,0,0, 0,0,0,0,0
};

// we have separate lists of move directions for each square type (wrt edge)
// this automatically confines the pieces to their board zones

// layout of firstDir move-generator array (debug purposes only)
char dirTestMapXQ[] =
"RRRRRRRRR CCCCCCCCC "  // R=0, C=10
"RRRRRRRRR CCCCCCCCC "
"RRRRRRRRR CCCCCCCCC "
"RRRRRRRRR CCCCCCCCC "
"RRRRRRRRR CCCCCCCCC "
"RRRRRRRRR CCCCCCCCC "
"RRRRRRRRR CCCCCCCCC "
"RRRRRRRRR CCCCCCCCC "
"RRRRRRRRR CCCCCCCCC "
"RRRRRRRRR CCCCCCCCC "
"HHHHHHHHH qqqqqqqqq "  // H=200, pq=210
"HHHHHHHHH qqqqqqqqq "
"HHHHHHHHH qqqqqqqqq "
"HHHHHHHHH qqqqqqqqq "
"HHHHHHHHH qqqqqqqqq "
"HHHHHHHHH pPpPpPpPpP"
"HHHHHHHHH pPpPpPpPpP"
"HHHHHHHHH  QQQQQQQQQ"  // PQ=251
"HHHHHHHHH  QQQQQQQQQ"
"HHHHHHHHH  QQQQQQQQQ"
"  E   E    QQQQQQQQQ"  // E=400, e=300
"           QQQQQQQQQ"
"E   E   E KKK A A   "  // K=447, A=451
"          KKK  A    "  // k=307, a=311
"  E   E   KKK A A";

char steps[] = { // null-terminated lists of board steps
    /* H  0*/ -39,-41,-22,-18,18,39,41,22,0, /*9*/ 39,-41,41,22,-18,-39,0,
    /*16*/ 22,18,-18,-39,-41,-22,0, /*23*/ -39,41,-41,-22,18,39,0,
    /* E 30*/ 38,42,-38,-42,0, /*35*/ -42,38,0, /*38*/ 38,42,0, /*41*/ 42,-38,0,
    /* K 44*/ 1,20,-1,-20,0, /*49*/ -1,-20,1,0, /*53*/ -20,1,20,0, /*57*/ 1,20,-1,0,
    /* A 61*/ 19,21,-19,-21,0, /*66*/ -19,0, /*68*/ 21,0, /*70*/ 19,0,
    /* Q 72*/ 1,-1,0,
    /* R 75*/ 1,20,-1,-20,0, /*80*/ -1,-20,1,0, /*84*/ -20,1,20,0,/*88*/ 1,20,-1,0,
    /* C 92*/ 1,20,-1,-20,0, /*97*/ -1,-20,1,0, /*101*/ -20,1,20,0,/*105*/ 1,20,-1,0
};

char block[109] = { // if non-null, square where move can be blocked
    /*H*/ -20,-20,-1,1,-1,20,20,1,0,   20,-20,20,1,1,-20,0,
    1,-1,1,-20,-20,-1,0,         -20,20,-20,-1,-1,20,0,
    /*E*/ 19,21,-19,-21,0,    -21,19,0,    19,21,0,    21,-19,0
};

unsigned char firstDirTab[] = { // points to null-terminated lists in steps[]
    85,88,88,88,88,88,88,88,89,  8,    102,105,105,105,105,105,105,105,106,   8,
    84,75,75,75,75,75,75,75,76,  8,    101, 92, 92, 92, 92, 92, 92, 92, 93,   8,
    84,75,75,75,75,75,75,75,76,  8,    101, 92, 92, 92, 92, 92, 92, 92, 93,   8,
    84,75,75,75,75,75,75,75,76,  8,    101, 92, 92, 92, 92, 92, 92, 92, 93,   8,
    84,75,75,75,75,75,75,75,76,  8,    101, 92, 92, 92, 92, 92, 92, 92, 93,   8,
    84,75,75,75,75,75,75,75,76,  8,    101, 92, 92, 92, 92, 92, 92, 92, 93,   8,
    84,75,75,75,75,75,75,75,76,  8,    101, 92, 92, 92, 92, 92, 92, 92, 93,   8,
    84,75,75,75,75,75,75,75,76,  8,    101, 92, 92, 92, 92, 92, 92, 92, 93,   8,
    84,75,75,75,75,75,75,75,76,  8,    101, 92, 92, 92, 92, 92, 92, 92, 93,   8,
    81,80,80,80,80,80,80,80,77,  8,     98, 97, 97, 97, 97, 97, 97, 97, 94,   8,
    
    6, 5, 4, 4, 4, 4, 4, 4,27,  8,    51,72,72,72,72,72,72,72,59,  8,
    11, 3, 2, 2, 2, 2, 2,24,26,  8,    50,49,49,49,49,49,49,49,46,  8,
    11, 9, 0, 0, 0, 0, 0,23,25,  8,    50,49,49,49,49,49,49,49,46,  8,
    11, 9, 0, 0, 0, 0, 0,23,25,  8,    50,49,49,49,49,49,49,49,46,  8,
    11, 9, 0, 0, 0, 0, 0,23,25,  8,    50,49,49,49,49,49,49,49,46,  8,
    11, 9, 0, 0, 0, 0, 0,23,25,  8,    47,55,47,55,47,55,47,55,47,55,
    11, 9, 0, 0, 0, 0, 0,23,25,  8,    47,55,47,55,47,55,47,55,47,55,
    11, 9, 0, 0, 0, 0, 0,23,25,  8,8,     54,57,57,57,57,57,57,57,58,
    12,10,16,16,16,16,16,17,25,  8,8,     54,57,57,57,57,57,57,57,58,
    13,18,18,18,18,18,18,19,20,  8,8,     54,57,57,57,57,57,57,57,58,
    
    8, 8,38, 8, 8, 8,38, 8, 8,  8,8,     54,57,57,57,57,57,57,57,58,
    8, 8, 8, 8, 8, 8, 8, 8, 8,  8,8,     51,72,72,72,72,72,72,72,59,
    41, 8, 8, 8,30, 8, 8, 8,35,  8,       54,57,58,  8,  68, 8,70,  8,8,8,
    8, 8, 8, 8, 8, 8, 8, 8, 8,  8,       53,44,45,  8,   8,61, 8,  8,8,8,
    8, 8,32, 8, 8, 8,32, 8, 8,  8,       50,49,46,  8,  66, 8,64
};

unsigned int firstDir[48] =
{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,                               // unused
    447,0,0,10,10,200,200,451,451,400,400,251,251,251,251,251,  // white pieces
    307,0,0,10,10,200,200,311,311,300,300,210,210,210,210,210   // black pieces
};

// layout of Zobrist keys and PST (only for debug purposes)

char ZobristTestMapXQ[] =
"RRRRRRRRRRrrrrrrrrrr"  // R=0, r=10
"RRRRRRRRRRrrrrrrrrrr"
"RRRRRRRRRRrrrrrrrrrr"
"RRRRRRRRRRrrrrrrrrrr"
"RRRRRRRRRRrrrrrrrrrr"
"RRRRRRRRRRrrrrrrrrrr"
"RRRRRRRRRRrrrrrrrrrr"
"RRRRRRRRRRrrrrrrrrrr"
"RRRRRRRRRRrrrrrrrrrr"
"CCCCCCCCCCcccccccccc"  // C=200, c=210
"CCCCCCCCCCcccccccccc"
"CCCCCCCCCCcccccccccc"
"CCCCCCCCCCcccccccccc"
"CCCCCCCCCCcccccccccc"
"CCCCCCCCCCcccccccccc"
"CCCCCCCCCCcccccccccc"
"CCCCCCCCCCcccccccccc"
"CCCCCCCCCCcccccccccc"
"HHHHHHHHHHhhhhhhhhhh"  // H=400, h=410
"HHHHHHHHHHhhhhhhhhhh"
"HHHHHHHHHHhhhhhhhhhh"
"HHHHHHHHHHhhhhhhhhhh"
"HHHHHHHHHHhhhhhhhhhh"
"HHHHHHHHHHhhhhhhhhhh"
"HHHHHHHHHHhhhhhhhhhh"
"HHHHHHHHHHhhhhhhhhhh"
"HHHHHHHHHHhhhhhhhhhh"
"qqqqqqqqqq0000000000" // pq=600, empty=610
"qqqqqqqqqq0000000000"
"qqqqqqqqqq0000000000"
"qqqqqqqqqq0000000000"
"qqqqqqqqqq0000000000"
"p pEp pEp 0000000000" // E=701, A=718, a=582
"pApApapap 0000000000"
" EA  Ea  E0000000000"
"PAPAPaPaP 0000000000"
"P PEP PEP 0000000000"
"QQQQQQQQQ   e   e   "  // PQ=700, e=710, K=828, k=692
"QQQQQQQQQ  KKK kkk  "
"QQQQQQQQQ eKKKekkke "
"QQQQQQQQQ  KKK kkk  "
"QQQQQQQQQ   e   e   "; // end=900


// piece-square tables: value of piece on each square (scale Rook ~ 225)

// Piece-square table stolen from XQWLight
unsigned char pst[] = {
    194,206,204,212,200,212,204,206,194,  0,   206,208,207,213,214,213,207,208,206,  0,
    200,208,206,212,200,212,206,208,200,  0,   206,212,209,216,233,216,209,212,206,  0,
    198,208,204,212,212,212,204,208,198,  0,   206,208,207,214,216,214,207,208,206,  0,
    204,209,204,212,214,212,204,209,204,  0,   206,213,213,216,216,216,213,213,206,  0,
    208,212,212,214,215,214,212,212,208,  0,   208,211,211,214,215,214,211,211,208,  0,
    208,211,211,214,215,214,211,211,208,  0,   208,212,212,214,215,214,212,212,208,  0,
    206,213,213,216,216,216,213,213,206,  0,   204,209,204,212,214,212,204,209,204,  0,
    206,208,207,214,216,214,207,208,206,  0,   198,208,204,212,212,212,204,208,198,  0,
    206,212,209,216,233,216,209,212,206,  0,   200,208,206,212,200,212,206,208,200,  0,
    206,208,207,213,214,213,207,208,206,  0,   194,206,204,212,200,212,204,206,194,  0,
    
    96, 96, 97, 99, 99, 99, 97, 96, 96,  0,   100,100, 96, 91, 90, 91, 96,100,100,  0,
    96, 97, 98, 98, 98, 98, 98, 97, 96,  0,   98, 98, 96, 92, 89, 92, 96, 98, 98,  0,
    97, 96,100, 99,101, 99,100, 96, 97,  0,   97, 97, 96, 91, 92, 91, 96, 97, 97,  0,
    96, 96, 96, 96, 96, 96, 96, 96, 96,  0,   96, 99, 99, 98,100, 98, 99, 99, 96,  0,
    95, 96, 99, 96,100, 96, 99, 96, 95,  0,   96, 96, 96, 96,100, 96, 96, 96, 96,  0,
    96, 96, 96, 96,100, 96, 96, 96, 96,  0,   95, 96, 99, 96,100, 96, 99, 96, 95,  0,
    96, 99, 99, 98,100, 98, 99, 99, 96,  0,   96, 96, 96, 96, 96, 96, 96, 96, 96,  0,
    97, 97, 96, 91, 92, 91, 96, 97, 97,  0,   97, 96,100, 99,101, 99,100, 96, 97,  0,
    98, 98, 96, 92, 89, 92, 96, 98, 98,  0,   96, 97, 98, 98, 98, 98, 98, 97, 96,  0,
    100,100, 96, 91, 90, 91, 96,100,100,  0,   96, 96, 97, 99, 99, 99, 97, 96, 96,  0,
    
    88, 85, 90, 88, 90, 88, 90, 85, 88,  0,   90, 90, 90, 96, 90, 96, 90, 90, 90,  0,
    85, 90, 92, 93, 78, 93, 92, 90, 85,  0,   90, 96,103, 97, 94, 97,103, 96, 90,  0,
    93, 92, 94, 95, 92, 95, 94, 92, 93,  0,   92, 98, 99,103, 99,103, 99, 98, 92,  0,
    92, 94, 98, 95, 98, 95, 98, 94, 92,  0,   93,108,100,107,100,107,100,108, 93,  0,
    90, 98,101,102,103,102,101, 98, 90,  0,   90,100, 99,103,104,103, 99,100, 90,  0,
    90,100, 99,103,104,103, 99,100, 90,  0,   90, 98,101,102,103,102,101, 98, 90,  0,
    93,108,100,107,100,107,100,108, 93,  0,   92, 94, 98, 95, 98, 95, 98, 94, 92,  0,
    92, 98, 99,103, 99,103, 99, 98, 92,  0,   93, 92, 94, 95, 92, 95, 94, 92, 93,  0,
    90, 96,103, 97, 94, 97,103, 96, 90,  0,   85, 90, 92, 93, 78, 93, 92, 90, 85,  0,
    90, 90, 90, 96, 90, 96, 90, 90, 90,  0,   88, 85, 90, 88, 90, 88, 90, 85, 88,  0,
    
    9,  9,  9, 11, 13, 11,  9,  9,  9,  0,    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    19, 24, 34, 42, 44, 42, 34, 24, 19,  0,    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    19, 24, 32, 37, 37, 37, 32, 24, 19,  0,    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    19, 23, 27, 29, 30, 29, 27, 23, 19,  0,    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    14, 18, 20, 27, 29, 27, 20, 18, 14,  0,    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    7,  0, 13, 20, 16,  0, 13, 20,  7,  0,    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    7, 20,  7, 20, 15, 20,  7, 20,  7,  0,    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    0, 18, 23,  0,  0, 23, 23,  0,  0, 18,    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    7, 20,  7, 20, 15, 20,  7, 20,  7,  0,    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    7,  0, 13, 20, 16,  0, 13, 20,  7,  0,    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    14, 18, 20, 27, 29, 27, 20, 18, 14,  0,    0,  0, 20,  0,  0,  0, 20,  0,  0,  0,
    19, 23, 27, 29, 30, 29, 27, 23, 19,  0,    0, 11, 15, 11,  0,  1,  1,  1,  0,  0,
    19, 24, 32, 37, 37, 37, 32, 24, 19,  0,   18,  2,  2,  2, 23,  2,  2,  2, 18,  0,
    19, 24, 34, 42, 44, 42, 34, 24, 19,  0,    0,  1,  1,  1,  0, 11, 15, 11,  0,  0,
    9,  9,  9, 11, 13, 11,  9,  9,  9,  0,    0,  0, 20,  0,  0,  0, 20
};

int Zob[900]; // Zobrist randoms

#define NULL_ZOB Zob+610

int *Zobrist[] = {
    NULL_ZOB, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    Zob+828, Zob,     Zob,     Zob+200, Zob+200, Zob+400, Zob+400, Zob+718,
    Zob+718, Zob+701, Zob+701, Zob+700, Zob+700, Zob+700, Zob+700, Zob+700,
    Zob+692, Zob+10,  Zob+10,  Zob+210, Zob+210, Zob+410, Zob+410, Zob+582,
    Zob+582, Zob+710, Zob+710, Zob+600, Zob+600, Zob+600, Zob+600, Zob+600
};

unsigned char *PST[] = {
    pst+610, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    pst+828, pst,     pst,     pst+200, pst+200, pst+400, pst+400, pst+718,
    pst+718, pst+701, pst+701, pst+700, pst+700, pst+700, pst+700, pst+700,
    pst+692, pst+10,  pst+10,  pst+210, pst+210, pst+410, pst+410, pst+582,
    pst+582, pst+710, pst+710, pst+600, pst+600, pst+600, pst+600, pst+600
};

#ifdef WIN32
#include <windows.h>
#else
#include <sys/time.h>
int GetTickCount() // with thanks to Tord Romstad
{       struct timeval t;
    gettimeofday(&t, NULL);
    return (int) (t.tv_sec*1000 + t.tv_usec/1000);
}
#endif

int comp(const void *x, const void *y)
{  return *(int*)y-*(int*)x;
}

int historyComp(const void *x, const void *y)
{  return history[*(int*)y & 0xFFFF]-history[*(int*)x & 0xFFFF];
}

void InitMaterial()
{
    int nR, nC, nH, nP, nA, nE, density, total, i;
    
    for(nR=0; nR<=2; nR++)
        for(nC=0; nC<=2; nC++)
            for(nH=0; nH<=2; nH++)
                for(nA=0; nA<=2; nA++)
                    for(nE=0; nE<=2; nE++)
                        for(nP=0; nP<=5; nP++) {
                            i = nR*mval[17] + nC*mval[19] + nH*mval[21] + nP*mval[27]
                            + nA*mval[39] + nE*mval[41];
                            density = nA + nE + nP + 2*(nR + nC + nH);
                            total = nC*(10-(density-20)*(density-20)/10);
                            total += nR*10 + nP*5;
                            density = nP + nC + nH + 2*nR;
                            total += (nA+nE)*((13-density)*(13-density)/10 - 5);
                            materialTable[i] = total;
                            if(total > 127 || total < -128) exit(0);
                            // Commented by HPHAN: total += 25*nP + 112*(nC+nH) + 225*nR - 50*nA - 35*nE;
                            // devaluate superfluous defenders
                            
                            // Commented by HPHAN: to fix the warning "Expression result unused"
                            //   and "Value stored ... never read
                            //defenders = 2*nA + 2*nE;
                            //attackers = 8*nR + 4*nH + 2*nC + 3*nP;
                            //if(defenders > attackers ,0)
                            //    materialTable[i] += 6*(defenders - attackers);
                        }
}

int p1, p2, p3, p4;
int Evaluate(int stm)
{
    int x, y, score=0, penalty;
    
    if(pos[WHITE] == 4) {
        x = 4;
        if(board[24] == 23 || board[24] == 24) score += 10;
        while(board[x+=20] == EMPTY);
        if(!(board[x]&WHITE) && code[board[x]] == 0x4F)
            score -= 30; // king held at gun point
        else { // caculate potential penalty (in case piece is pinned)
            penalty = (board[x] & ~COLOR)<=6 ? (x==24?30:20) : (x==24?10:0);
            y = x;
            while(board[x+=20] == EMPTY);
            if(board[x] == BLACK) {    // special case: piece between kings
                if(board[y] & BLACK) { // it was a black piece, recalculate
                    penalty = (board[y] & ~COLOR)<=6 ? (x==164?-30:-20)
                    : (x==164?-10:0);
                }
                score -= penalty;
                goto Done;
            }
            if(!(board[x]&WHITE) && code[board[x]] == 0xF) {    // Rook pin
                score -= penalty; // for actually pinned piece
                penalty = 20;     // for potential discovered Cannon check
            } else if( (board[x] & ~COLOR) <= 6 ) penalty += 20;
            if(x<180) {
                while(board[x+=20] == EMPTY);
                if(!(board[x]&WHITE) && code[board[x]] == 0x4F) // Cannon pin
                    score -= penalty;
            }
        }
    }
    p1 = score;
    if(pos[BLACK] == 184) {
        x = 184;
        if(board[644] == 39 || board[164] == 40) score -= 10;
        while(board[x-=20] == EMPTY);
        if(!(board[x]&BLACK) && code[board[x]] == 0x4F)
            score += 30; // king held at gun point
        else { // caculate potential penalty (in case piece is pinned)
            penalty = (board[x] & ~COLOR)<=6 ? (x==164?30:20) : (x==164?10:0);
            while(board[x-=20] == EMPTY);
            if(!(board[x]&BLACK) && code[board[x]] == 0xF) {    // Rook pin
                score += penalty; // for actually pinned piece
                penalty = 20;     // for potential discovered Cannon check
            } else if( (board[x] & ~COLOR) <= 6 ) penalty += 20;
            if(x>20) {
                while(board[x-=20] == EMPTY);
                if(!(board[x]&BLACK) && code[board[x]] == 0x4F) // Cannon pin
                    score += penalty;
            }
        }
    }
    p2 = score - p1;
Done:
    if(stm == BLACK) score = -score;
    
    return (p3=materialTable[materialIndex>>(stm-16) & 0xFFFF]) + score
    - (p4=materialTable[materialIndex>>(32-stm) & 0xFFFF]);
}

int StupidInCheck(int stm, int to)
{   // scan board in all directions, to see if opponent can capture 'to'
    int i, x;
    i=0; x=to;
    while(board[++x] == EMPTY) i++;
    if(!(board[x]&stm) &&  (board[x]&~COLOR) <= 2 )
        return 1;
    if(!(board[x]&stm) && (board[x]&~COLOR) > 10 && i==0)
        return 1;
    while(board[++x] == EMPTY) i++;
    if(!(board[x]&stm) && ( (board[x]&~COLOR)==3 || (board[x]&~COLOR)==4) )
        return 1;
    i=0; x=to;
    while(board[--x] == EMPTY) i++;
    if(!(board[x]&stm) &&  (board[x]&~COLOR) <= 2 )
        return 1;
    if(!(board[x]&stm) && (board[x]&~COLOR) > 10 && i==0)
        return 1;
    while(board[--x] == EMPTY) i++;
    if(!(board[x]&stm) && ( (board[x]&~COLOR)==3 || (board[x]&~COLOR)==4) )
        return 1;
    i=0; x=to;
    while(board[x+=20] == EMPTY) i++;
    if(!(board[x]&stm) &&  (board[x]&~COLOR) <= 2 )
        return 1;
    if(!(board[x]&stm) && (board[x]&~COLOR) > 10 && i==0 && stm == WHITE)
        return 1;
    while(board[x+=20] == EMPTY) i++;
    if(!(board[x]&stm) && ( (board[x]&~COLOR)==3 || (board[x]&~COLOR)==4) )
        return 1;
    i=0; x=to;
    while(board[x-=20] == EMPTY) i++;
    if(!(board[x]&stm) &&  (board[x]&~COLOR) <= 2 )
        return 1;
    if(!(board[x]&stm) && (board[x]&~COLOR) > 10 && i==0 && stm == BLACK        )
        return 1;
    while(board[x-=20] == EMPTY) i++;
    if(!(board[x]&stm) && ( (board[x]&~COLOR)==3 || (board[x]&~COLOR)==4) )
        return 1;
    if(board[to+21] == EMPTY) {
        x = to+22;
        if(!(board[x]&stm) && ( (board[x]&~COLOR)==5 || (board[x]&~COLOR)==6) )
            return 1;
        x = to+41;
        if(!(board[x]&stm) && ( (board[x]&~COLOR)==5 || (board[x]&~COLOR)==6) )
            return 1;
    }
    if(board[to-21] == EMPTY) {
        x = to-22;
        if(!(board[x]&stm) && ( (board[x]&~COLOR)==5 || (board[x]&~COLOR)==6) )
            return 1;
        x = to-41;
        if(!(board[x]&stm) && ( (board[x]&~COLOR)==5 || (board[x]&~COLOR)==6) )
            return 1;
    }
    if(board[to+19] == EMPTY) {
        x = to+18;
        if(!(board[x]&stm) && ( (board[x]&~COLOR)==5 || (board[x]&~COLOR)==6) )
            return 1;
        x = to+39;
        if(!(board[x]&stm) && ( (board[x]&~COLOR)==5 || (board[x]&~COLOR)==6) )
            return 1;
    }
    if(board[to-19] == EMPTY) {
        x = to-18;
        if(!(board[x]&stm) && ( (board[x]&~COLOR)==5 || (board[x]&~COLOR)==6) )
            return 1;
        x = to-39;
        if(!(board[x]&stm) && ( (board[x]&~COLOR)==5 || (board[x]&~COLOR)==6) )
            return 1;
    }
    return 0;
}

// simple alpha-beta search for mailbox + piece list
int Search(int origAlpha, int beta, int lastPly, int PV, int depth)
{
    int alpha, curEval, curMove, lastMove, bestMove, capts, nonCapts, iterDep;
    int score, i, j, from, to, step, piece, victim, dir, mustSort, firstMove;
    int bestScore = 0, prevScore = -INF, startScore = -INF, ranKey;
    int saveKeyH = hashKeyH, saveKeyL = hashKeyL;
    int /*alphaMoves,*/ evalCor, hashMove = 0, inCheck = 0, xking = pos[stm], king;
#ifdef HASH
    int origDep = depth;
    struct _hash *hashEntry = NULL;
#endif
    int old50 = revMovCnt;
    int savDifEval = difEval;
    //int oldCnt;
#ifdef CASTLE
    char saveRights = castlingRights;
#endif
    level++;
    stm ^= COLOR;
    king = pos[stm];
    nodeCnt++;
    
    curEval = difEval + (evalCor = Evaluate(stm)) + 3;
    if(depth==1000)
    {   ranKey = rand();
        for(i=0;i<256*256;i++)history[i] = 0;
    }
    
    // pe-adjust limits for delayed-loss bonus
    origAlpha -= (origAlpha <  curEval);
    beta      -=      (beta <= curEval);
    
    to = lastPly & 255;
    from = lastPly>>8 & 255;
    
    if(captCode[king-to+188] & 0x1F) { // orthogonal or adjacent hippogonal
        // test for check with mover or mover-activated Cannon
        if(captCode[king-to+188] & 0xF) { // could be Rook or Cannon
            int x=king, v=deltaVec[to-king+188];
            i=0;
            while(board[x+=v] == EMPTY) i++;
            if(!(board[x]&stm)) {
                if( code[board[x]] == 0xF) // Rook
                    inCheck = 1;
                else if(i==0 && (board[x]&~COLOR)>10
                        && v != (stm==WHITE ? -20 : 20)) // Pawn
                    
                    inCheck = 1;
            }
            if(!inCheck) {
                while(board[x+=v] == EMPTY);
                if(!(board[x]&stm) && code[board[x]] == 0x4F) // Cannon
                    inCheck = 1;
            }
        } else { // must be Horse
            if(code[board[to]] == H) {
                if(board[to+deltaVec[king-to+42]] == EMPTY) inCheck = 1;
            }
        }
    }
    
    if(captCode[from-king+188] & 0x2F) { // orthogonal or adjacent diagonal
        // test for discovered check
        if(captCode[king-from+188] & 0xF) { // could be Rook or Cannon
            int x=king, v=deltaVec[from-king+188];
            while(board[x+=v] == EMPTY);
            if(!(board[x]&stm) && code[board[x]] == 0xF) // Rook
                inCheck = 1;
            else {
                while(board[x+=v] == EMPTY);
                if(!(board[x]&stm) && code[board[x]] == 0x4F) // Cannon
                    inCheck = 1;
            }
        } else { // must be Horse
            if((COLOR&board[from+deltaVec[from-king+51]]) == (stm^COLOR) &&
               code[board[from+deltaVec[from-king+51]]] == H)
                inCheck = 1;
            else
                if((COLOR&board[from+deltaVec[from-king+42]]) == (stm^COLOR) &&
                   code[board[from+deltaVec[from-king+42]]] == H  )
                    inCheck = 1;
        }
    }
    
#ifdef CHECKEXT
    depth += inCheck; // check extension
#endif
    if(depth <= 0) startScore = curEval;
    
    if(startScore >= beta) {
        bestScore = startScore; // stand-pat cutoff or mate-distance pruning
        goto NullCut;
    }
#ifdef HASH
    // PROBE HASH
    if(depth >= 0) { 
        hashEntry = hashTable + (hashKeyL + (stm<<3) & hashMask);
        if(hashKeyH == hashEntry->signature) { // hash hit
            if(hashEntry->depth >= depth && (
                                             hashEntry->flags & 1 && hashEntry->score >= beta ||
                                             hashEntry->flags & 2 && hashEntry->score <= origAlpha) ) {
                bestScore = hashEntry->score;
                goto NullCut;
            }
            hashMove = hashEntry->to + (hashEntry->from << 8); // get move
#ifdef DEPTHPREF
        } else { struct _hash *oldEntry = hashEntry;
            hashEntry = hashTable + (hashKeyL + (stm<<3) & hashMask ^ 1);
            if(hashKeyH == hashEntry->signature) { // hash hit
                if(hashEntry->depth >= depth && (
                                                 hashEntry->flags & 1 && hashEntry->score >= beta ||
                                                 hashEntry->flags & 2 && hashEntry->score <= origAlpha) ) {
                    bestScore = hashEntry->score;
                    goto NullCut;
                }
                hashMove = hashEntry->to + (hashEntry->from << 8); // get move
            } else {
                if(hashEntry->depth >= oldEntry->depth) // replace lowest draft
                    hashEntry = oldEntry;
                hashMove = 0;
            }}
#else
    } else hashMove = 0;
#endif
}
#endif
// CHECK LEGALITY
if(captCode[xking-to+188] & 0xF) { // orthogonal
    // test for King capture by mover-activated Cannon
    int x=to, v=deltaVec[to-xking+188];
    while(board[x+=v] == EMPTY);
    if((board[x]&COLOR) == stm && code[board[x]] == 0x4F) { // Cannon
        x = xking;
        while(board[x+=v] == EMPTY);
        if(x == to) { bestScore = INF; goto NullCut; }
    }
}

if(captCode[from-xking+188] & 0x2F) { // orthogonal or adjacent diagonal
    // test if we moved pinned piece
    if(captCode[xking-from+188] & 0xF) { // could be Rook or Cannon
        int x=xking, v=deltaVec[from-xking+188];
        while(board[x+=v] == EMPTY);
        if(x == king ||                                   // King facing
           ((board[x]&COLOR) == stm && code[board[x]] == 0xF)) // Rook
        { bestScore = INF; goto NullCut; }
        while(board[x+=v] == EMPTY);
        if((board[x]&COLOR) == stm && code[board[x]] == 0x4F) // Cannon
        { bestScore = INF; goto NullCut; }
    } else { // might be Horse
        if( ((COLOR&board[from+deltaVec[from-xking+51]]) == stm &&
           code[board[from+deltaVec[from-xking+51]]] == H)
           || ((COLOR&board[from+deltaVec[from-xking+42]]) == stm &&
           code[board[from+deltaVec[from-xking+42]]] == H)  )
        { bestScore = INF; goto NullCut; }
    }
}
#if 0
{   int xking = pos[COLOR-stm];
    align = captCode[xking-(from=lastPly>>8&255)+188];
    if(align & code[board[to]])
        }
#endif
#ifdef NULLMOVE
// null move pruning
if(depth>0 && !inCheck && curEval >= beta) { // this ensures no two consecutive null moves
    revMovCnt = 0;    // null move irreversible, to avoid repeats
    difEval = -savDifEval;
    
    score = -Search(-beta, 1-beta, 0x3C3C, 0, depth-3-(PV<0)<0?0:depth-3-(PV<0));
    
    if(score >= beta) {
        bestScore = beta;
        goto NullCut; // skip hash store
    }
#ifdef THREAT
    // null move failed low; identify threat from refutation
    
#endif
}
#endif
if(PV<0) PV=0;

// GENERATE MOVES, put them on back-to-back capture and non-capt stacks
bestMove = capts = lastMove = nonCapts =
moveSP += 256;   // reserve space for move list

for(piece = stm + 15; piece >= stm; piece--) { // loop over our pieces
    if((from = pos[piece]) == 0xFF) continue;  // is captured
    dir = firstDirTab[ firstDir[piece]+from ];
    while( (step = steps[dir]) ) { // loop over directions
        to = from + step;
        do{ // scan along ray (ends after one iteration for leapers)
            
            if(block[dir] && board[block[dir]+from] != EMPTY)
                break; // lame leaper is blocked, next direction
            
            victim = board[to];
            
            if(victim != EMPTY) {              // capture
                
                if(dir >= 92) {                // Cannon
                    // displace toSqr to first obstacle after platform
                    while((victim = board[to+=step]) == EMPTY);
                }
                
                if(victim & stm) break;        // capture own, next dir
                if((victim & ~COLOR) == 0) {  // King capture
                    bestScore = INF;
                    goto KingCapt;
                }
                moveStack[--capts].u.from = from;
                moveStack[capts].u.to = to;
                moveStack[capts].u.key = PST[victim][to] -
                (PST[piece][from]>>4);
            } else {                           // non-capture
                moveStack[lastMove].u.key  = PST[piece][to] - PST[piece][from] + 128;
                moveStack[lastMove].u.from = from;
                moveStack[lastMove++].u.to = to;
            }
            
            to += step;
        } while(!(victim-EMPTY | (dir<75)));     // end if leaper or capt
        dir++;                                 // try new direction
    }
}
mustSort = KILLERS+1; // indicate that we yet have to sort the moves
#ifdef HASH
if(hashMove) {
    for(i=capts; i<lastMove; i++) {
        if(moveStack[i].u.to   == (hashMove&0xFF) &&
           moveStack[i].u.from == (hashMove>>8&0xFF) ) {
            int m = moveStack[i].m | 0xFF000000; // give highest priority
            if(i >= nonCapts) {       // hash move is non-capture
                if(depth < 1) break;  // in QS only capt
                moveStack[i].m = moveStack[nonCapts].m;
                moveStack[nonCapts++].m = m;     // group with captures
            }  else moveStack[i].m = m;     // upgrade priority in place
            break;
        }
    }
}
#endif
// Commented by HPHAN: alphaMoves = capts;

if(depth<=0) {
    if(curEval > origAlpha) {
        origAlpha = curEval;
    }
    lastMove = nonCapts;
    depth=1;
}

// ITERATIVE DEEPENING LOOP
for(iterDep = depth>=1000?1:hashMove?depth:PV?2-(depth&1):depth>2?1:depth; iterDep <= depth; iterDep+=2-((depth>=1000)|!PV)) {
    bestScore = startScore;
    alpha = origAlpha;
    firstMove = (depth>=1000)|lastPly?2:0;
    
    // LOOP OVER MOVES
    for(curMove=capts; curMove<lastMove; curMove++) {
        int promo;
        
        // SORT MOVES: extract remaining capture with highest priority;
        if(mustSort) {
            if(curMove < nonCapts) {         // no all captures don yet
                if(nonCapts - curMove > 1) { // more than one, so sort
                    // there is more than 1 capture, so we must sort
                    unsigned int move;
                    move = moveStack[curMove].m; j = curMove;
                    for(i = curMove+1; i<nonCapts; i++) {
                        if(moveStack[i].m > move) {
                            j = i;
                            move = moveStack[i].m;
                        }
                    }
                    moveStack[j].m = moveStack[curMove].m;
                    moveStack[curMove].m = move;
                }
            } else {
                if(mustSort > 1) {
#if KILLERS
                    // after captures, dig out killers
                    for(i=curMove; i<lastMove; i++) {
                        unsigned int m = moveStack[i].m;
                        if(m == killer[level][0] ||
                           m == killer[level][1]   ) {
                            // non-capture matches killer; swap to front
                            moveStack[i].m = moveStack[curMove].m;
                            moveStack[curMove].m = m;
                            break;
                        }
                    }
                }
                if(i >= lastMove) mustSort = 1; // could not find killer
#endif
                if(mustSort == 1) { // extract positionally good nonCapts
#ifdef HISTORY
                    qsort(moveStack + curMove, lastMove-curMove, sizeof(int), historyComp);
#else
                    unsigned int best = moveStack[curMove].m;
                    int j = curMove;
                    for(i=curMove+1; i<lastMove; i++)
                        if(moveStack[i].m > best) {
                            best = moveStack[i].m;
                        }
                    best -= 0x8000000;
                    for(i=curMove; i<lastMove; i++)
                        if(moveStack[i].m > best) {
                            unsigned int h;
                            h = moveStack[j].m;
                            moveStack[j++].m = moveStack[i].m;
                            moveStack[i].m = h;
                        }
                    qsort(moveStack + curMove, j - curMove,
                          sizeof(int), comp);
#endif
                }
                mustSort--;
            }
        }
    xxx:
        // NEXT MOVE
        victim =          board[  to=moveStack[curMove].u.to];
        promo  = piece  = board[from=moveStack[curMove].u.from];
        
#ifdef CHESS
        // make special part of e.p., castling or promotion
        if(mode = movStack[curMove].u.special) {
            if(mode<5) { // castling, move Rook
                rookVictim = board[rookTo[mode]]; // just in case
                pos[board[rookTo[mode]] = board[rookFrom[mode]]] =
                rookTo[mode];
                board[rookFrom[mode]] = EMPTY;
            } else if(mode == 5) { // e.p., remove Pawn
            } else { // promotion, create 
                promo = ;
            }
        }
#endif
        // HASH KEY UPDATE
        hashKeyL = saveKeyL ^ Zobrist[piece ][from]
        ^ Zobrist[piece ][to]
        ^ Zobrist[victim][to];
        hashKeyH = saveKeyH ^ Zobrist[piece ][from+1] // kludge to save
        ^ Zobrist[piece ][to+1]   //   table space:
        ^ Zobrist[victim][to+1];  //   overlap tables
        
#ifdef REPDRAW
        // REPETITION CHECK
        if(IRREVERSIBLE) revMovCnt = 0; else {
            revMovCnt = old50 + 1;
            for(i=3; i<revMovCnt; i+=2) // check same stm only
                if(hashKeyL == repStack[repSP + i]) {
                    // repeat; determine outcome
                    int j, myPerp = 1, hisPerp = inCheck;
                    for(j=i-1; j>=0; j-=2)
                        myPerp &= repCheck[repSP + j];
                    for(j=i-2; j>=0; j-=2)
                        hisPerp &= repCheck[repSP + j];
                    score = myPerp != hisPerp ? -INF : 0;
                    if(hisPerp && !myPerp) break;
                    goto Repeat;
                }
        }
#endif
        // FUTILITY PRUNING
        difEval = -savDifEval - PST[victim][to]
        - PST[piece ][to]
        + PST[piece ][from];
        if(depth >= 1000 && Randomize && GamePtr < 8)
            difEval += ((ranKey ^ hashKeyL) * 756195691 >> 27 & 15) - 8;
        
        // MAKE MOVE
        repStack[--repSP] = hashKeyL; repCheck[repSP] = inCheck;
        materialIndex -= mval[victim];
        board[from] = EMPTY;
        board[to]   = promo;
        pos[promo]  = to;
        pos[victim] = 0xFF;
#ifdef CASTLE
        castlingRights |= spoiler[piece] |= spoiler[victim];
#endif
        
        // LEGALITY TEST for king moves and check evasions
        // (to ensure a simplified mover-based test suffices in daughter)
        if((inCheck || piece==stm) && StupidInCheck(stm, pos[stm]))
            score = -INF;
        else
            
            // RECURSION
            score = -Search(-beta, -alpha, moveStack[curMove].m, mustSort?firstMove:-1, iterDep-1);
        
#ifdef CASTLE
        castlingRights = saveRights;
#endif
        // UNMAKE MOVE
        pos[piece]  = from;
        pos[victim] = to;
        board[from] = piece;
        board[to]   = victim;
        repSP++; materialIndex += mval[victim];
        
    Repeat:
#ifdef CHESS
        // UNMAKE SIDE EFFECTS
        if(mode) {
            if(mode<5) { // castling;
                pos[board[rookFrom[mode]] = board[rookTo[mode]]] =
                rookFrom[mode];
                board[rookTo[mode]] = rookVictim;
            } else if(mode == 5) { // e.p.
                capt = ; mode = 0;
            } else { // promotion
                promo = ;
            }
        }
#endif
        // SCORE MINIMAXING
        if(score > bestScore) {
            bestScore = score; bestMove = curMove;
            if(score > alpha) {
#ifdef ALPHABETA
                if(score >= beta) {
                    // beta cutoff detected
#if KILLERS
                    if(curMove >= nonCapts) {
                        // remember non-Capt cut-moves as killers
                        if(moveStack[curMove].m != killer[level][0])
                            killer[level][1] = killer[level][0];
                        killer[level][0] = moveStack[curMove].m;
                    }
#endif
                    history[moveStack[curMove].m & 0xFFFF] += iterDep*iterDep;
                    goto Cutoff;
                }
#endif
                alpha = score;
            }
        }
        if(depth>=1000 && GetTickCount()-Ticks > tlim2
           && bestScore > prevScore-7) break;
        // next move
        firstMove = 0;
    }
    
    if(depth>=1000) {
        Evaluate(stm);
        if(Post) printf("%2d %6d %6d %10d %c%c%c%c {%d,%d(%d,%d,%d,%d)%x}\n",iterDep, 4*bestScore,
                        (GetTickCount()-Ticks)/10, nodeCnt,
                        'a'+moveStack[bestMove].u.from%20, '0'+moveStack[bestMove].u.from/20,
                        'a'+moveStack[bestMove].u.to%20, '0'+moveStack[bestMove].u.to/20,
                        curEval, evalCor,p1,p2,p3,p4,materialIndex
                        ); fflush(stdout);
        if(GetTickCount()-Ticks > tlim || iterDep >= MaxDepth ||
           iterDep >= 2*(INF-bestScore)-1 || iterDep >= 2*(bestScore+INF)) {
            // in root, stop deepening if time (or depth) used up
            gameMove = moveStack[bestMove]; // return best move
            difEval  = savDifEval; // make sure we leave globals unchanged
            hashKeyH = saveKeyH; hashKeyL = saveKeyL;
            revMovCnt = old50;
            break;
            prevScore = bestScore;
        }
    }
    if(iterDep < depth) { // bring best move to front of list
        j = moveStack[bestMove].m; 
        for(i=bestMove; i>capts; i--)
            moveStack[i].m = moveStack[i-1].m;
        moveStack[capts].m = j;
        bestMove = capts;
    }
    
#ifdef STALEMATE
    // MATE TEST, only needed in games where stalemate is draw
    if(bestScore == -INF) { // no legal moves were found
        if(!inCheck) bestScore = 0; // stalemate
    }
#endif
    
Cutoff:
#ifdef HASH
    // HASH STORE
    if(origDep >= 1 && hashEntry) {
        hashEntry->signature = saveKeyH;
        hashEntry->from = moveStack[bestMove].u.from;
        hashEntry->to   = moveStack[bestMove].u.to;
        hashEntry->depth = iterDep
#ifdef CHECKEXT
        - inCheck
#endif
        ;
        hashEntry->score = bestScore;
        hashEntry->flags = (bestScore > origAlpha) // is lower bound
        + 2*(bestScore < beta);   // is upper bound
    }
#endif
    if(iterDep<depth-1 && depth < 1000 && !PV) iterDep = depth-1;
        }

KingCapt:

moveSP -= 256;

NullCut:

// apply delayed-loss bonus and return
level--;
stm ^= COLOR;
return bestScore + (bestScore < curEval);
}

// The engine is invoked through the following
// subroutines, that can draw on the global vaiables
// that are maintained by the interface:
// Side         side to move
// TimeLeft     ms left to next time control
// MovesLeft    nr of moves to play within TimeLeft
// MaxDepth     search-depth limit in ply
// Post         boolean to invite engine babble
// Randomize    if set, first 4 moves are randomized

// InitEngine() progran start-up initialization
// InitGame()   initialization to start new game
//              (sets Side, but not time control)

// define this to the codes used in your engine,
// if the engine hasn't defined it already.

int initDone = 0;

void HaQiKiD_InitEngine()
{
    int i, j;
    
#ifdef HASH
    hashTable = (struct _hash *) calloc(hashMask+1, sizeof(struct _hash));
#endif
    for(j=0; j<50; j++) rand();
    for(j=0; j<900; j++)
        Zob[j] = rand() + rand()/100 + rand()*193 + rand()*138753;
    for(i=0; i<200; i+=20) for(j=0; j<10; j++)
        Zob[i + j + 610] = 0;
    InitMaterial(); initDone = 1;
}

void HaQiKiD_InitGame()
{
    int i,j; static char array[] = { 1,5,9,7,0,8,10,6,2 };
    
    if(!initDone) HaQiKiD_InitEngine();
    
    for(j=0; j<9; j++) { // setup board
        for(i=0; i<200; i+=20) board[i+j] = EMPTY;
        board[j]     = array[j] + WHITE; pos[board[j]] = j;
        board[180+j] = array[j] + BLACK; pos[board[180+j]] = 180+j;
        if(!(j&1)) { // Pawns
            board[60+j]  = j/2 + 11 + WHITE; pos[board[60+j]] = 60+j;
            board[120+j] = j/2 + 11 + BLACK; pos[board[120+j]] = 120+j;
        }
    }
    board[41]  = 3 + WHITE; pos[3+WHITE] = 41;  // Cannons
    board[47]  = 4 + WHITE; pos[4+WHITE] = 47;
    board[141] = 3 + BLACK; pos[3+BLACK] = 141;
    board[147] = 4 + BLACK; pos[4+BLACK] = 147;
    Side = WHITE;
    revMovCnt = difEval = GamePtr = 0;
    repSP = 1000;
    hashKeyH=729; hashKeyL=89556; // just some non-zero values;
    materialIndex = 1457 + (1457<<16);
    srand(GetTickCount());
#ifdef HASH
    for(i=0; i<=hashMask; i++) hashTable[i].signature = 0;
#endif
    MovesLeft = MaxMoves; TimeLeft = MaxTime; /* initialize time control */
}

void MakeMove()
{
    difEval = -difEval - PST[board[gameMove.u.to]][gameMove.u.to]
    - PST[board[gameMove.u.from]][gameMove.u.to]
    + PST[board[gameMove.u.from]][gameMove.u.from];
    hashKeyL ^= Zobrist[board[gameMove.u.from]][gameMove.u.from]
    ^ Zobrist[board[gameMove.u.from]][gameMove.u.to]
    ^ Zobrist[board[gameMove.u.to]][gameMove.u.to];
    hashKeyH ^= Zobrist[board[gameMove.u.from]][gameMove.u.from+1] // kludge to save
    ^ Zobrist[board[gameMove.u.from]][gameMove.u.to+1]             // table space:
    ^ Zobrist[board[gameMove.u.to]][gameMove.u.to+1];              // overlap tables
    repStack[--repSP] = hashKeyL;
    repCheck[repSP] = StupidInCheck(Side,pos[Side]);
    materialIndex -= mval[board[gameMove.u.to]];
    revMovCnt++; if(board[gameMove.u.to]) revMovCnt = 0;
    pos[board[gameMove.u.from]] = gameMove.u.to;
    pos[board[gameMove.u.to]]   = 0xFF;
    board[gameMove.u.to]   = board[gameMove.u.from];
    board[gameMove.u.from] = EMPTY;
    Side ^= COLOR; GamePtr++;
}

void HaQiKiD_OnOpponentMove(const char *line)
{
    int m;
    
    // command not recognized, assume input move
    m = line[0]<'a' | line[0]>'i' | line[1]<'0' | line[1]>'9' |
    line[2]<'a' | line[2]>'i' | line[3]<'0' | line[3]>'9';
    {const char *c=line;  gameMove.u.from=c[0]-'a'+20*(c[1]-'0');
        gameMove.u.to  =c[2]-'a'+20*(c[3]-'0');}
    if (m)
        printf("Bad move syntax: %s\n", line);  // doesn't have move syntax
    else 
        MakeMove();  // legal move, perform it
}

const char* HaQiKiD_GenerateNextMove()
{
    static char move[5];
    int m;
    
    // determine time to sepend on next move
    Ticks = GetTickCount();
    m = MovesLeft<=0 ? 40 : MovesLeft;
    tlim = 0.5*(TimeLeft+(m-1)*TimeInc)/(m+7);
    if(10*tlim > TimeLeft) tlim = TimeLeft/10;
    tlim2 = 2*tlim;
    
    // now call the AI
    nodeCnt=0;
    stm = Side ^ COLOR;
    if (Search(-INF, INF, gameMove.m, 0, 1000) > 1-INF) {
        MakeMove(); // perform the move it came up with
        
        sprintf(move, "%c%c%c%c",
                'a'+gameMove.u.from%20, '0'+gameMove.u.from/20,
                'a'+gameMove.u.to  %20, '0'+gameMove.u.to/20);
        
        m = GetTickCount() - Ticks;
        
        // time-control accounting
        TimeLeft -= m;
        TimeLeft += TimeInc;
        if(--MovesLeft == 0) {
            MovesLeft = MaxMoves;
            if(MaxMoves == 1)
                TimeLeft  = MaxTime;
            else TimeLeft += MaxTime;
        }
        
    } else {
        printf("resign\n"); // no move, we must be mated
    }
    return move;
}

/*****************************************************************************/
/*****************************************************************************/

/////////////////////////////////////////////////
//       Huy Phan 's changes                   //
/////////////////////////////////////////////////

void HaQiKiD_DeInitEngine()
{
    if ( initDone )
    {
#ifdef HASH
        free(hashTable);
#endif
        initDone = 0;
    }
}

void HaQiKiD_SetMaxDepth( int searchDepth )
{
    MaxDepth = searchDepth;
}

///////////////// END of Huy Phan's changes //////////////////////////////////

/************************* END OF FILE ***************************************/
