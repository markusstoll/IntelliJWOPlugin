# IntelliJWOPlugin

Early-stage IntelliJ plugin for **mavenized WebObjects / WONDER** applications: run configurations, WO component workflow, and basic HTML template assistance inside `.wo` component folders.

> **Note:** This is an **early / experimental** release. APIs and behaviour may still change. Feedback and pull requests are welcome.

## Requirements

- **IntelliJ IDEA** (Community or Ultimate) **2025.2+** (`since-build` 252 in `plugin.xml`).
- **Java** and **Maven** support in the IDE (the plugin declares dependencies on the Java and Maven bundled plugins).
- A **mavenized** WebObjects / WONDER project layout (the run configuration and tooling assume Maven integration).

## Features (current)

- **WebObjects run configuration** — starts your WO app with sensible defaults (e.g. `-WOPort -1`), Maven `process-resources`, working directory under `./target/<appname>.woa`, and optional JDK 9+ VM flags (`--add-exports` / `--add-opens`) when needed.
- **New WO Component** action — creates WO component scaffolding from the bundled templates.
- **`.wo` folder editor** — opens the component folder with tabs (HTML + related files); embedded text editors with normal undo behaviour.
- **Navigation shortcuts** — switch between the WO component and its Java source (default shortcuts: macOS **⌥⌘1** / **⌥⌘2**, others **Ctrl+Alt+1** / **Ctrl+Alt+2**).
- **WO HTML templates (`*.wo/*.html`)** — `wo:*` tag awareness, tag name completion, and **binding / attribute** completion and validation:
  - Reflects **public fields** and **public JavaBean properties** (getter + setter) on the resolved tag class.
  - Ignores members that exist only on the **`WOComponent`** base class.
  - For Apple **system dynamic elements**, uses binding names from bundled `WebObjectDefinitions.xml` when defined for the short class name (e.g. `WOForm`).

## Installation

### From the JetBrains Marketplace (when published)

Search for **IntelliJWOPlugin** in the IDE plugin manager and install from the Marketplace listing.

### From a distribution JAR

1. Download the plugin ZIP/JAR from your **GitHub Releases** (or build output; see below).
2. In the IDE: **Settings → Plugins → ⚙ → Install Plugin from Disk…**
3. Restart the IDE when prompted.

### Build from source

```bash
./gradlew buildPlugin
```

The packaged plugin is written under `build/distributions/`. Install that archive via **Install Plugin from Disk…**.

## WebObjects run configuration

Create a new run configuration from the templates and choose **WebObjects**.

Compared to a plain Java application run configuration, this template:

- Detects **full Maven layout** vs a **“fluffy bunny”** style layout and adjusts `.project` nature handling accordingly.
- Sets the **working directory** to `./target/<appname>.woa`.
- Runs Maven **`process-resources`** before launch.
- On **JDK 9+**, adds default VM options (customizable in the run configuration):

  - `--add-exports=java.base/sun.security.action=ALL-UNNAMED`
  - `--add-exports=java.base/sun.util.calendar=ALL-UNNAMED`
  - `--add-opens=java.base/java.lang=ALL-UNNAMED`

## Roadmap / known gaps

Examples of planned or incomplete work (non-exhaustive):

- “Select opened file” behaviour for WO components opened via the custom editor.
- Stronger validators for `.wod` and richer HTML / binding diagnostics.
- Project wizard, EOGen / EOF tooling, rename/delete hooks for WO components.

## License

This project is licensed under the **MIT License** — see [`LICENSE`](LICENSE).

## Author / contact

**Markus Stoll** — [junidas.de](https://www.junidas.de) — markus.stoll@junidas.de
