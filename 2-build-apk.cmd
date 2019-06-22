rm -f 1040128-*.apk
java -jar -Duser.language=en -Dfile.encoding=UTF8 apktool_2.4.0.jar -f build c:/_me/android/FBReader-OLD_APK_FIXES/1040128/ -o 1040128-unaligned-unsigned.apk
echo "build done"
echo "2. zip aling"
c:\_me\android\sdk\build-tools\28.0.3\zipalign.exe -v -p 4 1040128-unaligned-unsigned.apk 1040128-aligned-unsigned.apk
echo "3. sign"
c:\_me\android\sdk\build-tools\28.0.3\apksigner.bat sign --ks c:/_me/android/FBReader-OLD_APK_FIXES/aplicatii.romanesti-release-key.keystore --out 1040128-aligned-signed.apk 1040128-aligned-unsigned.apk
echo "new apk done"