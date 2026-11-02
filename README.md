# IntelliJWOPlugin

Plugin for **mavenized WebObjects / Project WONDER** applications in IntelliJ IDEA: project wizard, run configurations, WO component workflow, template editing, and deep code insight for HTML component bindings across Java and Kotlin.

> **Note:** This is an active community project. Feedback, feature requests, and pull requests are welcome!

---

## Requirements

- **IntelliJ IDEA** (Community or Ultimate) **2026.2+** (`since-build="262"`).
- **Java** and **Maven** plugins enabled (bundled with IntelliJ IDEA).
- A **mavenized** WebObjects / Project WONDER project layout.

---

## Features

### 1. WebObjects Project Wizard
Create new WebObjects and Project WONDER projects directly from IntelliJ's **New Project** dialog or the **File → New → WebObjects Project…** action:
- Scaffolds projects using official **WOCommunity Maven archetypes** (e.g. `woapplication-archetype`, `wonder-archetype`).
- Configures Maven structure, packaging, and project properties automatically.

### 2. WO Component Workflow & `.wo` Folder Editor
- **New WO Component** action: Creates `.wo` component scaffolding (HTML, WOD, WOO) with sensible defaults.
- **`.wo` Folder Editor**: Opens the component folder in a dedicated multi-tabbed editor:
  - **Component Tab**: Split-view editor (HTML template + WOD bindings) with 80:20 layout and undo/redo support.
  - **API Tab**: Eclipse-style visual Component API editor synchronized with the component's `.api` file in real-time, plus a raw XML tab.
  - **Plain Text / WOO Tab**: Inspect and edit component display groups and metadata.
- **Project View Integration**: First-class representation in IntelliJ's project view (`<ComponentName> WO`). Fully integrated with **"Select Opened File"** (crosshair toolbar button), **"Always Select Opened File"** (autoscroll from source), and **"Select In…" (⌥F1)**.

### 3. Component API Editor (`.api` Files)
Define and manage component binding specifications in an intuitive visual editor:
- **Visual & XML Synchronization**: Add, rename, or remove bindings and edit binding definitions (Required, Defaults, Type, Validation rules) visually or via raw XML with automatic two-way synchronization.
- **Automatic Scaffolding**: Automatically creates and initializes `<ComponentName>.api` when switching to the API tab if not already present.

### 4. Multi-Language Component & Source Switching
Seamlessly jump back and forth between a WO component template and its backing controller class:
- **Supported Languages**: Works with **Java** (`.java`) and **Kotlin** (`.kt`) implementations.
- **Template → Source**: Press **⌥⌘1** (macOS) or **Ctrl+Alt+1** (Windows/Linux) from inside a `.wo` folder, `.html`, `.wod`, or `.woo` file to open its backing class. Works immediately from the active component tab without requiring a prior click into the editor.
- **Source → Template**: Press **⌥⌘2** (macOS) or **Ctrl+Alt+2** (Windows/Linux) from any Java or Kotlin component class to open its `.wo` component folder.
- **Interactive Component vs. Code Chooser**: When **⌘-Click**ing (or **Ctrl-Click**ing) on any component type in `.wod` or inline `<wo:...>` templates, a chooser popup lets you choose whether to open the Java/Kotlin controller source file or directly open and focus the `.wo` Component Editor tab.
- **Strict Multi-Module Scoping & Ambiguity Resolution**: In multi-module Maven projects where multiple components share the same name (e.g. `Main.wo` across different modules), navigation is strictly bounded to the same IntelliJ module. It verifies `WOComponent`/`ERXComponent` inheritance before ambiguity checks to ensure common names (like `Main`) resolve cleanly without false-positive errors.
- **Build Artifact Exclusion**: Navigation and class resolution completely ignore build artifacts (`target/`, `build/`, `out/`, `/.gradle/`, and compiled `.class` files), ensuring you always land on editable source files.

