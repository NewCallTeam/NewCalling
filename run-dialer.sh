#!/bin/sh
if [ "$1" == 'mtkr' ] || [ "$1" == 'mr' ]; then
  echo "assemble MTK Vendor Release..."
  ./gradlew :newcall_library:assembleMtkRelease
  cp newcall_library/build/outputs/aar/newcall_library-Mtk-release.aar  ../cmcc5gnewcall-dialer/app/libs/newcall_library.aar
elif [ "$1" == 'mtkd' ] || [ "$1" == 'md' ]; then
  echo "assemble Mtk vendor Debug..."
  ./gradlew :newcall_library:assembleMtkDebug
  cp newcall_library/build/outputs/aar/newcall_library-Mtk-debug.aar  ../cmcc5gnewcall-dialer/app/libs/newcall_library.aar

elif [ "$1" == 'Qcomr' ] || [ "$1" == 'qr' ]; then
  echo "assemble Qcom Vendor Release..."
  ./gradlew :newcall_library:assembleQcomRelease
  cp newcall_library/build/outputs/aar/newcall_library-Qcom-release.aar  ../cmcc5gnewcall-dialer/app/libs/newcall_library.aar
elif [ "$1" == 'Qcomd' ] || [ "$1" == 'qd' ]; then
  echo "assemble Qcom vendor Debug..."
  ./gradlew :newcall_library:assembleQcomDebug
  cp newcall_library/build/outputs/aar/newcall_library-Qcom-debug.aar  ../cmcc5gnewcall-dialer/app/libs/newcall_library.aar

elif [ "$1" == 'Samsungr' ] || [ "$1" == 'ssr' ]; then
  echo "assemble Samsung Vendor Release..."
  ./gradlew :newcall_library:assembleSamsungRelease
  cp newcall_library/build/outputs/aar/newcall_library-Samsung-release.aar  ../cmcc5gnewcall-dialer/app/libs/newcall_library.aar
elif [ "$1" == 'Samsungd' ] || [ "$1" == 'ssd' ]; then
  echo "assemble Samsung vendor Debug..."
  ./gradlew :newcall_library:assembleSamsungDebug
  cp newcall_library/build/outputs/aar/newcall_library-Samsung-debug.aar  ../cmcc5gnewcall-dialer/app/libs/newcall_library.aar
elif [ "$1" == 'Spreadtrumr' ] || [ "$1" == 'sr' ]; then
  echo "assemble Spreadtrum vendor Release..."
  ./gradlew :newcall_library:assembleSpreadtrumRelease
  cp newcall_library/build/outputs/aar/newcall_library-Spreadtrum-release.aar  ../cmcc5gnewcall-dialer/app/libs/newcall_library.aar
elif [ x"$1" = x ] || [ "$1" == 'Spreadtrumd' ] || [ "$1" == 'sd' ]; then
  echo "assemble Spreadtrum vendor Debug..."
  ./gradlew :newcall_library:assembleSpreadtrumDebug
  cp newcall_library/build/outputs/aar/newcall_library-Spreadtrum-debug.aar  ../cmcc5gnewcall-dialer/app/libs/newcall_library.aar
fi
# 2nd param 'i' indicates Install dialer
if [ ! "$2" ]; then
  echo "do not install dialer"
elif [ "$2" == 'i' ]; then
  echo "install dialer..."
  cd ../cmcc5gnewcall-dialer || exit
  ./gradlew clean installDebug
fi
