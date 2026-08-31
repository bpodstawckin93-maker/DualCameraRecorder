# Dual Camera Recorder

Android приложение для одновременной записи видео с двух камер (передней и задней) на устройствах с Android 11+, используя CameraX. Приложение аналогично Rode Capture для iOS.

## Возможности

✅ **Двойная запись камер** - Одновременная запись видео с передней и задней камер
✅ **Режим PiP (Picture-in-Picture)** - Вспомогательная камера отображается в углу основного видео
✅ **Drag-and-Drop** - Перемещение окна PiP по экрану
✅ **Масштабирование** - Увеличение/уменьшение размера окна PiP двумя пальцами
✅ **Переключение камер** - Быстрое переключение между передней и задней камерой
✅ **Паузировка/Возобновление** - Приостановка записи без завершения (API 31+)
✅ **Таймер записи** - Отображение времени записи в реальном времени
✅ **Высокое качество** - Запись в максимально доступном качестве устройства

## Требования

- Android 11 (API 30) или выше
- Устройство с двумя камерами (передней и задней)
- Разрешения: Camera, Microphone, Storage

## Структура проекта

```
app/src/main/
├── java/com/example/dualcamerarecorder/
│   ├── MainActivity.kt                 # Главная Activity
│   ├── camera/
│   │   ├── CameraManager.kt            # Управление камерами
│   │   ├── DualCameraRecorder.kt       # Логика двойной записи (устарело)
│   │   └── DualCameraVideoRecorder.kt  # Новая реализация записи
│   └── ui/
│       ├── DragDropScalablePreviewView.kt  # Предпросмотр с PiP
│       └── RecordingControlsView.kt        # Элементы управления
├── res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   ├── view_dual_camera_preview.xml
│   │   └── view_recording_controls.xml
│   ├── drawable/
│   │   ├── preview_border.xml
│   │   └── recording_indicator.xml
│   └── values/
│       ├── colors.xml
│       ├── strings.xml
│       └── themes.xml
└── AndroidManifest.xml
```

## Установка и запуск

### 1. Клонирование репозитория

```bash
git clone https://github.com/bpodstawckin93-maker/DualCameraRecorder.git
cd DualCameraRecorder
```

### 2. Откройте в Android Studio

```bash
# macOS
open -a "Android Studio" .

# Linux
android-studio .

# Windows
studio64.exe .
```

### 3. Синхронизируйте Gradle

File → Sync Now

### 4. Запустите приложение

- Выберите физическое устройство или эмулятор (требуется минимум 2 камеры)
- Нажмите Run → Run 'app'

## Использование

### Основные элементы управления

| Кнопка | Функция |
|--------|----------|
| **Start Recording** | Начать/остановить запись | 
| **Switch** | Переключить камеру в полноэкранном режиме |
| **Pause** | Приостановить/возобновить запись (доступна во время записи) |
| **PiP** | Активировать режим картинка-в-картинке |

### Режим PiP

1. Нажмите кнопку **PiP** для активации режима
2. **Перемещение**: Перетащите окно PiP в нужное место
3. **Масштабирование**: Используйте два пальца (pinch-to-zoom) для изменения размера
4. **Сброс масштаба**: Двойной тап для возврата к исходному размеру

### Сохранение видео

Видеофайлы автоматически сохраняются в:
```
/sdcard/Android/data/com.example.dualcamerarecorder/files/videos/
```

Файлы:
- `back_YYYY-MM-DD-HH-mm-ss-SSS.mp4` - видео с задней камеры
- `front_YYYY-MM-DD-HH-mm-ss-SSS.mp4` - видео с передней камеры

## Зависимости

```gradle
// CameraX
implementation 'androidx.camera:camera-core:1.1.0'
implementation 'androidx.camera:camera-camera2:1.1.0'
implementation 'androidx.camera:camera-lifecycle:1.1.0'
implementation 'androidx.camera:camera-video:1.1.0'

// AndroidX
implementation 'androidx.appcompat:appcompat:1.4.1'
implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.4.1'
implementation 'androidx.core:core-ktx:1.7.0'
implementation 'com.google.android.material:material:1.5.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.3'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.1'
```

## Архитектура

### DualCameraVideoRecorder

Основной класс для управления записью видео с двух камер:

```kotlin
// Инициализация
val recorder = DualCameraVideoRecorder(context, lifecycleOwner)

// Настройка
recorder.setupDualRecording(
    cameraProvider,
    backPreview,
    frontPreview,
    onSuccess = { /* готово */ },
    onError = { /* ошибка */ }
)

// Запись
recorder.startRecording(
    onRecordingStart = { },
    onRecordingStop = { backFile, frontFile -> },
    onError = { }
)

// Паузировка (API 31+)
recorder.pauseRecording()
recorder.resumeRecording()

// Остановка
recorder.stopRecording { backFile, frontFile -> }
```

### DragDropScalablePreviewView

Пользовательский View для предпросмотра с поддержкой:
- Drag-n-drop для окна PiP
- Масштабирование (0.5x - 3x)
- Ограничение движения в пределах экрана

## Возможные улучшения

- [ ] Синхронизация аудиотреков обоих видео
- [ ] Объединение видеозаписей в один файл
- [ ] Расширенные опции качества (480p, 720p, 1080p, 4K)
- [ ] Выбор битрейта аудио и видео
- [ ] Фильтры и эффекты в реальном времени
- [ ] Экспорт в различные форматы
- [ ] Встроенный видеоредактор
- [ ] Облачное хранилище

## Известные ограничения

1. **Отсутствие синхронизации** - Видео двух камер могут иметь небольшое смещение по времени
2. **Производительность** - На некоторых устройствах может снизиться качество записи при одновременной записи с двух камер
3. **Паузировка** - Доступна только на Android 12 (API 31+)
4. **Двойные камеры** - Требуется устройство с минимум двумя камерами

## Тестирование

### Рекомендуемые устройства

- **Redmi Note 8 Pro** (целевое устройство)
- Xiaomi Mi 10T Pro
- Samsung Galaxy S21
- OnePlus 9 Pro

### Тестовые сценарии

1. ✅ Проверка инициализации камер при запуске
2. ✅ Запись видео с обеих камер
3. ✅ Переключение режима PiP
4. ✅ Перемещение окна PiP
5. ✅ Масштабирование окна PiP
6. ✅ Паузировка и возобновление записи
7. ✅ Проверка сохранения файлов

## Отладка

### Логирование

Проверьте логи Android Studio:

```
adb logcat | grep DualCamera
```

### Общие проблемы

| Проблема | Решение |
|----------|----------|
| Камеры не инициализируются | Проверьте разрешения в настройках |
| Запись не сохраняется | Проверьте свободное место на диске |
| Приложение падает при PiP | Убедитесь, что устройство поддерживает 2 камеры |
| Видео дергается | Уменьшите качество или закройте другие приложения |

## Лицензия

MIT License

## Автор

bpodstawckin93-maker

## Благодарности

- Google CameraX team за отличную библиотеку
- Rode для вдохновения дизайном приложения

## Контакты

При обнаружении ошибок или предложениях создайте Issue в репозитории.
