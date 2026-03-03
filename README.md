# Cable Label Maker

A professional native Android application for generating printable A4 cable labels with a modern Material Design 3 interface.

## Features

### 🎨 Modern UI/UX
- **Material Design 3** implementation with dynamic color theming
- **Drawer Navigation** with collapsible sidebar for future settings expansion
- **Responsive Layout** with cards and proper spacing
- **Custom Toolbar** with hamburger menu icon
- **Dark Mode Support** with Material You color schemes

### ⚙️ Label Customization
- **Multi-line Text Input** for label content
- **Grid Configuration** with adjustable rows and columns (1-50 rows, 1-20 columns)
- **Font Size Control** (4-72 points)
- **Text Color Picker** with RGB sliders and preset colors
- **Gradient Editor** with start and end color selection
- **Live Preview** that updates in real-time as you type

### 📄 A4 Preview
- **Exact A4 Aspect Ratio** (595 x 842 points)
- **Scales to Fit Screen** while maintaining proper proportions
- **Dynamic Grid Drawing** based on row/column configuration
- **Centered Text** in each cell with accurate positioning
- **Material Card** elevation for polish

### 💾 Data Persistence
- **SharedPreferences** for saving and loading configurations
- **Save Labels** action in navigation drawer
- Remembers:
  - Label text
  - Grid dimensions
  - Font size
  - Text color
  - Gradient start/end colors

### 📱 PDF Export
- **Native PdfDocument** API (no third-party libraries)
- **Exact A4 Dimensions** (595 x 842 points)
- **System File Picker** via Intent.ACTION_CREATE_DOCUMENT
- User chooses exact save location
- **Pixel-perfect Export** matching preview
- Draws grid, text, and gradients directly to PDF canvas

## Technical Architecture

### Java + XML
- Pure Java implementation (no Kotlin)
- Traditional XML layouts (no Jetpack Compose)
- Minimum SDK: API 26 (Android 8.0 Oreo)
- Target SDK: API 36

### Key Components

#### MainActivity.java
- Manages UI lifecycle and user interactions
- Handles color picker dialogs
- Implements SharedPreferences persistence
- Manages PDF export workflow
- Sets up drawer navigation and toolbar

#### A4PreviewView.java
- Custom View extending Android View class
- Maintains A4 aspect ratio (595:842)
- Efficient rendering with proper Paint reuse
- Separate `drawToCanvas()` method for PDF export at actual size
- Handles both solid colors and gradients

#### ColorPickerDialog.java
- Custom Material Design 3 dialog
- RGB sliders with real-time preview
- 8 preset quick colors (Black, White, Red, Green, Blue, Yellow, Cyan, Magenta)
- Clean callback interface for color selection

### Layouts

#### activity_main.xml
- DrawerLayout root container
- CoordinatorLayout for proper scrolling behavior
- MaterialToolbar with hamburger icon
- Scrollable control panel with Material cards
- A4PreviewView in bottom half with card elevation
- NavigationView for sidebar

#### dialog_color_picker.xml
- Material card for color preview
- Three SeekBars for RGB values
- GridLayout with 8 preset color buttons
- Real-time value displays

#### nav_header_main.xml
- Material Design 3 styled header
- App icon and title
- Descriptive subtitle

### Resources

#### Material Design 3 Colors
- Complete light and dark theme color schemes
- Primary, Secondary, Tertiary color sets
- Surface, Background, and Error colors
- Proper contrast ratios for accessibility

#### Vector Drawables
- `ic_menu.xml` - Hamburger menu icon
- `ic_palette.xml` - Color picker icon
- `ic_gradient.xml` - Gradient editor icon
- `ic_pdf.xml` - PDF export icon
- `ic_save.xml` - Save configuration icon

## Usage

### 1. Enter Label Text
Type your cable label text (supports multi-line)

### 2. Configure Grid
- Set number of rows (1-50)
- Set number of columns (1-20)
- Adjust font size (4-72 points)

### 3. Customize Colors
- **Text Color**: Choose solid color for text
- **Gradient**: Select start and end colors for gradient effect
  - If start and end colors differ, gradient is applied
  - If same, solid text color is used

### 4. Preview
Watch the live A4 preview update as you make changes

### 5. Save Configuration
Open drawer menu → "Save Labels" to persist your settings

### 6. Export PDF
Click "Export to PDF" → Choose save location → PDF created at A4 size

## Building the App

```bash
# Clone repository
cd /home/junaid/Projects/LabelMaker

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on device
./gradlew installDebug
```

## APK Location
After building, find the APK at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Design Decisions

### Why Material Design 3?
- Modern, polished appearance
- Consistent with Android design guidelines
- Built-in dark mode support
- Accessible color contrast

### Why DrawerLayout?
- Provides clean navigation for future expansion
- Familiar Android pattern
- Easy to add more settings/features

### Why Custom View for Preview?
- Full control over rendering
- Maintains exact A4 aspect ratio
- Efficient drawing with Canvas API
- Reusable rendering logic for PDF export

### Why Native PdfDocument?
- No external dependencies
- Lightweight and fast
- Direct Canvas API access
- Same rendering as preview

## Future Enhancements
- [ ] Add barcode/QR code support
- [ ] Multiple label templates
- [ ] Image/logo insertion
- [ ] Font family selection
- [ ] Border styling options
- [ ] Batch export multiple pages
- [ ] Import/export configuration files
- [ ] Print directly from app

## License
MIT License - Feel free to use and modify

## Author
Built as a native Android demonstration following Material Design 3 guidelines