### 5. HTML Template Assistance (`wo:*` Tags) & API-Driven Bindings
- **Tag Name Completion**: Code completion for dynamic WebObjects elements using the `wo:` namespace (e.g. `<wo:WOString>`, `<wo:WOConditional>`, `<wo:WOSubmitButton>`) as well as custom WO components.
- **Built-in Namespace Provider**: Automatically binds the `wo:` namespace to eliminate annoying "Namespace 'wo' is not bound" inspection warnings in HTML templates.
- **Tag Attribute Completion**: Suggests valid bindings for system elements (backed by `WebObjectDefinitions.xml`) and custom components (backed by their `.api` definitions).
- **Required Bindings Prioritization**: Required bindings are shown in bold with `(required)` label and sorted to the top of completion suggestions.
- **Missing Required Binding Inspection**: In-editor warning highlighting `<wo:...>` tags missing required bindings with an **"Add missing binding"** quick-fix.
- **Tag Attribute Validation**: Validates dynamic element bindings against the component's public properties and methods.

### 6. Inline Binding Autocompletion (`$`-Bindings)
Directly bind HTML tag attributes to component properties and methods with context-aware autocompletion:
- **Trigger**: Type `$` inside any HTML/XML attribute value (e.g. `value="$"`, `action="$"`).
- **Member Suggestions**:
  - **Properties & Getters**: `getName()` and `isEditable()` are suggested as `name` and `editable`.
  - **Actions & Methods**: Zero-argument methods like `saveAction()` or `doSomething()` are suggested for action bindings.
  - **Fields**: Public and backing fields (`_foo`) are offered.
- **Key-Path Completion**: Fully supports nested paths (e.g. typing `$user.` suggests properties from the `User` class).

### 7. Go to Declaration & Refactoring for `$bindings`
- **Command-Click / Go to Declaration (⌘-Click / ⌘B)**:
  - Command-clicking a `$binding` in an HTML template jumps directly to its declaration in the Java or Kotlin source file.
  - Supports clicking both the `$` prefix and the property name.
  - Supports method calls with parentheses (e.g. `$saveAction()`).
  - Key-paths are resolved segment by segment (in `$user.address.street`, clicking `address` jumps directly to `getAddress()` on the `User` class).
- **Refactoring & Find Usages**:
  - **Find Usages (⌥F7)** on a Java/Kotlin method or field finds references in WO HTML templates.
  - **Rename (⇧F6)** on a property or getter automatically updates `$bindings` in templates.
- **WONDER & Hierarchy Awareness**:
  - Seamlessly resolves inheritance hierarchies for `WOComponent`, `ERXComponent`, `ERXStatelessComponent`, `ERXDirectAction`, and custom base classes.

### 8. WebObjects `.wod` File Support & `<webobject>` Navigation
Full language, navigation, and editing support for classic WebObjects `.wod` files and `<webobject name="...">` dynamic element declarations:
- **Dedicated `.wod` Language & Syntax Highlighting**: Clean syntax highlighting for element declarations, component types, binding keys, code values, and string literals, plus comment toggling (`⌘/` / `Ctrl+/`) and smart indentation on `Enter`.
- **`<webobject>` Tag Autocompletion**:
  - Typing `<web` suggests `webobject` and automatically generates ` name=""` with the caret placed inside the quotes.
  - Automatically pops up existing `.wod` element names upon attribute insertion.
  - Full self-closing support (`<webobject name="..." />`) with browser empty-tag warnings cleanly suppressed for server-side templates.
- **Bidirectional Gutter & Click Navigation**:
  - Gutter icon and **⌘-Click** on `<webobject name="xxx">` in the HTML editor instantly scroll and center the corresponding `xxx : Type { ... }` declaration in the WOD editor within the same split view, selecting the element name. The split ratio is preserved, only auto-expanding if the target editor was virtually collapsed (< 10%).
  - **Instant Element Scaffolding**: If a `<webobject name="xxx">` has no definition in `.wod`, clicking the gutter icon or **⌘-Click** immediately creates `xxx : WOComponent {\n}\n` in the `.wod` file, scrolls to it, and selects `WOComponent` for immediate typing.
  - Gutter icon next to `xxx : Type` in the WOD editor jumps directly back to the matching `<webobject name="xxx">` tag in the HTML template.
- **Go to Declaration (⌘-Click / ⌘B)**:
  - `name="xxx"` in HTML resolves to the WOD declaration.
  - Component types (e.g. `WOString`, `WOConditional`, `WORepetition`, `Main`, `ERXLocalizedString`, etc.) trigger an interactive chooser popup allowing you to open either the controller class or the `.wo` Component Editor tab.
  - Code bindings (without `$`, e.g. `condition = hasExtraLargeLoginFields;` or nested keypaths `user.address`) jump directly to the controller's property or method.
