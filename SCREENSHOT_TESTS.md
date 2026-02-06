# Screenshot Tests Automation

This project includes custom Gradle tasks to automatically generate screenshot tests from your Compose previews.

### 1. Scan Entire Project (Recommended)

Use the `@ComposeTest` annotation to mark which previews should be included in screenshot tests. The task will scan your entire project.

```bash
./gradlew :app:scanComposeTestsToScreenshotTests
```

## Usage

### Scanning with @ComposeTest (Recommended)

1. Add `@ComposeTest` annotation to your preview functions:

```kotlin
@ComposeTest
@Preview(showBackground = true, name = "Loading State")
@Composable
fun BookContentLoadingPreview() {
    BookContent(...)
}
```

2. Run the scan task:

```bash
./gradlew :app:scanComposeTestsToScreenshotTests
```

**Benefits:**
- ✅ Scans entire project automatically
- ✅ Fine-grained control - only annotated previews are included
- ✅ Works across multiple files and packages
- ✅ No configuration needed for new files

### Copying from Single File

### What They Do

**scanComposeTestsToScreenshotTests:**
1. **Scans** entire `src/main/java` directory recursively
2. **Finds** all functions annotated with `@ComposeTest`
3. **Extracts** complete function code from any file
4. **Replaces** `@ComposeTest` with `@PreviewTest`
5. **Generates** `ScreenshotTests.kt` with all marked previews
6. **Includes** necessary imports automatically
