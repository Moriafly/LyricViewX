# LyricViewX

[![](https://www.jitpack.io/v/Moriafly/LyricViewX.svg)](https://www.jitpack.io/#Moriafly/LyricViewX)

LyricViewX is a beautiful Lyrics control for Android.

Based on [LrcView](https://github.com/zion223/NeteaseCloudMusic-MVVM/blob/master/lib_common_ui/src/main/java/com/netease/lib_common_ui/lrc/LrcView.java) design.

[中文点击这里](/README-zh-CN.md)

## Compare with LrcView

- 100% Kotlin code
- Interaction optimization, removing outdated methods
- Provide the JitPack Library for easy use
- Provide new functionality

## How to use 

Step 1. Add the JitPack repository to the build file
Add it to build.gradle(root) :

```
allprojects {
    repositories {
        ...
        maven { url 'https://www.jitpack.io' }
    }
}
```

Step 2. Add dependencies

```
dependencies {
    implementation 'com.github.Moriafly:LyricViewX:1.1.3'
}
```

## Functions
```kotlin
/**
 * Set font color for non-current line lyrics [normalColor]
 */
fun setNormalColor(normalColor: Int)

/**
 * font size of normal lyrics text [size], unit px
 */
fun setNormalTextSize(size: Float)

/**
 * Current text size of lyrics
 */
fun setCurrentTextSize(size: Float)

/**
 * Sets the font color for the current line of lyrics
 */
fun setCurrentColor(currentColor: Int)

/**
 * Sets the font color selected when dragging lyrics
 */
fun setTimelineTextColor(timelineTextColor: Int)

/**
 * Sets the color of the timeline when dragging lyrics
 */
fun setTimelineColor(timelineColor: Int)

/**
 * Sets the font color to the right when dragging lyrics
 */
fun setTimeTextColor(timeTextColor: Int)

/**
 * Set lyrics to the text [label] displayed in the center of the screen when empty, such as "No lyrics yet".
 */
fun setLabel(label: String)

/**
 * Load the lyrics file
 * Lyric timestamps need to be consistent in both languages
 * @param mainLyricFile First language lyrics file
 * @param secondLyricFile Optional, second language song lyrics file
 */
fun loadLyric(mainLyricFile: File, secondLyricFile: File? = null)

/**
 * Load the lyric text
 * Lyric timestamps need to be consistent in both languages
 * @param mainLyricText First language lyric text
 * @Param secondLyricText optional, second language lyric text
 */
fun loadLyric(mainLyricText: String?, secondLyricText: String? = null)

/**
 * Load online lyrics
 * @Param lyricUrl The web address of the lyrics file
 * @param charset encoding format
 */
fun loadLyricByUrl(lyricUrl: String, charset: String? = "utf-8")

/**
 * Refresh lyrics
 * @param time Current playback time
 */
fun updateTime(time: Long)

/**
 * Sets whether dragging of lyrics is allowed
 * @Param draggable whether dragging is allowed
 * @Param onPlayClickListener sets lyrics drag after playback button click listener, if drag is allowed, it cannot be null
 */
fun setDraggable(draggable: Boolean, onPlayClickListener: OnPlayClickListener?)

/**
 * Set click
 */
fun setOnSingerClickListener(onSingerClickListener: OnSingleClickListener?)

/**
 * @NewAdded
 * Get the current lyrics of each line entity, can be used for lyrics sharing
 * @Return LyricEntry collection
 */
fun getLyricEntryList(): List<LyricEntry>

/**
 * When the play button is clicked, it should jump to the specified play position
 * @return whether the event was successfully consumed, if so, the UI will be updated
 */
fun onPlayClick(time: Long): Boolean

/**
 * Click events
 */
fun onClick()

```

## License

    LyricViewX  Copyright (C) 2021  Moriafly
    This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
    This is free software, and you are welcome to redistribute it
    under certain conditions; type `show c' for details.