#!/bin/bash
# vim: ts=4 sts=4 sw=4 et

icons=(
    "add"
    "add alert"
    "add circle"
    "add circle outline"
    "add shopping cart"
    "alarm"
    "alarm add"
    "alarm off"
    "arrow back"
    "arrow downward"
    "arrow drop down"
    "arrow drop up"
    "arrow forward"
    "arrow upward"
    "audiotrack"
    "bookmark"
    "cancel"
    "chat bubble"
    "chat bubble outline"
    "check"
    "check box"
    "check circle"
    "clear"
    "credit card"
    "delete"
    "directions"
    "do not disturb"
    "edit"
    "error"
    "error outline"
    "event"
    "favorite"
    "favorite border"
    "feedback"
    "forward"
    "get app"
    "gps fixed"
    "help"
    "info"
    "keyboard arrow down"
    "keyboard arrow left"
    "keyboard arrow right"
    "keyboard arrow up"
    "loop"
    "loyalty"
    "mail"
    "mail outline"
    "map"
    "menu"
    "mic"
    "mic off"
    "more horiz"
    "more vert"
    "navigation"
    "new releases"
    "notifications"
    "notifications off"
    "pause"
    "pause circle filled"
    "pause circle outline"
    "phone"
    "photo camera"
    "place"
    "play arrow"
    "play circle filled"
    "play circle outline"
    "question answer"
    "redeem"
    "refresh"
    "remove"
    "remove circle"
    "remove circle outline"
    "remove shopping cart"
    "replay"
    "reply"
    "report"
    "schedule"
    "search"
    "send"
    "sentiment dissatisfied"
    "sentiment neutral"
    "sentiment satisfied"
    "settings"
    "share"
    "shopping basket"
    "shopping cart"
    "star"
    "star border"
    "star half"
    "stars"
    "stop"
    "thumb down"
    "thumb up"
    "thumbs up down"
    "vibration"
    "volume mute"
    "volume off"
    "volume up"
    "warning"
    "zoom in"
)

ICONS_REPO="../material-design-icons/"

cd "$(dirname "$0")"

# Ensure access to the repository
if ! [ -d "$ICONS_REPO" ]; then
    echo "Cloning repository…"
    git clone https://github.com/google/material-design-icons.git "$ICONS_REPO"
else
    echo "Updating repository…"
    cd "$ICONS_REPO"
    git pull origin master
    cd -
fi
echo

# Copy each listed icon and check for no matches
echo "Copying icons…"
for icon in "${icons[@]}"; do
    id="${icon// /_}"
    find "$ICONS_REPO" -type f -path '*/drawable-*' -name "ic_${id}_white_24dp.png" | (
        nothing=1
        while read file; do
            nothing=0
            density="${file#*drawable-}"
            density="${density%%/*}"
            cp "$file" "sdk/src/main/res/drawable-$density/ic_${id}_white_24dp.png"
        done
        if [ "$nothing" = "1" ]; then
            echo ICON NOT FOUNT: $icon
        fi
    )
done
echo

# Find duplicates and print them
echo "Checking duplicates…"
find sdk/src/main/res/ -type f -path '*/drawable-*' | xargs md5sum | sort | while read line; do
    md5="${line:0:32}"
    file="${line:34}"
    if [ "$md5" = "$prevmd5" ]; then
        echo "DUPLICATE: $prevfile <=> $file"
    else
        prevmd5="$md5"
        prevfile="$file"
    fi
done
echo
