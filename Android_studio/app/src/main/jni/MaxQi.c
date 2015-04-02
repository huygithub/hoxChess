/***************************************************************************/
/*                                 MaxQi,                                  */
/* Xiangqi version of the sub-2KB (source) micro-Max Chess program,        */
/* by H.G. Muller ( http://home.hccnet.nl/h.g.muller/chess.html ).         */
/* This source contains only the AI routine, for integration in HOXChess.  */
/***************************************************************************/

// ******** Android NDK *****
#include <string.h>
#include <jni.h>
#include <android/log.h>

#define  LOG_TAG    "libAI_MaxQi"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

//
// AI error codes (or Return-Codes).
//
#define AI_RC_UNKNOWN       -1
#define AI_RC_OK             0  // A generic success
#define AI_RC_ERR            1  // A generic error
#define AI_RC_NOT_FOUND      2  // Something not found
#define AI_RC_NOT_SUPPORTED  3  // Something not supported

// **************************

#include <stdio.h>
#include <stdlib.h>

#ifdef WIN32
#include <windows.h>
#else
#include <sys/time.h>
int GetTickCount() // with thanks to Tord Romstad
{	struct timeval t;
	gettimeofday(&t, NULL);
	return t.tv_sec*1000 + t.tv_usec/1000;
}
#endif

int Side;
int Post = 0;           /* set to 1 to see machine thinking printed */
int MaxDepth  = 60;     /* must be set 2 higher than actual depth!  */
int MaxTime   = 300000; /* Time per session, msec                   */
int MaxMoves  = 40;     /* moves per session; 0 = entire game       */
int TimeInc   = 0;      /* extra time per move in msec              */
int TimeLeft;
int MovesLeft;
int Fifty;
int PlyNr;
int Ticks, tlim;

#define W while
#define K(A,B) *(int*)(T+A+((B&31)<<8))
#define J(A) K(y+A,b[y])-K(x+A,u)-K(y+A,t)

#define U (1<<22)
struct _ {int K,V;char X,Y,D,F;} A[U];         /* hash table, 4M entries   */

int M=136,S=128,I=8e3,Q,O,K,N,j,R,J,Z,LL,L,    /* M=0x88                   */
w[]={0,10,10,-1,15,15,19,19,20,45,46,90},      /* relative piece values    */
of[]={0xC07,0xC07,0xC07,0xC07,0,               /* move rights flags  King  */
 0x470,0x470,0x470,0x470,0,0x870,0x870,0x870,0x870,0,         /* Elephants */
 7,7,7,0,7,7,7,0,                                             /* Pawns     */
 0xC07,0xC07,0xC07,0xC07,0,                                   /* Advisors  */
 0x1070,0x1F070,0x10070,-0xFF90,0x1070,0x1F070,0x10070,-0xFF90,0, /* Horse */
 0xBA,0xBA,0xBA,0xBA,0,                                       /* Cannon    */
 3,3,3,3                                                      /* Chariot   */
},
od[]={0,16,20,-1,4,9,14,18,22,27,36,41,46};    /* 1st dir. in o[] per piece*/

char
o[]={1,16,-1,-16,0,                                    /* board steps King */
15,17,-15,-17,0,15,17,-15,-17,0,                              /* Elephants */
16,-16,-1,0,16,-16,1,0,                                       /* Pawns     */
15,17,-15,-17,0,                                              /* Advisors  */
16,16,1,1,-16,-16,-1,-1,0,                                    /* Horse     */
1,16,-1,-16,0,                                                /* Cannon    */
1,16,-1,-16,0                                                 /* Chariot   */
},
oo[32]={11,9,4,8,3,8,4,9,11},                  /* initial piece setup */
b[513],                                        /* board: 16x8+dummy, + PST */
T[8200],                                       /* hash translation table   */
centr[]={0,1,1,1,1,1,0,1,0,0},                 /* piece draws to center    */
n[]=".P*KEEQQAHCR????x+pkeeqqahcr????";        /* piece symbols on printout*/

char zn[] = {                                  /* zones of xiangqi board   */
1,1,1,1,1,2,2,2,2,2,    0,0,0,0,0,0,
1,1,1,1,1,2,2,2,2,2,    0,0,0,0,0,0,
1,1,1,1,1,2,2,2,2,2,    0,0,0,0,0,0,
0,0,0,1,1,2,2,0,0,0,    0,0,0,0,0,0,
0,0,0,1,1,2,2,0,0,0,    0,0,0,0,0,0,
0,0,0,1,1,2,2,0,0,0,    0,0,0,0,0,0,
1,1,1,1,1,2,2,2,2,2,    0,0,0,0,0,0,
1,1,1,1,1,2,2,2,2,2,    0,0,0,0,0,0,
1,1,1,1,1,2,2,2,2,2,    0,0,0,0,0,0
};

