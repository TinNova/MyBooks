import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class ScanComposeTestsTask : DefaultTask() {

    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputFile
    abstract val targetFile: RegularFileProperty

    @get:Input
    abstract val targetPackage: Property<String>

    @TaskAction
    fun scanAndGenerate() {
        val srcDir = sourceDirectory.get().asFile
        val target = targetFile.get().asFile
        
        logger.lifecycle("🔍 Scanning for @ComposeTest annotated functions in: ${srcDir.absolutePath}")
        
        val composeTestFunctions = mutableListOf<ComposeTestFunction>()
        val allImports = mutableSetOf<String>()
        
        // Recursively scan all .kt files
        srcDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val functions = extractComposeTestFunctions(file)
                if (functions.isNotEmpty()) {
                    logger.lifecycle("   📄 Found ${functions.size} function(s) in ${file.name}")
                    functions.forEach { logger.lifecycle("      - ${it.name}") }
                    composeTestFunctions.addAll(functions)
                    
                    // Extract imports from this file
                    val fileImports = extractImports(file.readText())
                    allImports.addAll(fileImports)
                    
                    // Extract package-specific types used in these functions
                    val sourcePackage = extractPackageName(file.readText())
                    val usedTypes = extractUsedTypesFromFunctions(functions, file)
                    val samePackageTypes = extractSamePackageTypes(functions, file)
                    
                    // Add imports for types from source package
                    if (sourcePackage != targetPackage.get() && sourcePackage.isNotEmpty()) {
                        (usedTypes + samePackageTypes).forEach { typeName ->
                            allImports.add("import $sourcePackage.$typeName")
                        }
                    }
                }
            }
        
        if (composeTestFunctions.isEmpty()) {
            logger.warn("⚠️  No @ComposeTest annotated functions found")
            return
        }
        
        val generatedContent = generateScreenshotTestFile(
            packageName = targetPackage.get(),
            imports = allImports,
            functions = composeTestFunctions
        )
        
        target.parentFile.mkdirs()
        target.writeText(generatedContent)
        
        logger.lifecycle("✅ Generated screenshot tests with ${composeTestFunctions.size} function(s)")
    }

    private fun extractComposeTestFunctions(file: File): List<ComposeTestFunction> {
        val functions = mutableListOf<ComposeTestFunction>()
        val content = file.readText()
        
        // Pattern to find @ComposeTest annotated functions
        val pattern = Regex(
            """@ComposeTest\s*\n(?:@[^\n]+\n)*@Composable\s+fun\s+(\w+)\s*\([^)]*\)\s*\{""",
            RegexOption.MULTILINE
        )
        
        pattern.findAll(content).forEach { match ->
            val functionName = match.groupValues[1]
            val functionStart = match.range.first
            val functionBodyStart = content.indexOf('{', functionStart)
            val functionBodyEnd = findClosingBrace(content, functionBodyStart)
            
            if (functionBodyEnd != -1) {
                val completeFunction = content.substring(functionStart, functionBodyEnd + 1)
                functions.add(ComposeTestFunction(functionName, completeFunction, file.nameWithoutExtension))
            }
        }
        
        return functions
    }

    private fun findClosingBrace(content: String, openBraceIndex: Int): Int {
        var braceCount = 0
        var inString = false
        var inChar = false
        var escaping = false
        var inSingleLineComment = false
        var inMultiLineComment = false

        for (i in openBraceIndex until content.length) {
            val char = content[i]
            val prevChar = if (i > 0) content[i - 1] else ' '

            if (!inString && !inChar) {
                if (inSingleLineComment) {
                    if (char == '\n') inSingleLineComment = false
                    continue
                }
                if (inMultiLineComment) {
                    if (char == '/' && prevChar == '*') inMultiLineComment = false
                    continue
                }
                if (char == '/' && i + 1 < content.length) {
                    when (content[i + 1]) {
                        '/' -> {
                            inSingleLineComment = true
                            continue
                        }
                        '*' -> {
                            inMultiLineComment = true
                            continue
                        }
                    }
                }
            }

            if (!escaping) {
                when (char) {
                    '"' -> if (!inChar) inString = !inString
                    '\'' -> if (!inString) inChar = !inChar
                    '\\' -> escaping = true
                }
            } else {
                escaping = false
            }

            if (!inString && !inChar && !inSingleLineComment && !inMultiLineComment) {
                when (char) {
                    '{' -> braceCount++
                    '}' -> {
                        braceCount--
                        if (braceCount == 0) return i
                    }
                }
            }
        }

        return -1
    }

    private fun extractPackageName(content: String): String {
        val packagePattern = Regex("""package\s+([\w.]+)""")
        return packagePattern.find(content)?.groupValues?.get(1) ?: ""
    }

    private fun extractImports(content: String): Set<String> {
        val imports = mutableSetOf<String>()
        val importPattern = Regex("""import\s+[\w.]+""")
        
        importPattern.findAll(content).forEach { match ->
            val import = match.value
            val shouldExclude = import.contains("hilt") ||
                import.contains("livedata.observeAsState") ||
                import.contains("viewmodel.compose.hiltViewModel") ||
                import.contains("ComposeTest")
            
            if (!shouldExclude) {
                imports.add(import)
            }
        }
        
        return imports
    }

    private fun extractUsedTypesFromFunctions(functions: List<ComposeTestFunction>, sourceFile: File): Set<String> {
        val usedTypes = mutableSetOf<String>()
        val typePattern = Regex("""\b([A-Z][\w]*)\b""")
        val sourceContent = sourceFile.readText()
        val functionNames = functions.map { it.name }.toSet()
        
        functions.forEach { function ->
            typePattern.findAll(function.function).forEach { match ->
                val typeName = match.groupValues[1]
                
                if (typeName !in functionNames && 
                    typeName != "ComposeTest" &&
                    !isCommonComposeType(typeName) &&
                    isTypeDefinedInSource(typeName, sourceContent)) {
                    usedTypes.add(typeName)
                }
            }
        }
        
        return usedTypes
    }

    private fun extractSamePackageTypes(functions: List<ComposeTestFunction>, sourceFile: File): Set<String> {
        val types = mutableSetOf<String>()
        val typePattern = Regex("""\b([A-Z][\w]*)\b""")
        val functionNames = functions.map { it.name }.toSet()
        
        val sourceDir = sourceFile.parentFile
        val samePackageFiles = sourceDir.listFiles { file ->
            file.extension == "kt" && file.name != sourceFile.name
        }?.map { it.nameWithoutExtension }?.toSet() ?: emptySet()
        
        functions.forEach { function ->
            typePattern.findAll(function.function).forEach { match ->
                val typeName = match.groupValues[1]
                
                if (typeName !in functionNames && 
                    typeName != "ComposeTest" &&
                    !isCommonComposeType(typeName) &&
                    typeName in samePackageFiles) {
                    types.add(typeName)
                }
            }
        }
        
        return types
    }

    private fun isTypeDefinedInSource(typeName: String, sourceContent: String): Boolean {
        val patterns = listOf(
            Regex("""\b(class|interface|object|sealed class|data class|fun)\s+$typeName\b"""),
        )
        return patterns.any { it.find(sourceContent) != null }
    }

    private fun isCommonComposeType(typeName: String): Boolean {
        val commonTypes = setOf(
            "Modifier", "Column", "Row", "Box", "Text", "Button",
            "LazyColumn", "LazyRow", "Spacer", "Scaffold", "TopAppBar",
            "Icon", "IconButton", "Card", "Surface", "Divider",
            "CircularProgressIndicator", "LinearProgressIndicator",
            "TextField", "OutlinedTextField", "Image", "AsyncImage",
            "MaterialTheme", "Alignment", "Arrangement", "ContentScale",
            "FontWeight", "TextAlign", "TextOverflow", "PaddingValues",
            "LazyVerticalGrid", "GridCells", "GridItemSpan",
            "ModalBottomSheet", "ExperimentalMaterial3Api",
            "LaunchedEffect", "List", "String", "Boolean", "Int", "Float", "Double"
        )
        return typeName in commonTypes
    }

    private fun generateScreenshotTestFile(
        packageName: String,
        imports: Set<String>,
        functions: List<ComposeTestFunction>
    ): String {
        return buildString {
            appendLine("package $packageName")
            appendLine()
            
            val requiredImports = mutableSetOf(
                "import androidx.compose.runtime.Composable",
                "import androidx.compose.ui.tooling.preview.Preview",
                "import com.android.tools.screenshot.PreviewTest"
            )
            
            val allImports = (requiredImports + imports).toSortedSet()
            
            allImports.forEach { import ->
                appendLine(import)
            }
            
            appendLine()
            
            functions.forEach { function ->
                // Replace @ComposeTest with @PreviewTest
                val modifiedFunction = function.function
                    .replace("@ComposeTest", "@PreviewTest")
                    .let {
                        // If @Preview doesn't exist, add it after @PreviewTest
                        if (!it.contains("@Preview")) {
                            it.replace("@PreviewTest\n@Composable", "@PreviewTest\n@Preview\n@Composable")
                        } else {
                            it
                        }
                    }
                
                appendLine(modifiedFunction)
                appendLine()
            }
        }
    }

    data class ComposeTestFunction(
        val name: String,
        val function: String,
        val sourceFileName: String
    )
}
