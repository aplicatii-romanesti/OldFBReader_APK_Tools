1040127 is permissions ready, uploaded to google play.
1040128 is permissions ready, fixed missing compiled classes, pending uploaded to google play.
1040120 -> is 99% same like 1040128 (with small diffs, like: # for 1040120, we also compile org/geometerplus/fbreader/fbreader/FBReaderApp.java so we will extract the books based on condition: "if not my version, extract them", ovveriding even books with higher version.)


SEE: 1040123.cmd
# decompile old apk:
apktool.bat d 1040121_vechea_aplicatie_Android_3.0_sau_mai_nou.apk

# build new apk:
apktool.bat build 1040121_vechea_aplicatie_Android_3.0_sau_mai_nou -o 1040122.apk

# zip align and signing:
https://stablekernel.com/creating-keystores-and-signing-android-apps/
c:\_me\android\sdk\build-tools\28.0.3\apksigner.bat

## zipalign
c:\_me\android\sdk\build-tools\28.0.3\zipalign.exe -v -p 4 1040122.apk 1040122-aligned.apk

## sign
c:\_me\android\sdk\build-tools\28.0.3\apksigner.bat sign --ks aplicatii.romanesti-release-key.keystore --out 1040122-aligned-signed.apk 1040122-aligned.apk


## Links
https://support.google.com/faqs/answer/7668308
https://stackoverflow.com/questions/30774833/how-to-increment-versioncode-using-apktool - change version!!!
https://www.reddit.com/r/Android/comments/11852r/how_to_modify_an_apk/


EXAMPLE:
echo "build"
apktool.bat -f build 1040123 -o 1040123-unaligned-unsigned.apk
echo "build done"

REM # zip align and signing:
REM #https://stablekernel.com/creating-keystores-and-signing-android-apps/
REM #c:\_me\android\sdk\build-tools\28.0.3\apksigner.bat

REM ## zipalign
echo "zip aling"
c:\_me\android\sdk\build-tools\28.0.3\zipalign.exe -v -p 4 1040123-unaligned-unsigned.apk 1040123-aligned-unsigned.apk

REM ## sign
echo "sign"
c:\_me\android\sdk\build-tools\28.0.3\apksigner.bat sign --ks aplicatii.romanesti-release-key.keystore --out 1040123-aligned-signed.apk 1040123-aligned-unsigned.apk

echo "done"
REM ## Links
REM #https://support.google.com/faqs/answer/7668308
REM #https://stackoverflow.com/questions/30774833/how-to-increment-versioncode-using-apktool - change version!!!
REM #https://www.reddit.com/r/Android/comments/11852r/how_to_modify_an_apk/
