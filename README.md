# SimpleAndroidDatasetsOrganizer

An Android application for organizing image datasets directly on your phone and automatically naming files for machine learning workflows.

This app lets you pick a target folder, capture photos with labels, and store them in a clean train/test/validation structure. It is designed for quick dataset creation and management without needing a desktop workflow.

## Features

- Select a root dataset folder using Android's Storage Access Framework
- Save photos into labeled folders such as `train`, `test`, and `validation`
- Automatically generate numbered file names like `0001.jpg`, `0002.png`, etc.
- Support one-click dataset splitting for test images into train/validation subsets
- Browse the dataset from the app and inspect images by folder
- Rename or delete files and folders directly from the device
- Works with labeled image collection for computer vision or ML experimentation

## Why this app?

Collecting and organizing image datasets on a phone is often tedious and error-prone. This project simplifies that process by:

- keeping all files in a consistent folder layout
- enforcing label-based directories
- reducing manual naming work
- helping prepare dataset splits for training workflows

## App workflow

1. Open the app and choose a folder to store your dataset.
2. Enter a label name (for example: `cat`, `dog`, `car`, `plant`).
3. Capture a photo from the camera.
4. The app saves the image into the selected dataset partition under the corresponding label folder.
5. Use the dataset viewer to review, rename, or delete items.
6. Split larger test sets automatically into train and validation folders.

## Project structure

```text
SimpleAndroidDatasetsOrganizer/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/femgr/datasetscreatorandorganizer/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── MainFragment.kt
│   │   │   │   ├── DatasetViewerActivity.kt
│   │   │   │   └── utils/
│   │   │   │       └── DatasetManager.kt
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── ...
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── README.md
└── .gitignore
```

## Main components

- `MainActivity` / `MainFragment`: main app flow for selecting folders, capturing images, and saving labeled data
- `DatasetViewerActivity`: browse the dataset, preview images, rename, and delete files
- `DatasetManager`: handles folder creation, naming, and train/test/validation splitting logic

## Technologies used

- Kotlin
- Android SDK 36
- AndroidX libraries
- Material Components
- Jetpack Compose dependencies in the project setup
- Storage Access Framework (SAF) for folder access

## Requirements

- Android Studio (recommended: latest stable version)
- Android device or emulator running Android 7.0+ (API 24+)
- Camera permission enabled for capturing photos

## Getting started

1. Clone the repository:

```bash
git clone https://github.com/FEMGR/SimpleAndroidDatasetsOrganizer.git
```

2. Open the project in Android Studio.

3. Let Gradle sync the project dependencies.

4. Connect an Android device or start an emulator.

5. Run the app from Android Studio.

## Usage

### 1. Select a dataset folder
When the app starts, it will ask you to choose the folder where your dataset should live. This folder is saved so you do not need to select it again on future launches.

### 2. Capture labeled images
Tap the capture button, enter a label, and take a photo. The app creates a folder for that label inside the chosen dataset section.

### 3. Manage the dataset
The dataset viewer allows you to:

- open folders
- inspect images
- rename items
- delete selected files
- keep the dataset organized by class

### 4. Split the dataset
If your test data contains enough images, the app can split them into `train` and `validation` folders using a common ratio.

## Notes

- The app uses Android's Storage Access Framework, so the selected folder remains accessible even when the app is restarted.
- File naming is automated to reduce manual work during dataset creation.
- The project is designed for experimentation and lightweight dataset collection on mobile devices.

## Contributing

Contributions are welcome. If you want to improve dataset organization, add new capture options, or enhance the viewer experience, feel free to open an issue or submit a pull request.

## License

This project does not currently include a license file. If you plan to distribute or reuse it, add an appropriate open-source license before publishing.