void pboard()
{int i;
 i=-1;W(++i<144)printf(" %c",(i&15)==10&&(i+=15-10)?10:n[b[i]&31]);
}

#if 0
D(k,q,l,e,z,n)          /* recursive minimax search, k=moving side, n=depth*/
int k,q,l,e,z,n;        /* (q,l)=window, e=current eval. score, E=e.p. sqr.*/
#endif
int D(int k,int q,int l,int e,int z,int n) 
{                       /* e=score, z=prev.dest; J,Z=hashkeys; return score*/
 int j,r,m,v,d,h,i,P,V,f=J,g=Z,C,s,flag,F;
 unsigned char t,p,u,x,y,X,Y,B,lu;
 struct _*a=A+(J+k&U-1);                       /* lookup pos. in hash table*/
 q-=q<e;l-=l<=e;                               /* adj. window: delay bonus */
 d=a->D;m=a->V;F=a->F;                         /* resume at stored depth   */
 X=a->X;Y=a->Y;                                /* start at best-move hint  */
if(z&S&&a->K==Z)printf("# root hit %d %d %x\n",a->D,a->V,a->F);
 if(a->K-Z|z&S  |                              /* miss: other pos. or empty*/
  !(m<=q|F&8&&m>=l|F&S))                       /*   or window incompatible */
  d=X=0,Y=-1;                                  /* start iter. from scratch */
 W(d++<n||d<3||              /*** min depth = 2   iterative deepening loop */
   z&S&&K==I&&(GetTickCount()-Ticks<tlim&d<=MaxDepth|| /* root: deepen upto time   */
   (K=X,L=Y,d=3)))                             /* time's up: go do best    */
 {x=B=X;lu=1;                                  /* start scan at prev. best */
  h=Y-255;                                       /* if move, request 1st try */
  P=d>2&&l+I?D(16-k,-l,1-l,-e,2*S,d-3):I;      /* search null move         */
  m=-P<l|R<5?d-2?-I:e:-P;   /*** prune if > beta  unconsidered:static eval */
  N++;                                         /* node count (for timing)  */
  do{u=b[x];                                   /* scan board looking for   */
   if(u)m=lu|u&15^3?m:(d=98,I),lu=u&15^3;        /* Kings facing each other  */
   if(u&&(u&16)==k)                            /*  own piece (inefficient!)*/
   {r=p=u&15;                                  /* p = piece type (set r>0) */
    j=od[p];                                   /* first step vector f.piece*/
    W(r=o[++j])                                /* loop over directions o[] */
    {A:                                        /* resume normal after best */
     flag=h?3:of[j];                           /* move modes (for fairies) */
     y=x;                                      /* (x,y)=move               */
     do{                                       /* y traverses ray, or:     */
      y=h?Y:y+r;                               /* sneak in prev. best move */
      if(y>=16*9|(y&15)>=10)break;            /* board edge hit           */
      t=b[y];                                  /* captured piece           */
      if(flag&1+!t)                            /* mode (capt/nonc) allowed?*/
      {if(t&&(t&16)==k||flag>>10&zn[y])break;  /* capture own or bad zone  */
       i=10*w[t&15];                           /* value of capt. piece t   */
       if(i<0)m=I,d=98;                        /* K capture                */
       if(m>=l&d>1)goto C;                     /* abort on fail high       */
       v=d-1?e:i-p;                            /*** MVV/LVA scoring if d=1**/
       if(d-!t>1)                              /*** all captures if d=2  ***/
       {v=centr[p]?b[x+257]-b[y+257]:0;        /* center positional pts.   */
        b[x]=0;b[y]=u;                         /* do move                  */
        v-=w[p]>0|R<10?0:20;                   /*** freeze K in mid-game ***/
        if(p<3)                                /* pawns:                   */
        {v+=2;                                 /* end-game Pawn-push bonus */
         if(zn[x]-zn[y])b[y]+=5,               /* upgrade Pawn and         */
          i+=w[p+5]-w[p];                      /*          promotion bonus */
        }
        if(z&S && PlyNr<6) v+=(rand()>>10&31)-16; // randomize in root
        J+=J(0);Z+=J(4);
        v+=e+i;V=m>q?m:q;                      /*** new eval & alpha    ****/
        C=d-1-(d>5&p>2&!t&!h);                 /* nw depth, reduce non-cpt.*/
        C=R<10|P-I|d<3||t&&p-3?C:d;            /* extend 1 ply if in-check */
        do
         s=C>2|v>V?-D(16-k,-l,-V,-v,/*** futility, recursive eval. of reply */
                                     0,C):v;
        W(s>q&++C<d); v=s;                     /* no fail:re-srch unreduced*/
        if(z&S&&K-I)                           /* move pending: check legal*/
        {if(v+I&&x==K&y==L)                    /*   if move found          */
         {Q=-e-i;
          if(O-I)a->D=99,a->V=500;             /* lock game in hash as loss*/
          O=P;PlyNr++;
          R-=i>>7;                             /*** total captd material ***/
          Fifty = t|p<3?0:Fifty+1;
          return l;}                           /*   & not in check, signal */
         v=m;                                  /* (prevent fail-lows on    */
        }                                      /*   K-capt. replies)       */
        J=f;Z=g;
        b[y]=t;b[x]=u;                         /* undo move                */
       }                                       /*          if non-castling */
       if(v>m)                                 /* new best, update max,best*/
        m=v,X=x,Y=y;                           /* no marking!              */
       if(h){h=0;goto A;}                      /* redo after doing old best*/
      }
      s=t;
      t+=flag&4;                               /* fake capt. for nonsliding*/
      if(s&&flag&8)t=0,flag^=flag>>4&15;       /* hoppers go to next phase */
      if(!(flag&S))                            /* zig-zag piece?           */
       r^=flag>>12,flag^=flag>>4&15;           /* alternate vector & mode  */
     }W(!t);                                   /* if not capt. continue ray*/
   }}
   if((++x&15)>=10)x=x+16&240,lu=1;            /* next sqr. of board, wrap */
   if(x>=16*9)x=0;
  }W(x-B);           
C:if(a->D<99)                                  /* protect game history     */
   a->K=Z,a->V=m,a->D=d,a->X=X,                /* always store in hash tab */
   a->F=8*(m>q)|S*(m<l),a->Y=Y;                /* move, type (bound/exact),*/
if(z&S&&Post){
  printf("%2d ",d-2);
  printf("%6d ",m);
  printf("%8d %10d %c%c%c%c\n",(GetTickCount()-Ticks)/10,N,
     'i'-(X>>4&15),'9'-(X&15),'i'-(Y>>4&15),'9'-(Y&15)),fflush(stdout);}
 }                                             /*    encoded in X S,8 bits */
 return m+=m<e;                                /* delayed-loss bonus       */
}

