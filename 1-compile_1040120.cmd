echo modify java files

echo #set cp=org/geometerplus/android/fbreader

javac -cp 1040126-aligned-signed-dex2jar.jar;android_26_r01.jar;support_23.4.0_classes.jar org/geometerplus/android/fbreader/FBReader.java || goto :error

# for 1040120, we also compile this so we will extract the books based on condition: "if not my version, extract them", ovveriding even books with higher version.
javac -cp 1040126-aligned-signed-dex2jar.jar;android_26_r01.jar;support_23.4.0_classes.jar org/geometerplus/fbreader/fbreader/FBReaderApp.java || goto :error

rem support_v13_26.1.0_classes.jar
rem javac -cp 1040120-aligned-signed-dex2jar.jar;android_26_r01.jar;androidx-core-1.0.2.jar;androidx-annotation-1.0.2.jar org/geometerplus/android/fbreader/FBReader.java

rem set /p ready=Ready for dex?
rem ### rem when you want to put above command in a batch file.cmd or file.bat, replace %x with %%x 
rem Older 2del: for %%x in (FBReader$1 FBReader$2 FBReader$3$1 FBReader$3$2 FBReader$3 FBReader$4 FBReader$5 FBReader$TipRunner FBReader) do (
for %%x in (FBReader$1 FBReader$2 FBReader$3 FBReader$4 FBReader$5 FBReader$6 FBReader$7$1 FBReader$7$2 FBReader$7 FBReader$8 FBReader$9 FBReader$TipRunner FBReader ) do (

 rm -f dex_out/%%x.dex
 c:\_me\android\sdk\build-tools\28.0.3\dx.bat --dex --output=dex_out/%%x.dex org/geometerplus/android/fbreader/%%x.class
 java -jar baksmali-2.2.7.jar dis dex_out/%%x.dex 

 rm -f c:/_me/android/FBReader-OLD_APK_FIXES/1040120/smali/org/geometerplus/android/fbreader/%%x.smali
 cp out/org/geometerplus/android/fbreader/%%x.smali c:/_me/android/FBReader-OLD_APK_FIXES/1040120/smali/org/geometerplus/android/fbreader/

) || goto :error

for %%x in (FBReaderApp$1 FBReaderApp$2 FBReaderApp$3$1 FBReaderApp$3$2 FBReaderApp$3 FBReaderApp$BookmarkDescription FBReaderApp$CancelActionDescription FBReaderApp$CancelActionType FBReaderApp$ImageTappingAction FBReaderApp$WordTappingAction FBReaderApp) do (

 rm -f dex_out/%%x.dex
 c:\_me\android\sdk\build-tools\28.0.3\dx.bat --dex --output=dex_out/%%x.dex org/geometerplus/fbreader/fbreader/%%x.class
 java -jar baksmali-2.2.7.jar dis dex_out/%%x.dex 

 rm -f c:/_me/android/FBReader-OLD_APK_FIXES/1040120/smali/org/geometerplus/fbreader/fbreader/%%x.smali
 cp out/org/geometerplus/android/fbreader/%%x.smali c:/_me/android/FBReader-OLD_APK_FIXES/1040120/smali/org/geometerplus/fbreader/fbreader/

) || goto :error

rm -f 1040120-*.apk || goto :error
java -jar -Duser.language=en -Dfile.encoding=UTF8 apktool_2.4.0.jar -f build c:/_me/android/FBReader-OLD_APK_FIXES/1040120/ -o 1040120-unaligned-unsigned.apk || goto :error
echo "build done"
echo "2. zip aling"
c:\_me\android\sdk\build-tools\28.0.3\zipalign.exe -v -p 4 1040120-unaligned-unsigned.apk 1040120-aligned-unsigned.apk || goto :error
echo "3. sign"
c:\_me\android\sdk\build-tools\28.0.3\apksigner.bat sign --ks c:/_me/android/FBReader-OLD_APK_FIXES/aplicatii.romanesti-release-key.keystore --out 1040120-aligned-signed.apk 1040120-aligned-unsigned.apk || goto :error
echo "new apk done"

2-build-apk.cmd

:error
echo Failed with error #%errorlevel%.
exit /b %errorlevel%