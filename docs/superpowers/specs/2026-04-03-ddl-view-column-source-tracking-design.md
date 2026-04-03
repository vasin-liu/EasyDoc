# DDL 驱动的视图字段来源追踪设计

## 背景
当前 EasyDoc 在为 `VIEW`/`MATERIALIZED_VIEW` 的列回填注释与类型时，主要依赖“列名（忽略大小写）在物理表与派生对象之间的同名匹配”。当视图列存在 `AS` 别名或简单表达式（如 `t.col_b + 0 AS b1`）时，这种策略无法准确定位“视图列来源于哪个物理表/物理列”，从而导致注释/类型继承不准确。

用户希望：字段来源追踪基于 IntelliJ Database 元数据提供的 DDL；当视图列是多个字段合并出来的（复杂表达式、多个源列），则忽略追踪逻辑（即不做来源推断/继承）。

## 目标（Goals）
1. 针对 `VIEW` 和 `MATERIALIZED_VIEW` 的列来源，基于 DDL 推导映射关系：`视图列(alias) -> (源物理表, 源物理列)`。
2. 在回填流程中优先使用 DDL 映射；映射缺失时回退到现有的“列名同名继承”策略（保持兼容与兜底）。
3. 对“多源列合并/复杂表达式”的视图列不建立来源映射，满足“合并则忽略”的要求。
4. 实现尽量轻量，避免引入重 SQL parser；只覆盖常见且可解析的 DDL 形态。

## 非目标（Non-Goals）
1. 完整解析所有 SQL 方言与复杂语法（子查询、CTE、窗口函数、CASE、聚合、JOIN、UNION 等复杂结构的完整语义正确性）。
2. 推断复杂表达式内部的所有依赖列（只在“明确单源列 + 可识别别名”的情况下做映射）。
3. 解决物化视图未出现在文档中的问题（该问题由用户自行确认/处理）。

## 约束（Constraints）
1. DDL 获取来源：IntelliJ Database 元数据（`DbTable`/`DasTable` 等对象提供的 DDL 文本）。
2. 追踪映射的精度优先于覆盖率：解析失败或不确定时应返回 `empty` 并回退旧逻辑。
3. 性能：文档生成可能遍历较多对象，DDL 解析需做缓存或延迟执行。

## 总体方案
### 1) 数据结构扩展
在 `TableDTO` 增加一个字段，用于存放“视图列(alias) 到来源列”的映射（不影响物理表）：

- `Map<String, ColumnSourceDTO> columnSourceByAlias`
  - key：视图列名/alias（建议统一为 `lower-case + trim` 的规范化形式）
  - value：
    - `String sourceTableName`（必要时含 schema）
    - `String sourceColumnName`

`ColumnSourceDTO` 可作为内部 DTO 或独立 DTO：
- `sourceTableName`：源物理表名（必要时含 schema）
- `sourceColumnName`：源物理列名

物理表（`ObjectKind.TABLE`）默认映射为空。

### 2) DDL 解析构建映射
新增一个工具类（示例名：`ViewDdlColumnSourceKit`），负责：
1. 从 `DbTable` 读取 DDL 文本（只对 `ObjectKind.VIEW`/`ObjectKind.MAT_VIEW` 执行）。
2. 从 DDL 中提取 `SELECT` 列表中的顶层“投影项”。
3. 对每个投影项尝试识别“单源列 + 明确别名”：
   - **本版本只支持 `t.col AS alias` / `t.col alias`（带表前缀）**。
   - 若不带表前缀（如 `col AS alias`），由于需要额外解析 `FROM`/表别名消歧，在轻量方案下默认 **弃用（不建立映射）**。
4. 对下列情形：视图列来源不唯一或涉及多源列 => **忽略该列**（映射不建立）：
   - 投影表达式中出现明显的二元/多元运算符或函数（如 `+ - * / || CONCAT CASE COALESCE ...`）
   - 投影项同时包含多个疑似列引用（例如出现两个不同的 `t.col`）
   - 解析正则未匹配成功

解析策略采取“保守匹配 + 失败即回退”，尽量避免错误映射。

