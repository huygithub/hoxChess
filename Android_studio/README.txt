How to build:
=============

Make sure to build the .so libraries first under the 'jni' folder:
 $ cd app/src/main/jni/
 $ ndk-build

The above commands will create the necessary .so files:
./app/src/main/libs/armeabi/libAI_MaxQi.so
./app/src/main/libs/armeabi/libReferee.so
./app/src/main/libs/armeabi-v7a/libAI_MaxQi.so
./app/src/main/libs/armeabi-v7a/libReferee.so
./app/src/main/libs/mips/libAI_MaxQi.so
./app/src/main/libs/mips/libReferee.so
./app/src/main/libs/x86/libAI_MaxQi.so
./app/src/main/libs/x86/libReferee.so