void
InitEngine()
{
 N=8100;W(N-->256)T[N]=rand()>>9;                  /* Zobrist random keys */
 srand(GetTickCount());
}

void
InitGame()
{
 int i; static int initDone;
 if(!initDone)initDone=1,InitEngine();

 for(i=0;i<16*9;i++)b[i]=0;           /* clear board   */
 b[23]=b[119]=10;b[18]=b[114]=26;     /* place Cannons */
 K=9;W(K--)
 {b[16*K]=(b[16*K+9]=oo[K])+16;       /* initial board setup */
  if(!(K&1))b[16*K+3]=18,b[16*K+6]=1; /* add Pawns     */
  L=10;W(L--)b[L+16*K+257]=(K-4.5)*(K-4.5)+(L-4)*(L-4); /* center-pts table*/
 }                                                   /*(in unused half b[])*/
 b[32]++;b[96]++;                                    /* adjust b elephants */
 Side=0; /* Side on move, red=0, back = 16 */
 PlyNr=Fifty=R=O=Q=R=0;
 for(i=0; i<10; i++) if(i!=3) R += (w[oo[i]]>>7) + (w[oo[i]]>>7);
 MovesLeft = MaxMoves; TimeLeft = MaxTime; /* initialize time control */
}

void _OnOpponentMove(const char *move)
{
 const char *c=move;
 //K=16*('i'-c[0])+'9'-c[1];
 //L=16*('i'-c[2])+'9'-c[3]; /* convert move string to internal formt */
 K=16*(c[0]-'0')+c[1]-'0';
 L=16*(c[2]-'0')+c[3]-'0'; /* convert move string to internal formt */
 if(D(Side,-I,I,Q,S,3)==I)
  Side ^= 16;  /* move was legal and is performed */
 else
 {
     LOGE("MaxQi says: Illegal move '%s' in position\n", move);
     pboard();
    // (Commented out by HUY) exit(0);
 }
}

