rem 1040129 vs 128: 129 has android exported false in 99% of the androidmanifest, as a preventive measure
rm -f 1040129-*.apk
java -jar -Duser.language=en -Dfile.encoding=UTF8 apktool_2.4.0.jar -f build c:/_me/android/FBReader-OLD_APK_FIXES/1040129/ -o 1040129-unaligned-unsigned.apk
echo "build done"
echo "2. zip aling"
c:\_me\android\sdk\build-tools\28.0.3\zipalign.exe -v -p 4 1040129-unaligned-unsigned.apk 1040129-aligned-unsigned.apk
echo "3. sign"
c:\_me\android\sdk\build-tools\28.0.3\apksigner.bat sign --ks c:/_me/android/FBReader-OLD_APK_FIXES/aplicatii.romanesti-release-key.keystore --out 1040129-aligned-signed.apk 1040129-aligned-unsigned.apk
echo "new apk done"