echo "1. build"
apktool.bat -f build 1040126 -o 1040126-unaligned-unsigned.apk
echo "build done"

REM # zip align and signing:
REM #https://stablekernel.com/creating-keystores-and-signing-android-apps/
REM #c:\_me\android\sdk\build-tools\28.0.3\apksigner.bat

REM ## zipalign
echo "2. zip aling"
c:\_me\android\sdk\build-tools\28.0.3\zipalign.exe -v -p 4 1040126-unaligned-unsigned.apk 1040126-aligned-unsigned.apk

REM ## sign
echo "3. sign"
c:\_me\android\sdk\build-tools\28.0.3\apksigner.bat sign --ks aplicatii.romanesti-release-key.keystore --out 1040126-aligned-signed.apk 1040126-aligned-unsigned.apk

echo "done"
REM ## Links
REM #https://support.google.com/faqs/answer/7668308
REM #https://stackoverflow.com/questions/30774833/how-to-increment-versioncode-using-apktool - change version!!!
REM #https://www.reddit.com/r/Android/comments/11852r/how_to_modify_an_apk/