const char *_GenerateNextMove()
{
 static char move[5];

 /* determine time to sepend on next move */
 Ticks = GetTickCount();                /* record starting time            */
 N = MovesLeft<=0 ? 40 : MovesLeft;     /* assume 40 movs for rest of game */
 tlim = (0.6-0.06*(10-8))*(TimeLeft+(N-1)*TimeInc)/(N+7);
 if(tlim>TimeLeft/15) tlim = TimeLeft/15;

 /* now call the AI */
 N=0;K=I;
 if (D(Side,-I,I,Q,S,3)!=I) sprintf(move, "none"); /* no move found */ else
 {/* legal move was found and played */
  Side ^= 16; /* other side moves next */
  //sprintf(move, "%c%c%c%c",'i'-(K>>4),'9'-(K&15),'i'-(L>>4&15),'9'-(L&15));
  sprintf(move, "%d%d%d%d",(K>>4),(K&15),(L>>4&15),(L&15));

  /* time-control accounting */
  N = GetTickCount() - Ticks;     /* determine time actually used for move */
  TimeLeft -= N;
  TimeLeft += TimeInc;
  if(--MovesLeft == 0)            /* new session starts                    */
  {MovesLeft = MaxMoves;        
   if(MaxMoves == 1)       /* assume non-accumulating TC if 1 move/session */
   TimeLeft  = MaxTime;
   else TimeLeft += MaxTime;
  }
 }
 return move;
}

///////////////////////////////////////////
//  Public API                           //
///////////////////////////////////////////

/*
 * Set AI 's difficulty level [1...10].
 */
jint
Java_com_playxiangqi_hoxchess_AIEngine_setDifficultyLevel( JNIEnv* env,
		                                                   jobject thiz,
		                                                   jint nAILevel )
{
    int actualLevel = 1;
    switch (nAILevel)
    {
        case 1: actualLevel = 6; break;
        case 2: actualLevel = 9; break;
        case 0: /* falls through */
        default: actualLevel = 2;
    }
    LOGI("setDifficultyLevel: nAILevel: [%d], actual [%d] \n", nAILevel, actualLevel);
    MaxDepth = actualLevel;
    return AI_RC_OK;
}

jint
Java_com_playxiangqi_hoxchess_AIEngine_initGame()
{
    InitGame();
    return AI_RC_OK;
}

jstring
Java_com_playxiangqi_hoxchess_AIEngine_generateMove( JNIEnv* env,
                                                      jobject thiz )
{
    const char* aiMove = _GenerateNextMove();
    LOGI("AI generated this move: (%s) \n", aiMove);

    char szMove[5] = {0, 0, 0, 0, 0 };
    szMove[0] = aiMove[1];
    szMove[1] = aiMove[0];
    szMove[2] = aiMove[3];
    szMove[3] = aiMove[2];
    LOGI("AI generated this move [FORMAT]: (%s) \n", szMove);

    return (*env)->NewStringUTF(env, szMove);
}

jint
Java_com_playxiangqi_hoxchess_AIEngine_onHumanMove( JNIEnv* env,
                                                    jobject thiz,
                                                    jint row1, jint col1,
                                                    jint row2, jint col2 )
{
    LOGI("onHumanMove(): [RAW]: (%d, %d) => (%d, %d) \n", row1, col1, row2, col2);

    char szMove[5] = {0, 0, 0, 0, 0 };
    szMove[0] = ('0' + col1);
    szMove[1] = ('0' + row1);
    szMove[2] = ('0' + col2);
    szMove[3] = ('0' + row2);

    LOGI("onHumanMove(): [FORMAT]: szMove (%s) \n", szMove);
    
    _OnOpponentMove( szMove );
    return AI_RC_OK;
}

/*
 * Get the AI 's information.
 */
jstring
Java_com_playxiangqi_hoxchess_AIEngine_getInfo( JNIEnv* env,
                                                jobject thiz )
{
	return (*env)->NewStringUTF(env, "H.G. Muller\n"
            "home.hccnet.nl/h.g.muller/XQhaqikid.html");
}

/************************* END OF FILE ***************************************/
