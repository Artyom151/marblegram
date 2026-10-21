#!/bin/sh
# M3E coverage audit: "component | Telegram class file(s)". Done if any file mentions marble.
R=$(dirname "$0")/../../TMessagesProj/src/main/java/org/telegram
total=0; done=0
while IFS='|' read -r name files; do
	[ -z "$name" ] && continue
	total=$((total+1)); ok=0
	for f in $files; do grep -qi marble "$R/$f" 2>/dev/null && ok=1; done
	[ $ok = 1 ] && { done=$((done+1)); echo "[x] $name"; } || echo "[ ] $name  ($files)"
done <<'LIST'
Color scheme / dynamic color|ui/ActionBar/Theme.java
Typography (Roboto Flex)|messenger/AndroidUtilities.java marble/MarbleTypography.java
Top app bar|ui/ActionBar/ActionBar.java
Large flexible top app bar|marble/MarbleSettingsActivity.java
Action menu / overflow|ui/ActionBar/ActionBarMenuItem.java
Menu (popup)|ui/ActionBar/ActionBarPopupWindow.java
Dialog|ui/ActionBar/AlertDialog.java
Bottom sheet|ui/ActionBar/BottomSheet.java
Snackbar|ui/Components/Bulletin.java
Undo snackbar|ui/Components/UndoView.java
Loading indicator|ui/Components/RadialProgressView.java
Circular wavy progress|marble/WavyProgress.java
Linear wavy progress|ui/Components/LineProgressView.java
Media download progress|ui/Components/RadialProgress.java ui/Components/MediaActionDrawable.java
Inline spinner|ui/Components/InfiniteProgress.java
Skeleton loading|ui/Components/FlickerLoadingView.java
Switch|ui/Components/Switch.java
Checkbox|ui/Components/CheckBoxSquare.java
Selection checkbox (round)|ui/Components/CheckBoxBase.java
Radio button|ui/Components/RadioButton.java
Slider|ui/Components/SeekBarView.java
Media slider|ui/Components/SeekBar.java
Video slider|ui/Components/VideoPlayerSeekBar.java
Button (filled)|ui/Stories/recorder/ButtonWithCounterView.java
Progress button|ui/Components/ProgressButton.java
FAB / FAB menu|marble/MarbleFabMenu.java
Button group / chips (folders)|ui/Components/FilterTabsView.java
Tabs|ui/Components/ScrollSlidingTextTabStrip.java
Navigation bar (floating)|ui/Components/glass/GlassTabView.java
Search bar|ui/Components/SearchField.java
Text field|ui/Components/EditTextBoldCursor.java
List item (text)|ui/Cells/TextCell.java
List item (switch)|ui/Cells/TextCheckCell.java
List item (settings)|ui/Cells/TextSettingsCell.java
List item (radio, uses RadioButton)|ui/Cells/RadioColorCell.java ui/Cells/TextRadioCell.java ui/Components/RadioButton.java
List item (user, avatar shapes)|ui/Cells/UserCell.java messenger/ImageReceiver.java
Section header|ui/Cells/HeaderCell.java
Section footer|ui/Cells/TextInfoPrivacyCell.java
Segmented list groups|ui/Components/UniversalRecyclerView.java
Chat list item|ui/Cells/DialogCell.java
Message bubble|ui/ActionBar/MessageDrawable.java
Message cell|ui/Cells/ChatMessageCell.java
Input / compose bar|ui/Components/ChatActivityEnterView.java
Chat screen bars|ui/ChatActivity.java
Media grid|ui/Cells/SharedPhotoVideoCell2.java
Avatar shapes|messenger/ImageReceiver.java
Badge (unread)|ui/Cells/DialogCell.java
Tooltip|ui/Stories/recorder/HintView2.java
Now-playing bar|ui/Components/FragmentContextView.java
Empty state|ui/Components/StickerEmptyView.java
Number picker|ui/Components/NumberPicker.java
Fast scroller|ui/Components/RecyclerListView.java
Rating|ui/Components/BetterRatingView.java
Chart|ui/Components/CacheChart.java
Attach sheet (extends BottomSheet)|ui/Components/ChatAttachAlert.java ui/ActionBar/BottomSheet.java
Settings/profile screen|ui/ProfileActivity.java ui/Components/ProfileActionsView.java
Call toggle buttons|ui/Components/voip/VoIPToggleButton.java
Emoji/sticker keyboard tabs|ui/Components/ScrollSlidingTabStrip.java
Story progress segments|ui/Stories/StoryLinesDrawable.java
Photo editor sliders|ui/Components/PhotoEditorSeekBar.java
Paint tools selector|ui/Components/Paint/Views/PaintToolsView.java
Telegram bundled fonts -> Roboto Flex|messenger/AndroidUtilities.java
LIST
echo "M3E coverage: $done/$total = $((done*100/total))%"
