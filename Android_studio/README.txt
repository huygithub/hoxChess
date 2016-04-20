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


Images for pieces:
==================
They were downloaded as SVG files from: https://en.wikipedia.org/wiki/Xiangqi
Licence: Public Domain

For example,
  https://commons.wikimedia.org/wiki/File:Xiangqi_General_(Trad).svg

These SVG files were exported to PNG file to create the images for pieces.


Icons in the project:
=====================
* Some icons were downloaded from https://design.google.com/icons/index.html#ic_menu


Sound files:
============
* The sound files (.wav) were imported from the iOS app "hoxChess", which is also an open source one.
* They were then converted to "AAC audio file" (.m4a) using built-in app Mac OS X 10.11.4 "iTunes",
  under the menu item "iTunes | File | Create New Version | Create AAC version".

////////////////////// END OF FILE //////////////////////////////////////////
