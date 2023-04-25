#!/bin/sh
# clean first avoid copy failure
./gradlew clean

# assemble aar
if [ "$1" == 'mtkr' ] || [ "$1" == 'mr' ]; then
  echo "assemble MTK Vendor Release..."
  ./gradlew :newcall_library:assembleMtkRelease
elif [ "$1" == 'mtkd' ] || [ "$1" == 'md' ]; then
  echo "assemble Mtk vendor Debug..."
  ./gradlew :newcall_library:assembleMtkDebug

elif [ "$1" == 'zr' ]; then
  echo "assemble ZTE on Qcom Release..."
  ./gradlew :newcall_library:assembleQualcommZTERelease
elif [ "$1" == 'zd' ]; then
  echo "assemble ZTE on Qcom Debug..."
  ./gradlew :newcall_library:assembleQualcommZTEDebug

elif [ "$1" == 'xr' ]; then
  echo "assemble Xiaomi on Qcom Release..."
  ./gradlew :newcall_library:assembleQualcommMiRelease
elif [ "$1" == 'xd' ]; then
  echo "assemble Xiaomi on Qcom Debug..."
  ./gradlew :newcall_library:assembleQualcommMiDebug

elif [ "$1" == 'qr' ]; then
  echo "assemble Qcom Release..."
  ./gradlew :newcall_library:assembleQualcommRelease
elif [ "$1" == 'qd' ]; then
  echo "assemble Qcom Debug..."
  ./gradlew :newcall_library:assembleQualcommDebug

elif [ "$1" == 'Samsungr' ] || [ "$1" == 'ssr' ]; then
  echo "assemble Samsung Vendor Release..."
  ./gradlew :newcall_library:assembleSamsungRelease
elif [ "$1" == 'Samsungd' ] || [ "$1" == 'ssd' ]; then
  echo "assemble Samsung vendor Debug..."
  ./gradlew :newcall_library:assembleSamsungDebug

elif [ "$1" == 'Spreadtrumr' ] || [ "$1" == 'sr' ]; then
  echo "assemble Spreadtrum vendor Release..."
  ./gradlew :newcall_library:assembleSpreadtrumRelease
elif [ x"$1" = x ] || [ "$1" == 'Spreadtrumd' ] || [ "$1" == 'sd' ]; then
  echo "assemble Spreadtrum vendor Debug..."
  ./gradlew :newcall_library:assembleSpreadtrumDebug
fi

# 2nd param 'i' indicates Install dialer
if [ ! "$2" ]; then
  echo "do not install dialer"
  cp newcall_library/build/outputs/aar/NewCall_*.aar  ../cmcc5gnewcall-dialer/app/libs/newcall_library.aar
elif [ "$2" == 'i' ]; then
  echo "install dialer..."
  cp newcall_library/build/outputs/aar/NewCall_*.aar  ../cmcc5gnewcall-dialer/app/libs/newcall_library.aar
  cd ../cmcc5gnewcall-dialer || exit
  ./gradlew clean installDebug
fi