- **Parent Bindings (`^bindingName`)**:
  - Full syntax, parsing, and autocompletion for parent bindings (`^binding`) in `.wod` files with validation against the component's `.api` specification.
- **Context-Aware Autocompletion**:
  - Typing `<webobject name="<caret>"` in HTML suggests all element definitions defined in `.wod`.
  - In `.wod`, autocompletion is available for component types (after `:`), binding keys from system definitions and `.api` files (inside `{}`), parent bindings (`^`), and controller properties/methods (after `=`).
- **Missing Required Binding Inspection in `.wod`**:
  - Highlights element declarations missing required bindings (as defined in `WebObjectDefinitions.xml` or the component's `.api` file) with quick-fixes to insert the missing bindings.

### 9. Two-Way Template Conversion (Convert to WOD / Convert to Inline)
Effortlessly switch between classic WebObjects `.wod` declarations and modern Wonder-style inline templates with a single click:
- **Convert to WOD**:
  - Converts inline `<wo:*>` tags into classic `<webobject name="...">` tags and appends the declarations to the `.wod` file.
  - **Shorthand & Alias Resolution**: Automatically maps short forms like `<wo:if>` $\rightarrow$ `WOConditional`, `<wo:str>` $\rightarrow$ `WOString`, `<wo:repeat>`/`<wo:foreach>` $\rightarrow$ `WORepetition`, `<wo:submit>` $\rightarrow$ `WOSubmitButton`, `<wo:form>` $\rightarrow$ `WOForm`, `<wo:link>` $\rightarrow$ `WOHyperlink`, `<wo:localized>` $\rightarrow$ `ERXLocalizedString`, etc.
  - **Type-Based Numbered Naming**: Generated element names follow the element type and increment sequentially (e.g. `WOConditional1`, `WOConditional2`, `WOString1`), avoiding collisions with existing declarations in `.wod`.
  - **Smart Binding Conversion**: Translates dynamic `$`-bindings (e.g. `condition="$isUserLoggedIn"`) into code bindings (`condition = isUserLoggedIn;`) and static values into quoted string literals with proper escaping.
  - Automatically scaffolds the `.wod` file if not already present.
- **Convert to Inline**:
  - Replaces `<webobject name="xxx">` tags in the HTML template with modern inline `<wo:TypeName>` tags using `$`-bindings.
  - Dynamic expressions become `key="$val"`, string literals become `key="val"`.
  - Cleanly prunes the converted element declarations from the `.wod` file while preserving unrelated declarations and comments.
- **Context Menu Integration**: Both actions are accessible directly from the **Project View** (right-clicking any `.wo` folder or file) and from the **Editor Context Menu**.

### 10. WebObjects Run Configuration
Create and run WebObjects applications with production-ready settings out of the box:
- Automatically sets the working directory to `./target/<appname>.woa`.
- Executes Maven **`process-resources`** before launch to keep templates and resources up to date.
- Applies standard WebObjects flags (e.g. `-WOPort -1`).
- Configures essential **JDK 9+ modular JVM flags** (customizable in the editor):
  - `--add-exports=java.base/sun.security.action=ALL-UNNAMED`
  - `--add-exports=java.base/sun.util.calendar=ALL-UNNAMED`
  - `--add-opens=java.base/java.lang=ALL-UNNAMED`

---

## Default Keyboard Shortcuts

| Action | macOS | Windows / Linux |
| :--- | :--- | :--- |
| **Open Source File** (from `.wo` / `.html` / `.wod` / `.woo`) | **⌥⌘1** | **Ctrl+Alt+1** |
| **Open WOComponent** (from Java / Kotlin class) | **⌥⌘2** | **Ctrl+Alt+2** |
| **Go to Declaration** (on `$binding` in HTML or code binding in `.wod`) | **⌘-Click** / **⌘B** | **Ctrl-Click** / **Ctrl+B** |
| **Navigate HTML ↔ WOD** | Gutter Icons / **⌘-Click** on `name` | Gutter Icons / **Ctrl-Click** on `name` |
| **Code Completion** (HTML attribute or `.wod` binding) | **$** / **⌃Space** | **$** / **Ctrl+Space** |
| **Convert to WOD / Convert to Inline** | Context Menu (**Project View** / **Editor**) | Context Menu (**Project View** / **Editor**) |

---

## Installation

### From the JetBrains Marketplace
Search for **WebObjects / WONDER** in the IDE Plugins Manager (**Settings → Plugins**) and install.

### From a Distribution Archive
1. Download `IntelliJWOPlugin-<version>.zip` from GitHub Releases.
2. In IntelliJ IDEA: **Settings → Plugins → ⚙ → Install Plugin from Disk…**
3. Select the `.zip` archive and restart the IDE when prompted.

### Building from Source
```bash
./gradlew buildPlugin
```
The packaged plugin archive will be generated at `build/distributions/IntelliJWOPlugin-<version>.zip`.

---

## Recent Highlights (v0.9.0)

- **Interactive Component vs. Code Navigation (Target Chooser Popup)**: ⌘-Clicking a component type in `.wod` or inline HTML presents a selector popup to open either the Java/Kotlin controller source file or directly open and focus the `.wo` Component Editor tab.
- **Direct Focus & Immediate Shortcut Switching**: Switching back to code via **⌥⌘1** / **Ctrl+Alt+1** works instantly when the WOComponent tab is active without requiring a prior mouse click into the editor.
- **Unified Grammar-Kit & JFlex PSI Architecture**: Full IntelliJ PSI AST for `.wod` files (`WODDeclaration`, `WODComponent`, `WODBinding`, `WODKeyPath`, `WODValue`, etc.) enabling deep refactoring, find-usages, and type safety.
- **WOD Code Formatter**: Dedicated code formatting model (`Reformat Code` / `⌥⌘L` / `Ctrl+Alt+L`) with custom spacing rules for WOD declarations, assignments, braces, and semicolons.
- **Java ↔ WOD Gutter Markers**: WebObjects gutter icons in Java controller classes on fields and methods navigating directly to referencing WOD key paths.
- **KVC Key Path Resolution & Validation**: Validates WOD key paths against backing Java component members with smart completion, type inference, and `// valid` comment suppression.
- **Parent Bindings Support**: Full syntax, completion, and validation for parent bindings (`^bindingName`) in `.wod` files against component `.api` specifications.
- **Non-Disruptive Split View Sizing**: Gutter navigation between HTML and WOD preserves user-adjusted split ratios; only auto-expands when the target pane is nearly collapsed (< 10%).
- **Unbound Namespace Resolution**: Built-in XML namespace provider completely eliminates "Namespace 'wo' is not bound" warnings in HTML templates.
- **Two-Way Template Conversion**: One-click conversion between modern inline `<wo:*>` tags and classic `.wod` declarations with automatic tag alias resolution (`wo:if` $\rightarrow$ `WOConditional`, `wo:str` $\rightarrow$ `WOString`, etc.) and element numbering.
- **Full `.wod` Language Support**: Dedicated syntax highlighter, brace matching, quote handler, comment toggling, and smart `Enter` indentation.
- **Bidirectional HTML ↔ WOD Navigation**: Gutter icons and **⌘-Click** jump and scroll directly between HTML tags and WOD declarations; instant element scaffolding on click for undefined tags.
- **`<webobject>` & WOD Completion**: Intelligent autocompletion for `<webobject name="...">`, WOD component types, binding keys (from definitions and `.api`), and controller methods.
- **Diagnostics & Quick-Fixes**: Missing required binding inspections for both inline `<wo:...>` tags and `.wod` declarations with quick-fixes to insert missing bindings.
- **Multi-Module Isolated Switching**: Bounded navigation between components and Java/Kotlin classes (`⌥⌘1` / `⌥⌘2`) strictly within the same IntelliJ module, with intelligent class disambiguation (e.g. for common names like `Main`) and complete exclusion of build artifacts (`target/`, `build/`, `out/`).

---

## Roadmap

Planned improvements for upcoming releases:
- Direct-to-Web (D2W) rule editor and assistant.
- EOGen / EOF data modeling tools and entity inspection.
- Component refactoring actions (rename/delete component bundles atomically).

---

## License

This project is licensed under the **MIT License** — see [`LICENSE`](LICENSE).

---

## Author & Contact

**Markus Stoll** — [junidas.de](https://www.junidas.de) — markus.stoll@junidas.de
