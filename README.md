# LyricViewX

[![](https://www.jitpack.io/v/Moriafly/LyricViewX.svg)](https://www.jitpack.io/#Moriafly/LyricViewX)

LyricViewX is a view to show lyrics. It based on [LrcView](https://github.com/zion223/NeteaseCloudMusic-MVVM).

## What's new
- 100% Kotlin.
- It's new and beautiful.
- It's easy to use.

## How to use

### Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```
allprojects {
    repositories {
        ...
        maven { url 'https://www.jitpack.io' }
    }
}
```
### Step 2. Add the dependency
```
dependencies {
    implementation 'com.github.Moriafly:LyricViewX:1.0.1'
}
```

### APIs
| Function | Descriptions |
| ---      |    ---       |
| loadLrc() | |
| setNormalColor() | Sets the font color for lyrics that are not on the current line |
| ... | ... |



## License

    LyricViewX  Copyright (C) 2021  Moriafly
    This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
    This is free software, and you are welcome to redistribute it
    under certain conditions; type `show c' for details.