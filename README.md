# Remindo

I made this little Android app for my sister. She wanted a nudge now and then, but she also wanted to keep a record and see how she was doing. Water was a good example. A reminder every few hours is handy, but who wants to fill out a form every time they pick up a glass? So the app nudges her during the day and asks just once in the evening how it went.

The kitty is there because she has little kitty things in her car. When I was deciding what the app should look like, that part was easy.

If it sounds useful to you too, you're welcome to use it, change the skin, or make your own version. I hope it helps you keep track of something you care about.

**[Download the Android APK](downloads/Remindo.apk?raw=true)** · **[Open the PDF quick guide](output/pdf/Remindo-Quick-Guide.pdf)** · **[Changelog](CHANGELOG.md)**

![Three screens from the installed app: today's reminders, the evening water check-in, and the saved history entry.](preview/app-preview.png)

## A day with it

1. In **Today**, tap **+ New**. Set a one-off date or repeat every few days, weeks, or months. Pick the weekdays for a daily task.
2. Set nudges and feedback separately. A weekly task can nudge on several weekdays, several times per day, then ask once on Sunday evening. You can turn off nudges and keep the check-in.
3. Open **Awaiting feedback** on Today to settle old check-ins. Choose Yes, No, Partial, Not Applicable, or Others, with an optional comment. Unanswered items stay pending.
4. In **History**, pick a day for a recap or a range for day counts by task and outcome. Tap a count to see its dates and comments. For older tasks with several check-ins per day, one date can appear under each outcome recorded that day.

If you already have Ammu Reminders installed, open the new APK and tap **Update** to keep your history. It works offline on Android 8 or newer. After installation, open its **Settings** to enable notifications and precise alarms. Your schedules and feedback stay on your phone.

<details>
<summary><b>Make it yours: change the skin</b></summary>

The skin is part of the app source. Fork or download this project, change the files below, then rebuild the APK.

| To change | Edit |
| --- | --- |
| Colors, text sizes, buttons, cards | [assets/style.css](assets/style.css). For example, change the primary button background from strawberry pink (`#b8315c`) to teal (`#286d65`) in the `.primary` rule. |
| Today and check-in artwork | Replace [water.jpg](assets/water.jpg) and [evening.jpg](assets/evening.jpg) with landscape pictures of your own. Keep the filenames. Update the image description in the `art` function in [app.js](assets/app.js) for screen readers. |
| Home screen icon | Replace [kitty.png](res/drawable-nodpi/kitty.png) with a square PNG. |
| Display name and headings | Edit `android:label` in [AndroidManifest.xml](AndroidManifest.xml) and the headings in [app.js](assets/app.js). |

</details>

<details>
<summary><b>Build your own APK on Windows</b></summary>

Use Python 3, a Windows x64 JDK 17, Android SDK Platform 35, and Android Build Tools 35.0.0. Put the complete tool directories under `.tools` so these paths exist:

    .tools/jdk-17/bin/javac.exe
    .tools/build-tools-35/aapt.exe
    .tools/build-tools-35/lib/d8.jar
    .tools/android-35/android.jar

Run `python build.py` from the project folder. It creates and verifies `Remindo.apk`. On your first build it generates your own signing key in `.signing/`. Back up that folder privately so future updates signed by you can install over your earlier builds. A build signed with your key cannot update the downloadable APK in place.

To run the recurrence checks with JDK 17 on PATH:

    javac -d build/rules src/Rule.java tests/RuleCheck.java
    java -cp build/rules app.ammu.reminders.RuleCheck

</details>

[MIT license](LICENSE)
