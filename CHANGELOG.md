# Changelog

本文件说明面向使用方与发布说明的功能与行为变更。版本号以 `build.gradle.kts` 与发布渠道为准。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 常用小节（`Added` / `Changed` / `Fixed` 等；正文可为中文）。

## [Unreleased]

### Added

- **Sort + 表说明显示（仅本次生成）**：启用排序时，多行/TXT 支持 `表名|本次说明`，拖曳列表支持双击或「表说明…」；不写入库与项目。模板表级 `comment` 经 `TableDTO#getComment` 优先生效。键为表名，多 schema 同名不区分；见 [README](README.md) 与内置帮助（?）。

### Documentation

- README、`plugin.xml`、中/英 `help_*.html` 与 [CHANGELOG](CHANGELOG.md) 交叉说明；`TableDTO` / `TableOrderInputKit` 补充 Javadoc。
