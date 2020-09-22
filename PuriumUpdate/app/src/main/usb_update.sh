trap '[[ $BASH_COMMAND != echo* ]] && echo $BASH_COMMAND' DEBUG

echo "Update Start."

echo "[0%] Checking Update Path in [$1]... "

echo "[5%] Checking Purium Update File ..."
if [ -f /storage/$1/purium.apk ];then
    echo "[15%] [O] Updating Purium App ..."
    /system/bin/pm install -i com.example.test -r -d /storage/udisk3/purium.apk
else
    echo "[15%] [X] Purium Update File is Not Exist"
fi
echo "[20%] Purium App Update Ended."

echo "[25%] Checking Manager Update File ..."
if [ -f /storage/$1/purium_manager.apk ];then
    echo "[35%] [O] Updating Manager App ..."
    /system/bin/pm install -i com.neocartek.purium.manager -r -d /storage/udisk3/purium_manager.apk
else
    echo "[35%] [X] Manager Update File is Not Exist"
fi
echo "[40%] Manager App Update Ended."

echo "[45%] Checking Update Update File ..."
if [ -f /storage/$1/purium_update.apk ];then
    echo "[55%] [O] Updating Update App ..."
    sleep 3
    echo "Success"
else
    echo "[55%] [X] Update Update File is Not Exist"
fi
echo "[60%] Update App Update Ended."

echo "[65%] Checking Media View Update File ..."
if [ -f /storage/$1/purium_media_view.apk ];then
    echo "[75%] [O] Updating Media View App ..."
    /system/bin/pm install -i com.awesomeit.purium -r -d /storage/udisk3/purium_media_view.apk
else
    echo "[75%] [X] Media View Update File is Not Exist"
fi
echo "[80%] Media View App Update Ended."

echo "[90%] Closing Update Process ..."

if [ -f /storage/$1/purium_update.apk ];then
    /system/bin/pm install -i com.neocartek.purium.update -r -d /storage/udisk3/purium_update.apk
fi

#echo "[100%] Reboot ..."
#sleep 3
#reboot