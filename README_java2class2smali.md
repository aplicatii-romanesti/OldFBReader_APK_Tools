eu am nev 11 pana la 23 (6.0).
exact in 23 incepe sa ceara perms sdcard at runtime.
targetSdk necesar pt upload new apk e 26 (M)

support library version should be the same like compileSdk version (https://stackoverflow.com/questions/51147404/what-support-library-version-should-we-use-with-targetsdk-28)
support library 26.0.0 ->> min Sdk 14 ! (https://developer.android.com/topic/libraries/support-library/revisions)

DAR, eu nu am gasit toate classes.jar pentru support lib. 26 (care e support v4 + v7 + v8? +v13). Asa ca am lasat minsdk tot 11.
Asa ca am folosit support v4 de la 23, le-am scos din aar si am facut: support_23.4.0_classes.jar si support_23.4.0_internal_impl-23.4.0.jar (pt compile time).
Cu dex2jar (cu script jar2dex) le-am facut si dex (support_23.4.0_classes-jar2dex.dex si support_23.4.0_internal_impl-23.4.0-jar2dex.dex) si apoi smali, si le-am pus in build-ul pentru apk (pt apk build time), pentru ca ele nu exista pe tel, ci trebuie sa le aduca apk-ul.

example: ContextCompat, de la 22.1.0
https://developer.android.com/reference/android/support/v4/app/ActivityCompat

smali-2.2.7.jar ->> assemble(ass,as,a) - Assembles smali files into a dex file.

baksmali-2.2.7.jar ->>  disassemble(dis,d) - Disassembles a dex file.

PREPS:
Dex2jar pt a face jar-ul 1040126-aligned-signed-dex2jar.jar.

1. modify java files

#set cp=org/geometerplus/android/fbreader

javac -cp 1040126-aligned-signed-dex2jar.jar;android_26_r01.jar;support_23.4.0_classes.jar org/geometerplus/android/fbreader/FBReader.java

### rem when you want to put above command in a batch file.cmd or file.bat, replace %x with %%x 
for %x in (FBReader$1 FBReader$2 FBReader$3$1 FBReader$3$2 FBReader$3 FBReader$4 FBReader$5 FBReader$TipRunner FBReader) do (
 rm -f dex_out/%x.dex
 c:\_me\android\sdk\build-tools\28.0.3\dx.bat --dex --output=dex_out/%x.dex org/geometerplus/android/fbreader/%x.class
 java -jar baksmali-2.2.7.jar dis dex_out/%x.dex 

 rm -f c:/_me/android/FBReader-OLD_APK_FIXES/1040126-perms/smali/org/geometerplus/android/fbreader/%x.smali
 cp out/org/geometerplus/android/fbreader/%x.smali c:/_me/android/FBReader-OLD_APK_FIXES/1040126-perms/smali/org/geometerplus/android/fbreader/

)

rm -f 1040126-perms-*.apk
java -jar -Duser.language=en -Dfile.encoding=UTF8 apktool_2.4.0.jar -f build c:/_me/android/FBReader-OLD_APK_FIXES/1040126-perms/ -o 1040126-perms-unaligned-unsigned.apk
echo "build done"
echo "2. zip aling"
c:\_me\android\sdk\build-tools\28.0.3\zipalign.exe -v -p 4 1040126-perms-unaligned-unsigned.apk 1040126-perms-aligned-unsigned.apk
echo "3. sign"
c:\_me\android\sdk\build-tools\28.0.3\apksigner.bat sign --ks c:/_me/android/FBReader-OLD_APK_FIXES/aplicatii.romanesti-release-key.keystore --out 1040126-perms-aligned-signed.apk 1040126-perms-aligned-unsigned.apk
echo "new apk done"