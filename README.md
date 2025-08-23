<h1 align="center">📄 PDFly</h1>

<p align="center">
  <b>A lightweight and powerful Android library/app for working with PDF documents.</b><br>
  (View, annotate, and manage PDFs with ease)
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android" />
  <img src="https://img.shields.io/badge/Language-Java-blue?style=for-the-badge&logo=java" />
  <img src="https://img.shields.io/badge/Build-Gradle-yellow?style=for-the-badge&logo=gradle" />
  <img src="https://img.shields.io/github/license/Mukesh2080/PDFly?style=for-the-badge" />
</p>

---

## ✨ Features

| Feature | Status | Progress |
|---------|--------|----------|
| 📖 Open & View PDFs | ✅ Implemented | ![100%](https://progress-bar.dev/100/) |
| 🔍 Zoom & Navigate | ✅ Implemented | ![100%](https://progress-bar.dev/100/) |
| 🖊️ Annotate / Highlight | ⚡ In Progress | ![70%](https://progress-bar.dev/70/) |
| 📂 Create / Merge PDFs | 🚧 Planned | ![40%](https://progress-bar.dev/40/) |
| ☁️ Export & Share PDFs | 🚧 Planned | ![30%](https://progress-bar.dev/30/) |
| 🎨 Dark Mode Support | ✅ Implemented | ![100%](https://progress-bar.dev/100/) |

---

## ⚙️ Specifications

- **Language:** Java (Android)
- **Build System:** Gradle (KTS support)
- **Min SDK:** `21` (Android 5.0 Lollipop) *(example, update if different)*
- **Target SDK:** `34` *(example, update if different)*
- **Dependencies:** 
  - AndroidX
  - PDF rendering libraries *(update if specific ones used)*

---

## 📊 Architecture Overview

```mermaid
graph TD
    A[PDFly] --> B[PDF Viewer Core]
    A --> C[Annotation Engine]
    A --> D[File Manager]
    C --> E[Highlighting]
    C --> F[Notes / Comments]
    D --> G[Merge PDFs]
    D --> H[Export & Share]