### 3) 回填流程改造（DerivedCommentInheritanceKit）
改造 `DerivedCommentInheritanceKit` 的回填逻辑如下：
1. 原有逻辑：构建物理表的 `colComment`/`colType`（按列名不区分大小写）。
2. 新增：构建更精确的“物理列索引”：  
   - `(schema?, tableName, columnName) -> comment/type`  
   - 这样当 DDL 给出 `(sourceTableName, sourceColumnName)` 时能直接命中并避免同名列冲突。
3. 当处理派生对象（VIEW/MAT_VIEW）时，优先对每个视图列：
   - 若 `TableDTO.columnSourceByAlias` 存在该 alias：
     - 用来源 `(sourceTableName, sourceColumnName)` 定位注释/类型（如果来源注释/类型不可得，则不强行填充）
   - 若来源映射不存在或来源不可用 => 回退到旧逻辑：按视图列名与物理列名同名继承。

> 注：为了“合并则忽略”，DDL 解析器对复杂表达式列不会建立映射，因此上述优先逻辑不会命中，从而自然满足要求。

## 解析规则（建议的正则级最小实现）
在不引入重 SQL Parser 的前提下，建议实现一个“小步覆盖、保守失败”的规则集：

1. 提取 `SELECT ... FROM ...` 的投影区间（仅处理最外层 SELECT；当无法区分嵌套时选择弃用）。
2. 将投影区间按逗号切分为候选投影项（需避免切分到括号内部；可用简单括号计数）。
3. 对每个投影项执行匹配：
   - 形态 A：`(?i)^\\s*([a-z0-9_]+)\\.([a-z0-9_]+)\\s+(?:AS\\s+)?([a-z0-9_]+)\\s*$`
4. 在匹配成功后，再验证该投影项是否包含任意“高风险表达式特征”（例如 `\\+|\\-|'\\||\\b(case|coalesce|concat|cast|sum|min|max|avg|count)\\b` 等），一旦命中则忽略。
5. 对匹配到的 alias 做规范化：`lower-case + trim`。

## 与现有代码的集成点
1. `TableDTO`：在构造或延迟解析时填充 `columnSourceByAlias`。
2. `DerivedCommentInheritanceKit`：回填列注释/类型时优先使用 DDL 映射，其次回退到列名继承逻辑。
3. 若 IntelliJ API 在当前 JDBC/方言下无法稳定返回 DDL，需保证：
   - 解析失败 => 不中断生成 => 映射为空 => 回退旧逻辑。

### DDL 读取时序与 PSI 释放约束
`TableDTO.getColumns()` 在完成列提取后会把 `das` 置空（减少内存压力）。因此本方案要求：
- DDL 文本读取与映射解析必须在 `das` 被置空前完成并缓存到 `TableDTO` 中；
- 或者确保不在解析前释放 `das`（两者择一，以其中一种保证解析能稳定拿到 DDL）。

## 与枚举/类型命名的对齐
代码中：
- `DocOptions` 使用 `ObjectKind.MAT_VIEW`
- `NamespaceDTO` 通过字符串 `"MATERIALIZED_VIEW"` 反射解析 `ObjectKind`

规格建议在实现时统一枚举名来源（避免 MATERIALIZED_VIEW / MAT_VIEW 不一致导致物化视图对象未进入 `TableDTO` 列表），否则视图来源追踪对物化视图将无法覆盖到真实对象。

## 测试计划
建议补充单元测试（如果项目已有测试框架）：
1. 单源列 + AS：`SELECT t.a AS x` => 映射 `x -> (t,a)`。
2. 单源列 + 不带 AS：`SELECT t.a x` => 映射 `x -> (t,a)`。
3. 多源合并：`SELECT t.a || '_' || t.b AS x` => 映射不存在。
4. 简单表达式：`SELECT t.b + 0 AS x` => 若表达式检测为高风险则忽略映射。
5. 解析失败：DDL 空/无法读取 => 映射为空并触发回退策略。
6. 不带表前缀：`SELECT a AS x` => 映射不存在（轻量方案默认弃用）。

## 风险与缓解
1. 风险：DDL 文本可能包含方言差异导致正则匹配失败。  
   缓解：失败即回退，保证不会破坏原有继承功能。
2. 风险：投影项切分（逗号/括号）不严谨导致误解析。  
   缓解：保守切分（括号计数），必要时弃用整条 DDL。
3. 风险：IntelliJ API 在部分对象上 DDL 取不到。  
   缓解：对缺失 DDL 返回 `empty`，不影响流程。

