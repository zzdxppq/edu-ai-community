# 校共体AI助手 产品需求文档 (PRD)

## 1. Goals and Background Context

### 1.1 Goals

- 构建一个嵌入校共体网站的智能问答助手，为教育管理者、校长、教师及研究者提供精准的政策解读、结对指导、案例推荐、流程导航与决策支持
- 通过自然语言交互，将静态政策文件与动态实践经验转化为可交互、可执行、可追溯的个性化指导服务
- 有效降低制度性成本，提升校共体建设的规范化、科学化与智能化水平
- 建立人工与AI协同的学习闭环，实现知识库与服务能力的持续迭代

### 1.2 Background Context

根据《河南省教育厅 河南省财政厅 河南省人力资源和社会保障厅关于深入推进城乡学校共同体建设工作的通知》及《河南省县域结对型城乡学校共同体建设指南》，河南省正在深入推进县域城乡义务教育学校共同体建设。当前校共体建设面临政策文件分散、解读门槛高、实践经验难以复用、流程指导缺乏个性化等痛点。现有方式主要依赖线下培训和文件下发，信息传递效率低、覆盖面有限，亟需一个智能化的在线服务助手来弥补这一缺口。

本智能体定位为集"政策解读器、规划引导员、案例推荐官、流程导航仪"于一体的业务赋能平台，嵌入校共体网站，面向6类角色提供差异化服务，一次性全功能交付。

### 1.3 Change Log

| 日期 | 版本 | 描述 | 作者 |
|------|------|------|------|
| 2026-03-13 | v1.0 | 初始PRD | 孔明（业务分析师） |
| 2026-04-01 | v1.1 | 技术栈调整（后端→JDK 21 + Spring Cloud，前端确认Vue 3，Redis 7）；运营后台增强（反馈处理、知识条目可见角色、文件预览、推荐问题管理）；智能体页面UI重构 | 鲁班（架构师） |
| 2026-04-02 | v1.2 | 新增平台数据集成：校共体网站实时数据API对接、搜索结果向量化入知识库、6大内容模块（示范校/新闻资讯/政策文件/资源共享/活动/月报） | 范蠡（产品经理） |
| 2026-04-03 | v1.3 | PRD 细节完善：常见问题改为5个单行自适应；评分星标缩小至12px；反馈流程简化（去除专家概念，运营直接回复自动完成）；反馈加入知识库须先回复；新增安全合规需求（免责声明FR38、敏感词过滤FR39、用户输入隐私警告FR40、语义缓存FR41、对话日志脱敏FR42）；新增NFR（幻觉检测NFR11、内容合规NFR12、多轮上下文限制NFR13、缓存命中NFR14）；修复FR3/FR29矛盾 | 范蠡（产品经理） |

---

## 2. Requirements

### 2.1 Functional Requirements

**用户系统**

- **FR1:** 系统支持手机号注册/登录，采集注册所需信息（手机号、用户名、所属角色、所属区域/学校）
- **FR2:** 系统支持保持登录状态（Token机制），用户关闭浏览器后再次打开无需重新登录（合理有效期内）
- **FR3:** 系统记录并存储用户使用日志，包括账号、用户名、登录时间、完整对话记录（用户询问和智能体回复）。对话日志存储前须对用户消息中的敏感个人信息（身份证号、手机号、银行卡号等）进行自动脱敏处理（如 `4101**********1234`），脱敏后的内容用于持久化存储和运营后台展示
- **FR4:** 用户注册时必须选择角色身份：区域教育管理人员、校共体核心牵头校人员、校共体成员校人员（城镇）、校共体成员校人员（乡村）、研究人员、其他人员

**自然语言理解与对话**

- **FR5:** 系统支持口语化、多句式、带错别字容错的提问方式（如："校共体怎么结对？""1+1+N是什么模式？"）
- **FR6:** 系统在对话区域中间默认展示5个常见问题（由运营配置的推荐问题），每个问题单行不换行、宽度自适应文字长度；下方提供"🔄 换一批"操作按钮，用户可刷新获取不同的推荐问题
- **FR7:** 系统支持多轮对话上下文管理，保持对话连续性，支持用户连续追问（如："上面说的评估指标具体有哪些？""能给我举个例子吗？"）
- **FR8:** 系统具备话题边界感知能力，在长时间对话或话题跳跃时进行温和确认（如："您现在想了解的是XX方面的内容吗？"）

**精准政策解读（RAG）**

- **FR9:** 系统基于知识库中的政策文件内容，通过混合检索（向量检索+关键词检索+重排序）提供精准语义检索与答案生成
- **FR10:** 任何政策类回答必须强制引用出处，在回复中明确标注文件名称、具体章节条款
- **FR11:** 用户可点击引用出处，跳转查看原文片段（PDF/文档预览+定位到具体页码/段落）
- **FR12:** 系统支持解析PDF和Word格式的政策文件，包括文件中的文本、表格和流程图内容

**场景化角色服务**

- **FR13:** 左侧侧边栏顶部放置"新对话"按钮，方便用户快速开启新对话；侧边栏下方展示政策动态、相关案例等角色相关资源推送内容，各区块右侧提供"🔄 换一批"圆角胶囊按钮刷新内容（与对话中心换一批按钮样式统一）
- **FR14:** 不同角色看到不同的推荐问题和资源推送内容，系统根据角色调整回复的侧重点和专业深度

**智能推荐与案例学习**

- **FR15:** 当用户咨询特定问题时，系统在给出通用建议后，主动推荐知识库中同类区域或学校的成功实践案例
- **FR16:** 推荐的案例必须经过自动脱敏处理后展示（人名→某教师、校名→某小学、精确数字→模糊量级、地名→某县/某镇等）

**数据脱敏处理**

- **FR17:** 系统实现自动脱敏管道，对客户提供的原始案例文献进行自动脱敏处理，脱敏类型包括：
  - 人名：NER识别 → 替换为角色泛化词（如"某教师""某校长"）
  - 学校名称：NER识别 → 替换为"某小学""某中学"
  - 地名：NER识别 → 替换为"某县""某镇"（省级地名可保留）
  - 精确数字：数值模糊化（如103→近百）
  - 联系方式：正则匹配 → 删除或掩码
  - 具体日期：模糊化为季度/年份
- **FR18:** 脱敏结果必须经运营人员在后台审核确认后方可入库，系统保留原始→脱敏的映射表（仅运营可见）

**资源推送**

- **FR19:** 系统根据用户角色和历史对话内容，主动推送平台内相关的政策动态、通知公告、优质课程资源链接

**平台数据集成**

- **FR33:** 系统对接校共体网站实时数据搜索API（`{configurable_domain}/api/content/v1/search`），域名须支持通过配置文件动态调整，不硬编码
- **FR34:** 平台数据覆盖6大内容模块：示范校（model_school）、新闻资讯（news）、政策文件（policy）、资源共享（resource）、活动（activity）、月报（monthly_report），系统须按模块分类存储和检索
- **FR35:** 系统在每次搜索平台数据后，自动将返回结果进行向量化处理并写入知识库（pgvector），以持续丰富RAG检索语料、提升问答质量
- **FR36:** 平台数据向量化入库时须记录来源模块（module）、原始ID、标题、发布时间、原始URL，支持溯源跳转至校共体网站原文页面
- **FR37:** 系统支持定时增量同步平台数据（基于publishTime增量拉取），避免重复入库，同时支持运营手动触发全量同步

**安全合规与内容治理**

- **FR38:** 每条AI回答末尾须自动追加免责声明（如："以上内容由AI基于公开政策文件生成，仅供参考，不构成官方解读。如需确认，请咨询当地教育行政部门。"）。声明文案由运营在后台配置管理，支持修改
- **FR39:** 系统须具备双向敏感词过滤能力：
  - **输入侧**：对用户输入做敏感词扫描（政治敏感、不当言论、Prompt注入攻击等），命中时拒绝处理并给出安全提示
  - **输出侧**：对模型生成内容做敏感词扫描，命中时替换为安全回答（"该问题涉及敏感内容，建议咨询相关部门"）
  - 敏感词库由运营在后台维护，支持增删改
- **FR40:** 系统须在用户输入消息时进行实时隐私信息检测（身份证号、手机号、银行卡号等正则模式匹配），检测到时前端弹出提示："您的输入中似乎包含敏感个人信息，建议删除后再发送。这些信息对AI回答帮助不大，且可能存在隐私风险。"用户可选择继续发送或修改
- **FR41:** 系统须实现语义缓存机制，对高频热门政策问答进行缓存以降低模型API调用成本和响应延迟：
  - 对用户问题做Embedding，与缓存池中已有问题做相似度匹配（阈值≥0.95），命中则直接返回缓存答案
  - 缓存按角色分组（不同角色同一问题的回答侧重点不同）
  - 知识库更新（新文件上传/条目修改）时自动清除关联缓存
  - 多轮追问、低置信度回答不使用缓存
- **FR42:** 案例脱敏处理采用"检索与展示分离"策略：检索索引基于原始文本（保证召回率），展示时通过原始→脱敏映射表实时替换为脱敏文本。替换必须在服务端完成，原始文本不得出现在API响应中

**知识库管理与自进化**

- **FR20:** 运营人员可将处理后的优质问答经审核后一键同步至智能体知识库，作为后续回答的参考
- **FR21:** 用户提出的问题自动同步至知识库待审核队列（经去重/筛选）
- **FR22:** 运营后台支持知识库的完整CRUD管理（新增、编辑、删除、查看知识条目）
- **FR22a:** 知识条目上传时须选择可见角色（多选），不同角色用户仅能检索到其角色可见的知识条目
- **FR22b:** 知识条目列表页须显示每条知识的可见角色标签
- **FR22c:** 已上传文件列表须提供在线预览功能，支持PDF和Word文件的在线查看
- **FR23:** 运营后台支持上传新的政策文件（PDF/Word），系统自动完成文档解析、切分、向量化并入库

**用户反馈**

- **FR24:** 每条智能体回答下方设置满意度反馈评分（1-5分），星标图标采用小尺寸（12px）以不干扰阅读体验
- **FR25:** 用户评分后可通过信息收集框说明不满意的具体原因
- **FR26:** 用户对智能体回答不满意时，可通过"提交反馈"按钮将问题提交至运营后台，由运营人员直接处理并回复（运营充当专家角色，系统无独立专家角色，无需"转交专家"流程）
- **FR26a:** 用户端提供"反馈记录"入口，用户可查看自己提交的反馈列表及平台给出的处理结果

**运营后台**

- **FR27:** 运营后台提供知识库管理功能（文件上传、知识条目CRUD、脱敏审核、问答审核入库）
- **FR28:** 运营后台提供用户反馈查看与处理功能，反馈状态仅包含三种：待处理、处理中、已完成（无专家转交概念）
- **FR28a:** 运营人员可在反馈详情页直接输入回复内容，提交回复后自动标记为"已完成"状态
- **FR28b:** 运营填写的处理结果将展示给提交反馈的用户查看
- **FR28c:** 反馈的问答内容可通过"加入知识库"功能同步至知识库（默认审核通过直接可用），但须先完成回复后才可操作
- **FR29:** 运营后台提供对话日志查看与分析功能，展示的对话内容为脱敏后版本（与FR3存储一致），运营人员不可查看用户原始敏感信息
- **FR30:** 运营后台提供常用问题快捷菜单的配置管理功能
- **FR31:** 运营后台提供各角色推荐内容的配置管理功能
- **FR32:** 运营后台新增"推荐问题管理"功能模块，推荐问题显示在对话框下方区域，由运营独立维护
- **FR32a:** 推荐问题须按角色区分，不同角色用户看到不同的推荐问题列表
- **FR32b:** 推荐问题内容为一问一答格式（运营维护问题和对应的标准答案），用户点击问题后直接展示预设答案
- **FR32c:** 推荐问题与FR6的"常用问题快捷菜单"为不同功能：快捷菜单触发AI实时回答，推荐问题展示运营预设的标准答案

### 2.2 Non-Functional Requirements

- **NFR1:** 95%的查询响应时间在3秒以内（含模型推理+检索）
- **NFR2:** 系统支持峰值并发用户数 ≥ 500
- **NFR3:** 所有数据（含上传文件）在私有化环境中处理，API调用与数据传输全程HTTPS加密
- **NFR4:** 实现用户权限隔离（普通用户 vs 运营人员）与操作审计日志
- **NFR5:** 系统可用性 ≥ 99.5%
- **NFR6:** 具备完善的错误处理与降级策略（如检索失败时，引导至常见问题列表或提示联系人工）
- **NFR7:** 模型API调用异常时，系统给出友好提示而非暴露技术错误
- **NFR8:** 知识库支持200-300条初始数据规模，设计上可扩展至数千条
- **NFR9:** 用户密码/敏感信息加密存储，符合数据安全基本要求
- **NFR10:** 系统支持移动端网页自适应（嵌入校共体网站，需适配不同设备）
- **NFR11:** 系统须具备幻觉检测能力：Prompt层强制要求模型"仅基于检索片段回答"；回答中的引用标注须在检索结果中可追溯（后端校验）；对回答中出现的条款编号、比例数字做正则校验，确认其在知识库中真实存在；检索置信度低于阈值时直接声明"暂无相关政策依据"
- **NFR12:** AI生成内容须经过合规审核层：免责声明自动追加（FR38）+ 敏感词过滤（FR39），确保输出内容不含政治敏感、虚假政策信息或不当言论
- **NFR13:** 多轮对话上下文管理须设置明确边界：单次对话最大轮数≤30轮（超出后提示开启新对话）；上下文Token总量限制≤8000 tokens（超出时采用滑动窗口截断早期消息，保留最近N轮）；Redis存储的会话上下文设置TTL（默认24小时）
- **NFR14:** 语义缓存命中率目标≥30%（基于初期高频政策问题的重复率估算），缓存条目TTL默认7天，知识库变更时主动失效

---

## 3. User Interface Design Goals

### 3.1 Overall UX Vision

面向教育工作者的简洁、专业、低学习成本的对话式交互界面。整体风格庄重但不沉闷，兼顾政务属性与亲和力。核心交互以对话为主、侧边栏辅助导航，降低教育行业用户（尤其是乡村教师）的使用门槛。

### 3.2 Key Interaction Paradigms

- **对话式交互为主：** 中央区域为聊天式问答界面，空会话时默认展示6个推荐问题+换一批，支持文本输入和推荐问题点击
- **侧边栏辅助导航：** 顶部放置"新对话"按钮，下方展示政策动态、相关案例（各区块带"换一批"按钮），不再放置推荐问题
- **引用可追溯：** 回答中的政策引用可点击展开/跳转查看原文
- **反馈内嵌：** 每条回答下方直接嵌入评分和反馈组件，无需跳转
- **反馈记录可查：** 用户可查看自己提交的反馈处理结果

### 3.3 Core Screens and Views

**用户端：**

1. **登录/注册页** — 手机号输入、验证码、角色选择
2. **主对话界面** — 聊天窗口（中间默认6个推荐问题+换一批） + 侧边栏（新对话按钮/政策动态/相关案例+换一批）
3. **原文预览弹窗/页面** — PDF/文档在线预览，定位到引用片段
4. **历史对话列表** — 查看和恢复历史对话会话
5. **反馈记录页** — 查看已提交反馈及平台处理结果

**运营后台：**

5. **知识库管理页** — 文件上传（选择可见角色）、知识条目列表（显示可见角色）、CRUD操作、已上传文件在线预览
6. **脱敏审核页** — 查看自动脱敏结果、确认/修改后入库
7. **问答审核页** — 审核优质问答、一键入库
8. **用户反馈管理页** — 查看反馈评分、处理不满意提交；标记完成时弹框输入处理结果（长文本必填）
9. **对话日志与分析页** — 对话记录查看、基础统计分析
10. **内容配置页** — 快捷问题菜单配置、角色推荐内容配置
11. **推荐问题管理页** — 按角色管理推荐问题（一问一答格式），显示在对话框下方

### 3.4 Accessibility

WCAG AA — 考虑到乡村教师群体，需确保字体清晰可读、对比度足够、操作区域足够大

### 3.5 Branding

与校共体网站整体视觉风格保持一致，嵌入式部署，采用网站现有配色和品牌元素

### 3.6 Target Device and Platforms

Web Responsive — 嵌入校共体网站，需适配桌面端和移动端浏览器

---

## 4. Technical Assumptions

### 4.1 Repository Structure: Monorepo

采用Monorepo结构，前后端在同一仓库中管理，简化部署和版本协同。

### 4.2 Service Architecture

前后端分离 + 智能中台设计：

- **前端：** Vue 3 + Element Plus SPA，嵌入校共体网站
- **后端：** JDK 21 + Spring Cloud 微服务框架，按业务拆分为1-2个服务模块（如：核心业务服务、AI智能服务），提供REST API
- **智能中台：** RAG检索引擎（混合检索+重排序）、文档解析管道、脱敏管道
- **模型层：** 通过API调用Qwen3系列模型

**模型分工：**

| 模型 | 用途 |
|------|------|
| Qwen3-32B | 主推理（问答生成、多轮对话、NER脱敏） |
| Qwen3-VL-235B | 文档中表格/流程图的视觉理解 |
| Qwen3-Embedding-8B | 文档和查询向量化 |
| Qwen3-Rerank-8B | 检索结果重排序 |

### 4.3 Testing Requirements

Unit + Integration — 后端API接口测试 + RAG检索质量测试 + 前端组件测试

### 4.4 Additional Technical Assumptions and Requests

- **数据库：** PostgreSQL + pgvector扩展（业务数据+向量数据统一管理）
- **缓存：** Redis 7（用户会话上下文、热点知识缓存、分布式Session）
- **文档解析：** PDF解析（Apache PDFBox）、Word解析（Apache POI）、流程图识别（Qwen3-VL）
- **文件存储：** 原始文件本地/对象存储，需保留用于原文跳转预览和在线预览
- **认证方案：** JWT Token（手机号+验证码登录），Spring Security + JWT 实现
- **部署：** 本地服务器或云主机均可，无需GPU（模型通过API调用）
- **前端框架：** Vue 3 + Element Plus（已确认）
- **后端框架：** JDK 21 + Spring Boot 3.x + Spring Cloud（已确认），构建工具 Maven/Gradle
- **外部数据源：** 校共体网站搜索API（默认域名 `https://edu.51xiaoping.com`，通过 `app.platform.base-url` 配置项动态调整）；API路径 `/api/content/v1/search`，支持 `keyword`、`page`、`size` 参数；返回结构包含 `data.list[]`（含 module/id/title/description/publishTime/url）和分页信息（total/page/size）；内容模块包括：示范校、新闻资讯、政策文件、资源共享、活动、月报

---

## 5. Epic List

- **Epic 1: 基础设施与用户系统** — 搭建项目基础架构、用户注册登录、角色体系、JWT认证与会话管理
- **Epic 2: 知识库与文档处理引擎** — 文档解析管道（PDF/Word/表格/流程图）、向量化入库、混合检索+重排序、原文定位索引、平台数据实时同步与向量化
- **Epic 3: 核心对话与政策问答** — RAG问答引擎、多轮对话上下文管理、强制引用溯源、话题边界感知、常用问题快捷菜单
- **Epic 4: 角色化服务与智能推荐** — 角色差异化推送、案例自动脱敏管道、智能案例推荐、资源精准推送
- **Epic 5: 用户反馈与知识自进化** — 满意度评分、不满意提交、问答审核入库、用户问题自动入库
- **Epic 6: 运营后台** — 知识库CRUD管理（可见角色、文件预览）、文件上传与解析、脱敏审核、反馈处理（长文本处理结果）、对话日志、内容配置管理、推荐问题管理（角色区分一问一答）

---

## 6. Epics

## Epic 1: 基础设施与用户系统

**Epic Summary:** 搭建项目基础架构，实现用户手机号注册/登录、角色选择、JWT认证、会话保持与使用日志记录，为后续所有功能模块提供基础支撑。

**Target Repositories:** monolith

```yaml
epic_id: 1
title: "基础设施与用户系统"
description: |
  建立项目基础设施（前后端框架、数据库、缓存），实现完整的用户系统，
  包括手机号注册/登录、角色选择、JWT认证保持登录、使用日志记录。
  此Epic是所有后续功能的基础依赖。

stories:
  - id: "1.1"
    title: "项目初始化与基础架构搭建"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "后端项目初始化"
        scenario:
          given: "空项目仓库"
          when: "开发人员执行项目初始化"
          then:
            - "Spring Boot 3.x + Spring Cloud 微服务项目结构创建完成，包含 Controller、Service、Repository 分层目录"
            - "PostgreSQL数据库连接配置完成（Spring Data JPA）"
            - "Redis 7 连接配置完成（Spring Data Redis）"
            - "健康检查接口 GET /api/health 返回200（Spring Boot Actuator）"
        business_rules:
          - id: "BR-1.1"
            rule: "项目采用 JDK 21 + Spring Boot 3.x + Spring Cloud 微服务框架，按业务拆分1-2个服务模块"
          - id: "BR-1.2"
            rule: "数据库使用PostgreSQL + pgvector扩展"
          - id: "BR-1.3"
            rule: "使用Flyway进行数据库迁移管理"
        error_handling:
          - scenario: "数据库连接失败"
            code: "503"
            message: "服务暂时不可用"
            action: "启动时检查连接，失败则记录日志并退出"

      - id: AC2
        title: "前端项目初始化"
        scenario:
          given: "空项目仓库"
          when: "开发人员执行前端初始化"
          then:
            - "前端项目结构创建完成（Vue 3 + Element Plus）"
            - "Vue Router 路由配置、Pinia 状态管理、Axios HTTP请求封装完成"
            - "开发环境可正常启动并显示首页"
        business_rules:
          - id: "BR-2.1"
            rule: "前端技术栈确认为 Vue 3 + Element Plus + Pinia + Vue Router"
          - id: "BR-2.2"
            rule: "需支持响应式布局（桌面端+移动端）"
        error_handling:
          - scenario: "后端API不可达"
            code: "NETWORK_ERROR"
            message: "服务连接中，请稍后重试"
            action: "前端显示友好提示，不暴露技术细节"

    provides_apis:
      - "GET /api/health"
    consumes_apis: []
    dependencies: []

  - id: "1.2"
    title: "手机号注册与角色选择"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "手机号注册"
        scenario:
          given: "用户未注册"
          when: "用户输入手机号、验证码、用户名、选择角色并提交注册"
          then:
            - "用户记录创建到数据库"
            - "密码/验证码安全处理"
            - "返回注册成功并引导登录"
        business_rules:
          - id: "BR-1.1"
            rule: "手机号必须唯一，不可重复注册"
          - id: "BR-1.2"
            rule: "角色为必选项，可选值：区域教育管理人员、校共体核心牵头校人员、校共体成员校人员（城镇）、校共体成员校人员（乡村）、研究人员、其他人员"
          - id: "BR-1.3"
            rule: "用户名为必填项，2-20个字符"
        data_validation:
          - field: "phone"
            type: "string"
            required: true
            rules: "中国大陆手机号格式，11位数字，1开头"
            error_message: "请输入正确的手机号"
          - field: "sms_code"
            type: "string"
            required: true
            rules: "6位数字验证码，5分钟有效期"
            error_message: "验证码错误或已过期"
          - field: "username"
            type: "string"
            required: true
            rules: "2-20个字符"
            error_message: "用户名需2-20个字符"
          - field: "role"
            type: "string"
            required: true
            rules: "枚举值，必须为6种角色之一"
            error_message: "请选择您的角色身份"
        error_handling:
          - scenario: "手机号已注册"
            code: "409"
            message: "该手机号已注册，请直接登录"
            action: "返回错误，提供登录入口链接"
          - scenario: "验证码错误"
            code: "400"
            message: "验证码错误或已过期，请重新获取"
            action: "清空验证码输入框，允许重新获取"
          - scenario: "短信发送失败"
            code: "503"
            message: "短信发送失败，请稍后重试"
            action: "记录日志，60秒后允许重新发送"
        interaction:
          - trigger: "点击获取验证码"
            behavior: "发送短信，按钮变为60秒倒计时"
          - trigger: "提交注册"
            behavior: "显示加载状态，成功后跳转登录页"
        examples:
          - input: "手机号: 13800138000, 验证码: 123456, 用户名: 张老师, 角色: 校共体成员校人员（乡村）"
            expected: "注册成功，跳转至登录页"

      - id: AC2
        title: "角色选择界面"
        scenario:
          given: "用户在注册页面"
          when: "用户进入角色选择步骤"
          then:
            - "展示6种角色选项，每个角色附带简短说明"
            - "用户选择后角色标签高亮"
            - "角色选择结果随注册信息一起提交"
        business_rules:
          - id: "BR-2.1"
            rule: "必须选择且只能选择一个角色"
          - id: "BR-2.2"
            rule: "角色选择后可在个人设置中修改"
        error_handling:
          - scenario: "未选择角色直接提交"
            code: "400"
            message: "请选择您的角色身份"
            action: "角色选择区域高亮提示"

    provides_apis:
      - "POST /api/auth/send-sms"
      - "POST /api/auth/register"
    consumes_apis: []
    dependencies: ["1.1"]

  - id: "1.3"
    title: "登录与会话管理"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "手机号登录"
        scenario:
          given: "用户已注册"
          when: "用户输入手机号和验证码登录"
          then:
            - "验证通过后生成JWT Token（access_token + refresh_token）"
            - "access_token有效期2小时，refresh_token有效期7天"
            - "登录时间记录到用户日志"
            - "跳转至主对话界面"
        business_rules:
          - id: "BR-1.1"
            rule: "登录方式为手机号+短信验证码"
          - id: "BR-1.2"
            rule: "access_token过期后自动使用refresh_token刷新，用户无感知"
          - id: "BR-1.3"
            rule: "refresh_token过期后需重新登录"
          - id: "BR-1.4"
            rule: "记录每次登录的时间、IP、设备信息"
        data_validation:
          - field: "phone"
            type: "string"
            required: true
            rules: "中国大陆手机号格式"
            error_message: "请输入正确的手机号"
          - field: "sms_code"
            type: "string"
            required: true
            rules: "6位数字验证码"
            error_message: "验证码错误或已过期"
        error_handling:
          - scenario: "手机号未注册"
            code: "404"
            message: "该手机号未注册，请先注册"
            action: "提供注册入口链接"
          - scenario: "验证码错误"
            code: "401"
            message: "验证码错误或已过期"
            action: "允许重新获取验证码"
          - scenario: "频繁登录尝试"
            code: "429"
            message: "登录尝试过于频繁，请5分钟后再试"
            action: "临时锁定5分钟"
        examples:
          - input: "POST /api/auth/login {phone: '13800138000', sms_code: '123456'}"
            expected: "200 OK {access_token: 'jwt...', refresh_token: 'jwt...', user: {id, username, role}}"

      - id: AC2
        title: "Token自动刷新与会话保持"
        scenario:
          given: "用户已登录且access_token即将过期"
          when: "前端发起API请求"
          then:
            - "自动使用refresh_token获取新的access_token"
            - "用户无感知，请求正常完成"
        business_rules:
          - id: "BR-2.1"
            rule: "前端在access_token剩余5分钟时主动刷新"
          - id: "BR-2.2"
            rule: "refresh_token刷新时同时更新refresh_token（滑动过期）"
        error_handling:
          - scenario: "refresh_token已过期"
            code: "401"
            message: "登录已过期，请重新登录"
            action: "清除本地Token，跳转至登录页"

    provides_apis:
      - "POST /api/auth/login"
      - "POST /api/auth/refresh"
      - "POST /api/auth/logout"
    consumes_apis: []
    dependencies: ["1.1", "1.2"]

  - id: "1.4"
    title: "用户使用日志记录"
    repository_type: monolith
    estimated_complexity: low
    priority: P1

    acceptance_criteria:
      - id: AC1
        title: "对话日志自动记录"
        scenario:
          given: "用户已登录并进行对话"
          when: "用户发送消息或收到智能体回复"
          then:
            - "每条消息（用户询问和智能体回复）记录到数据库"
            - "记录包含：用户ID、会话ID、消息角色（user/assistant）、消息内容、时间戳"
        business_rules:
          - id: "BR-1.1"
            rule: "对话按会话（session）组织，每次新对话创建新会话"
          - id: "BR-1.2"
            rule: "日志数据保留期限至少1年"
        error_handling:
          - scenario: "日志写入失败"
            code: "N/A"
            message: "不影响用户对话"
            action: "异步写入，失败时记录错误日志，不中断对话"

    provides_apis:
      - "GET /api/conversations"
      - "GET /api/conversations/{id}/messages"
    consumes_apis: []
    dependencies: ["1.3"]
```

## Epic 2: 知识库与文档处理引擎

**Epic Summary:** 构建文档解析管道（支持PDF/Word含表格和流程图）、文本切分与向量化入库、混合检索引擎（向量+关键词+重排序）、原文定位索引，以及校共体网站平台数据实时同步与向量化入库，为RAG问答提供高质量的知识检索基础。

**Target Repositories:** monolith

```yaml
epic_id: 2
title: "知识库与文档处理引擎"
description: |
  实现完整的文档处理管道和检索引擎：支持PDF/Word文件的结构化解析（含表格和流程图），
  按章-条-款语义切分，向量化后入库PostgreSQL+pgvector。构建混合检索引擎（稠密向量检索
  +关键词检索+Qwen3-Rerank重排序），并建立片段→原文位置映射索引，支持原文跳转。
  同时对接校共体网站搜索API，实现平台数据（6大模块）的定时增量同步与向量化入库，
  持续丰富知识库语料。

stories:
  - id: "2.1"
    title: "文档解析管道 — 文本与表格提取"
    repository_type: monolith
    estimated_complexity: high
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "PDF文本与表格解析"
        scenario:
          given: "运营上传一份包含文本和表格的PDF政策文件"
          when: "系统执行文档解析"
          then:
            - "正确提取PDF中的纯文本内容，保留段落和层级结构"
            - "正确提取PDF中的表格，转为结构化数据（行×列）"
            - "记录每个文本片段和表格在原文中的页码位置"
        business_rules:
          - id: "BR-1.1"
            rule: "使用Apache PDFBox进行PDF解析"
          - id: "BR-1.2"
            rule: "表格提取为JSON结构，保留表头信息"
          - id: "BR-1.3"
            rule: "每个提取片段必须记录：文件ID、页码、段落序号/表格序号"
        error_handling:
          - scenario: "PDF文件损坏或加密"
            code: "422"
            message: "文件无法解析，请确认文件是否完整且未加密"
            action: "记录错误，通知运营人员"
          - scenario: "表格跨页"
            code: "N/A"
            message: "N/A"
            action: "尝试合并跨页表格，合并失败时按页拆分"

      - id: AC2
        title: "Word文档解析"
        scenario:
          given: "运营上传一份Word文档"
          when: "系统执行文档解析"
          then:
            - "正确提取Word中的文本、标题层级、表格"
            - "保留文档结构（标题→正文的层级关系）"
        business_rules:
          - id: "BR-2.1"
            rule: "使用Apache POI进行Word解析"
          - id: "BR-2.2"
            rule: "利用Word内置的标题样式（Heading 1/2/3）识别文档结构"
        error_handling:
          - scenario: "Word格式不兼容（.doc旧格式）"
            code: "422"
            message: "请上传.docx格式文件"
            action: "提示用户转换格式后重新上传"

      - id: AC3
        title: "流程图/图片识别"
        scenario:
          given: "文档中包含流程图或示意图"
          when: "系统检测到文档中的图片元素"
          then:
            - "调用Qwen3-VL-235B对图片进行视觉理解"
            - "生成图片的文字描述"
            - "文字描述作为独立知识片段入库"
        business_rules:
          - id: "BR-3.1"
            rule: "仅对>100x100像素的图片调用视觉模型，过小的忽略"
          - id: "BR-3.2"
            rule: "视觉模型返回的描述需标注来源为图片识别"
        error_handling:
          - scenario: "视觉模型API调用失败"
            code: "N/A"
            message: "N/A"
            action: "跳过该图片，记录日志，不阻塞整体解析流程"

    provides_apis:
      - "POST /api/admin/documents/upload"
      - "GET /api/admin/documents/{id}/status"
    consumes_apis: []
    dependencies: ["1.1"]

  - id: "2.2"
    title: "文本切分与向量化入库"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "语义切分"
        scenario:
          given: "文档解析完成，产出结构化文本片段"
          when: "系统执行切分流程"
          then:
            - "按章-条-款进行语义切分，每个切分单元语义完整"
            - "切分片段包含元数据：文件ID、文件名、章节标题、页码、段落序号"
            - "切分长度控制在500-1000 token范围内，过长时递归拆分"
        business_rules:
          - id: "BR-1.1"
            rule: "优先按文档结构（标题层级）切分，次选按段落切分"
          - id: "BR-1.2"
            rule: "切分片段之间保留50-100 token重叠，确保上下文连贯"
          - id: "BR-1.3"
            rule: "表格作为独立切分单元，附带表格标题/上下文"

      - id: AC2
        title: "向量化与入库"
        scenario:
          given: "文本切分完成"
          when: "系统执行向量化"
          then:
            - "调用Qwen3-Embedding-8B对每个片段生成向量"
            - "向量与元数据一起存储到PostgreSQL pgvector"
            - "同时建立关键词倒排索引（用于混合检索）"
        business_rules:
          - id: "BR-2.1"
            rule: "向量维度与Qwen3-Embedding-8B输出一致"
          - id: "BR-2.2"
            rule: "入库后建立HNSW索引加速向量检索"
        error_handling:
          - scenario: "Embedding API调用失败"
            code: "N/A"
            message: "N/A"
            action: "重试3次，仍失败则标记该片段为pending，记录日志"

    provides_apis: []
    consumes_apis: []
    dependencies: ["2.1"]

  - id: "2.3"
    title: "混合检索引擎与重排序"
    repository_type: monolith
    estimated_complexity: high
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "混合检索"
        scenario:
          given: "知识库已有向量化的文档片段"
          when: "接收到用户查询"
          then:
            - "同时执行向量语义检索和关键词检索"
            - "合并两路检索结果（加权融合）"
            - "调用Qwen3-Rerank-8B对合并结果重排序"
            - "返回Top-K最相关的文档片段（含元数据）"
        business_rules:
          - id: "BR-1.1"
            rule: "向量检索返回Top-20，关键词检索返回Top-20，合并后重排序取Top-5"
          - id: "BR-1.2"
            rule: "检索结果必须包含：片段文本、文件名、章节标题、页码、相关性分数"
          - id: "BR-1.3"
            rule: "检索延迟（不含模型推理）控制在500ms内"
        error_handling:
          - scenario: "向量检索失败"
            code: "N/A"
            message: "N/A"
            action: "降级为纯关键词检索，记录日志"
          - scenario: "Rerank API调用失败"
            code: "N/A"
            message: "N/A"
            action: "使用未重排序的合并结果，记录日志"

      - id: AC2
        title: "原文定位索引"
        scenario:
          given: "检索返回相关片段"
          when: "用户点击引用出处"
          then:
            - "系统根据片段元数据（文件ID+页码+段落）定位原文位置"
            - "前端打开原文预览，自动滚动到对应位置"
        business_rules:
          - id: "BR-2.1"
            rule: "PDF文件使用页码定位"
          - id: "BR-2.2"
            rule: "原文文件需持久化存储，支持在线预览"
        error_handling:
          - scenario: "原文文件被删除或不可用"
            code: "404"
            message: "原文文件暂时不可用"
            action: "显示片段文本内容作为替代"

    provides_apis:
      - "POST /api/search"
      - "GET /api/documents/{id}/preview"
    consumes_apis: []
    dependencies: ["2.2"]

  - id: "2.4"
    title: "平台数据实时同步与向量化入库"
    repository_type: monolith
    estimated_complexity: medium
    priority: P1

    acceptance_criteria:
      - id: AC1
        title: "平台搜索API对接与数据拉取"
        scenario:
          given: "校共体网站搜索API可用（{configurable_domain}/api/content/v1/search）"
          when: "系统发起平台数据搜索请求"
          then:
            - "按keyword、page、size参数调用API，正确解析返回JSON结构"
            - "提取list中每条记录的module、id、title、description、publishTime、url字段"
            - "按6大内容模块（示范校/新闻资讯/政策文件/资源共享/活动/月报）分类存储"
        business_rules:
          - id: "BR-1.1"
            rule: "API域名通过配置项 app.platform.base-url 动态管理，默认值 https://edu.51xiaoping.com，禁止硬编码"
          - id: "BR-1.2"
            rule: "API返回的module字段映射：activity→活动、resource→资源共享、news→新闻资讯、policy→政策文件、model_school→示范校、monthly_report→月报"
          - id: "BR-1.3"
            rule: "分页拉取，单次最大size=50，遍历所有页直至total耗尽"
        error_handling:
          - scenario: "平台API不可达或返回非200"
            code: "502"
            message: "平台数据暂时不可用"
            action: "记录日志，跳过本次同步，不影响已有知识库数据"
          - scenario: "返回数据格式异常"
            code: "422"
            message: "数据格式解析失败"
            action: "记录异常数据，跳过该条记录继续处理"

      - id: AC2
        title: "搜索结果向量化入知识库"
        scenario:
          given: "成功获取平台搜索结果"
          when: "系统执行向量化入库流程"
          then:
            - "将title + description拼接为文本，调用Qwen3-Embedding-8B向量化"
            - "向量与元数据（module、原始ID、title、publishTime、原始URL）一并写入pgvector"
            - "基于module+原始ID去重，已存在记录仅更新（title/description/publishTime变更时）"
        business_rules:
          - id: "BR-2.1"
            rule: "向量化文本格式：[模块类型] title\\n\\ndescription（description为空时仅用title）"
          - id: "BR-2.2"
            rule: "元数据须包含source_type='platform'以区别于运营上传的知识条目"
          - id: "BR-2.3"
            rule: "原始URL存储为相对路径，展示时拼接配置域名，支持跳转至校共体网站原文"
        error_handling:
          - scenario: "Embedding API调用失败"
            code: "500"
            message: "向量化处理失败"
            action: "记录失败记录ID，下次同步时重试"

      - id: AC3
        title: "定时增量同步与手动全量同步"
        scenario:
          given: "系统已完成首次全量同步"
          when: "到达定时同步时间点或运营手动触发"
          then:
            - "定时任务基于上次同步的最大publishTime增量拉取新数据"
            - "运营后台提供'手动同步'按钮，触发全量重新拉取"
            - "同步结果记录日志（新增条数/更新条数/失败条数）"
        business_rules:
          - id: "BR-3.1"
            rule: "定时同步频率默认每6小时一次，通过配置项 app.platform.sync-cron 调整"
          - id: "BR-3.2"
            rule: "增量同步依据publishTime字段，仅拉取晚于上次同步时间的记录"
          - id: "BR-3.3"
            rule: "同步任务须加分布式锁（Redis），防止并发执行"
        error_handling:
          - scenario: "同步过程中断（如服务重启）"
            code: "N/A"
            message: "N/A"
            action: "下次定时触发时自动从断点续拉（基于publishTime）"

    provides_apis:
      - "POST /api/admin/platform/sync"
      - "GET /api/admin/platform/sync-status"
    consumes_apis:
      - "{configurable_domain}/api/content/v1/search"
    dependencies: ["2.2"]
```

## Epic 3: 核心对话与政策问答

**Epic Summary:** 基于RAG检索引擎构建核心对话能力，包括：LLM问答生成（强制引用溯源）、多轮对话上下文管理、话题边界感知、常用问题快捷菜单、流式输出，实现用户可直接使用的政策问答功能。

**Target Repositories:** monolith

```yaml
epic_id: 3
title: "核心对话与政策问答"
description: |
  基于Epic 2的检索引擎，构建核心对话服务。集成Qwen3-32B进行答案生成，
  强制引用政策出处。实现多轮对话上下文管理（Redis缓存），话题边界感知，
  常用问题快捷菜单，以及对话界面的完整前端实现。

stories:
  - id: "3.1"
    title: "RAG问答引擎与强制引用"
    repository_type: monolith
    estimated_complexity: high
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "基于检索结果生成回答"
        scenario:
          given: "用户提出政策相关问题"
          when: "系统完成检索并调用LLM生成回答"
          then:
            - "调用Qwen3-32B，将检索到的Top-K片段作为上下文生成回答"
            - "回答中必须包含引用标注（如 [1]、[2]）"
            - "回答末尾附带引用列表，包含文件名和章节信息"
            - "支持流式输出（SSE），实现逐字显示效果"
        business_rules:
          - id: "BR-1.1"
            rule: "系统提示词强制要求LLM在回答中引用出处"
          - id: "BR-1.2"
            rule: "如果检索结果与问题不相关（置信度低），明确告知用户'该问题暂无相关政策依据，建议咨询专家'"
          - id: "BR-1.3"
            rule: "回答语言风格：专业但易懂，适合教育工作者阅读"
          - id: "BR-1.4"
            rule: "回答长度控制在300-800字，过长时分点阐述"
        error_handling:
          - scenario: "LLM API调用失败"
            code: "503"
            message: "智能助手暂时繁忙，请稍后重试"
            action: "重试1次，仍失败则返回友好提示"
          - scenario: "检索无结果"
            code: "200"
            message: "N/A"
            action: "LLM基于通用知识回答，但明确标注'以下回答未找到相关政策文件支持'"
        examples:
          - input: "校共体怎么结对？"
            expected: |
              回答包含结对模式的详细说明，引用如：
              [1]《河南省县域结对型城乡学校共同体建设指南》第三章第二节
              可点击[1]查看原文

      - id: AC2
        title: "引用可点击跳转"
        scenario:
          given: "智能体回答中包含引用标注"
          when: "用户点击引用标注（如 [1]）"
          then:
            - "打开原文预览弹窗/页面"
            - "自动定位到被引用的具体片段位置"
            - "被引用文字高亮显示"
        business_rules:
          - id: "BR-2.1"
            rule: "引用标注为可点击的链接样式"
          - id: "BR-2.2"
            rule: "预览支持PDF在线渲染"
        error_handling:
          - scenario: "原文加载失败"
            code: "N/A"
            message: "原文加载失败"
            action: "显示引用片段的纯文本内容作为替代"
        interaction:
          - trigger: "点击引用标注"
            behavior: "弹出原文预览弹窗，加载中显示骨架屏"
          - trigger: "关闭预览弹窗"
            behavior: "返回对话界面，对话位置不变"

    provides_apis:
      - "POST /api/chat"
      - "GET /api/chat/stream"
    consumes_apis:
      - "POST /api/search"
      - "GET /api/documents/{id}/preview"
    dependencies: ["2.3"]

  - id: "3.2"
    title: "多轮对话与上下文管理"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "对话上下文保持"
        scenario:
          given: "用户正在进行多轮对话"
          when: "用户进行连续追问（如'上面说的评估指标具体有哪些？'）"
          then:
            - "系统理解'上面'指代前文内容，正确检索并回答"
            - "上下文窗口保持最近5轮对话历史"
            - "对话历史缓存在Redis中"
        business_rules:
          - id: "BR-1.1"
            rule: "上下文窗口为最近5轮（10条消息：5条用户+5条助手）"
          - id: "BR-1.2"
            rule: "超出窗口的历史消息仍保存在数据库，但不传入LLM"
          - id: "BR-1.3"
            rule: "对话上下文用于优化检索查询（query rewriting）"
        error_handling:
          - scenario: "Redis缓存不可用"
            code: "N/A"
            message: "N/A"
            action: "降级为从数据库读取最近对话历史"

      - id: AC2
        title: "话题边界感知"
        scenario:
          given: "用户在长时间对话中突然切换话题"
          when: "系统检测到当前问题与前文上下文关联度低"
          then:
            - "系统温和确认：'您现在想了解的是XX方面的内容吗？'"
            - "如果用户确认新话题，清空当前上下文窗口开始新话题"
        business_rules:
          - id: "BR-2.1"
            rule: "话题切换检测基于当前问题与上下文的语义相似度"
          - id: "BR-2.2"
            rule: "仅在相似度低于阈值时触发确认，不影响正常连续对话"
        error_handling:
          - scenario: "话题检测误判"
            code: "N/A"
            message: "N/A"
            action: "用户可忽略确认直接追问，系统恢复上下文"

    provides_apis: []
    consumes_apis: []
    dependencies: ["3.1"]

  - id: "3.3"
    title: "对话界面与快捷菜单"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "主对话界面"
        scenario:
          given: "用户已登录"
          when: "进入主界面"
          then:
            - "中央区域显示对话聊天窗口"
            - "底部显示消息输入框和发送按钮"
            - "支持Enter发送，Shift+Enter换行"
            - "智能体回复逐字流式显示"
        business_rules:
          - id: "BR-1.1"
            rule: "对话界面响应式布局，适配桌面和移动端"
          - id: "BR-1.2"
            rule: "新消息自动滚动到底部"
          - id: "BR-1.3"
            rule: "支持Markdown格式渲染（标题、列表、表格、加粗等）"
        error_handling:
          - scenario: "消息发送失败"
            code: "N/A"
            message: "发送失败，点击重试"
            action: "消息标记为发送失败，显示重试按钮"
        interaction:
          - trigger: "用户输入文字"
            behavior: "输入框自适应高度，最多4行"
          - trigger: "点击发送/按Enter"
            behavior: "消息出现在对话区，输入框清空，显示加载动画直到回复开始流式输出"

      - id: AC2
        title: "推荐问题展示与换一批"
        scenario:
          given: "用户首次进入对话或开始新会话"
          when: "对话区为空"
          then:
            - "显示欢迎语，对话区域中间展示6个推荐问题卡片"
            - "推荐问题下方显示'换一批'按钮"
            - "用户点击推荐问题，直接展示运营预设的标准答案（一问一答）"
            - "用户点击'换一批'，随机刷新另外6条推荐问题"
        business_rules:
          - id: "BR-2.1"
            rule: "推荐问题由运营在后台按角色配置（见Story 6.5），支持增删改"
          - id: "BR-2.2"
            rule: "默认展示6个推荐问题，按用户角色过滤"
          - id: "BR-2.3"
            rule: "用户发送第一条自由输入消息后推荐问题区域收起"
          - id: "BR-2.4"
            rule: "推荐问题不足6条时全部展示，不显示'换一批'按钮"
        error_handling:
          - scenario: "快捷问题配置为空"
            code: "N/A"
            message: "N/A"
            action: "仅显示欢迎语和输入框，不显示快捷按钮"

      - id: AC3
        title: "历史对话列表"
        scenario:
          given: "用户有历史对话记录"
          when: "用户点击历史对话入口"
          then:
            - "显示历史会话列表，按时间倒序排列"
            - "每条显示会话标题（首条消息摘要）和时间"
            - "点击可恢复该会话的对话内容"
        business_rules:
          - id: "BR-3.1"
            rule: "会话标题取用户首条消息的前30个字符"
          - id: "BR-3.2"
            rule: "支持删除历史会话"
        error_handling:
          - scenario: "历史会话加载失败"
            code: "500"
            message: "历史记录加载失败，请刷新重试"
            action: "显示重试按钮"

    provides_apis: []
    consumes_apis:
      - "POST /api/chat"
      - "GET /api/conversations"
    dependencies: ["3.1", "3.2"]
```

## Epic 4: 角色化服务与智能推荐

**Epic Summary:** 实现基于用户角色的差异化内容推送、案例自动脱敏管道（NER+规则引擎+人工审核）、智能案例推荐、资源精准推送，提升服务的个性化和实用价值。

**Target Repositories:** monolith

```yaml
epic_id: 4
title: "角色化服务与智能推荐"
description: |
  基于用户角色实现差异化内容推送和回复调整。构建案例自动脱敏管道
  （Qwen3-32B NER识别+规则引擎+运营审核），实现智能案例推荐和
  资源精准推送。

stories:
  - id: "4.1"
    title: "角色差异化内容推送"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "侧边栏布局与角色相关资源推送"
        scenario:
          given: "用户已登录且绑定角色（如：校共体成员校人员-乡村）"
          when: "用户进入主对话界面"
          then:
            - "侧边栏顶部显示'新对话'按钮，方便用户快速开启新对话"
            - "侧边栏下方显示'政策动态'和'相关案例'区块，各区块右侧有'换一批'按钮"
            - "开场欢迎语包含角色相关的任务引导"
            - "不同角色看到不同的资源推送内容"
        business_rules:
          - id: "BR-1.1"
            rule: "6类角色各有独立的资源推送内容配置"
          - id: "BR-1.2"
            rule: "资源推送内容由运营在后台配置管理"
          - id: "BR-1.3"
            rule: "侧边栏在移动端可折叠"
          - id: "BR-1.4"
            rule: "'换一批'点击后从该角色的资源池中随机刷新展示，不与当前内容重复"
          - id: "BR-1.5"
            rule: "侧边栏不再放置推荐问题（推荐问题已移至对话区域中间展示）"
        error_handling:
          - scenario: "角色推荐内容未配置"
            code: "N/A"
            message: "N/A"
            action: "显示通用推荐内容"

      - id: AC2
        title: "角色感知的回复调整"
        scenario:
          given: "用户角色为'区域教育管理人员'"
          when: "用户提问"
          then:
            - "系统提示词中注入用户角色信息"
            - "LLM回复侧重宏观政策解读和管理决策建议"
        business_rules:
          - id: "BR-2.1"
            rule: "角色信息作为系统提示词的一部分传入LLM"
          - id: "BR-2.2"
            rule: "角色影响回复的侧重点和专业深度，不改变事实准确性"
        error_handling:
          - scenario: "角色信息缺失"
            code: "N/A"
            message: "N/A"
            action: "使用通用系统提示词，不做角色定制"

    provides_apis:
      - "GET /api/recommendations/{role}"
    consumes_apis: []
    dependencies: ["1.2", "3.3"]

  - id: "4.2"
    title: "案例自动脱敏管道"
    repository_type: monolith
    estimated_complexity: high
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "自动脱敏处理"
        scenario:
          given: "运营上传原始案例文献"
          when: "系统执行脱敏管道"
          then:
            - "NER识别文中的人名、学校名、地名、机构名"
            - "正则匹配手机号、身份证号、邮箱等"
            - "精确数字模糊化为量级表述"
            - "具体日期模糊化为季度/年份"
            - "生成脱敏后的文本版本"
            - "标记为'待审核'状态"
        business_rules:
          - id: "BR-1.1"
            rule: "NER识别使用Qwen3-32B提示词驱动，零样本识别"
          - id: "BR-1.2"
            rule: "脱敏规则：人名→某教师/某校长，校名→某小学/某中学，地名→某县/某镇（省级可保留），数字→量级近似（103→近百），联系方式→删除，日期→季度/年份"
          - id: "BR-1.3"
            rule: "系统保留原始→脱敏的映射表，仅运营可查看"
          - id: "BR-1.4"
            rule: "脱敏结果必须经运营审核后方可入库"
        error_handling:
          - scenario: "NER API调用失败"
            code: "N/A"
            message: "N/A"
            action: "回退为纯正则脱敏，标记为'需人工复核'"
          - scenario: "脱敏遗漏（漏检实体）"
            code: "N/A"
            message: "N/A"
            action: "运营审核环节兜底，可手动补充脱敏"
        examples:
          - input: "2024年3月15日，XX县YY镇ZZ小学的张三老师带领103名学生..."
            expected: "2024年初，某县某镇某小学的某教师带领近百名学生..."

      - id: AC2
        title: "脱敏审核流程"
        scenario:
          given: "自动脱敏完成，案例状态为'待审核'"
          when: "运营人员在后台查看脱敏结果"
          then:
            - "左右对比显示原文和脱敏后文本"
            - "脱敏实体高亮标注"
            - "运营可修改脱敏结果"
            - "审核通过后案例自动向量化入库"
        business_rules:
          - id: "BR-2.1"
            rule: "审核操作：通过、修改后通过、拒绝"
          - id: "BR-2.2"
            rule: "拒绝时需填写原因"
        error_handling:
          - scenario: "向量化入库失败"
            code: "500"
            message: "入库失败，请重试"
            action: "案例状态回退为'审核通过待入库'，允许重试"

    provides_apis:
      - "POST /api/admin/cases/upload"
      - "GET /api/admin/cases/pending"
      - "PUT /api/admin/cases/{id}/review"
    consumes_apis: []
    dependencies: ["2.2"]

  - id: "4.3"
    title: "智能案例推荐与资源推送"
    repository_type: monolith
    estimated_complexity: medium
    priority: P1

    acceptance_criteria:
      - id: AC1
        title: "案例主动推荐"
        scenario:
          given: "用户咨询特定实践问题（如'如何开展线上集体备课'）"
          when: "系统完成政策问答回复"
          then:
            - "在回答末尾附加'相关案例推荐'区块"
            - "推荐1-3个知识库中匹配的成功实践案例（已脱敏）"
            - "案例以卡片形式展示：标题+摘要+点击展开"
        business_rules:
          - id: "BR-1.1"
            rule: "案例推荐基于问题与案例库的语义匹配"
          - id: "BR-1.2"
            rule: "仅推荐已通过脱敏审核的案例"
          - id: "BR-1.3"
            rule: "当无匹配案例时不显示推荐区块"
        error_handling:
          - scenario: "案例检索失败"
            code: "N/A"
            message: "N/A"
            action: "不显示推荐区块，不影响主回答"

      - id: AC2
        title: "资源精准推送"
        scenario:
          given: "用户有角色标签和历史对话记录"
          when: "系统有匹配的平台资源"
          then:
            - "在侧边栏推送相关政策动态、通知公告、课程资源链接"
            - "推送内容根据角色和最近对话主题动态更新"
        business_rules:
          - id: "BR-2.1"
            rule: "推送资源来源于运营配置的资源库"
          - id: "BR-2.2"
            rule: "每次最多推送5条资源"
          - id: "BR-2.3"
            rule: "已查看的资源不重复推送"
        error_handling:
          - scenario: "资源链接失效"
            code: "N/A"
            message: "N/A"
            action: "运营后台标记为失效，不再推送"

    provides_apis:
      - "GET /api/recommendations/cases"
      - "GET /api/recommendations/resources"
    consumes_apis:
      - "POST /api/search"
    dependencies: ["4.1", "4.2"]
```

## Epic 5: 用户反馈与知识自进化

**Epic Summary:** 实现用户满意度评分、不满意原因收集、问题提交至运营、优质问答审核入库、用户问题自动入库（去重筛选），形成知识库持续进化的闭环。

**Target Repositories:** monolith

```yaml
epic_id: 5
title: "用户反馈与知识自进化"
description: |
  构建完整的用户反馈闭环和知识库自进化机制。用户可对每条回答评分并提交
  不满意原因，可将问题转交运营人工处理。运营可将优质问答审核入库。
  用户新问题自动去重后进入待审核队列。

stories:
  - id: "5.1"
    title: "满意度评分与反馈收集"
    repository_type: monolith
    estimated_complexity: low
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "回答满意度评分"
        scenario:
          given: "智能体完成一条回答"
          when: "用户查看回答"
          then:
            - "回答下方显示1-5星评分组件"
            - "用户点击评分后立即提交"
            - "评分数据关联到具体消息ID"
        business_rules:
          - id: "BR-1.1"
            rule: "每条回答只能评分一次，评分后可修改一次"
          - id: "BR-1.2"
            rule: "评分为可选操作，不强制"
        error_handling:
          - scenario: "评分提交失败"
            code: "N/A"
            message: "评分提交失败，请重试"
            action: "保留评分状态，允许重新提交"

      - id: AC2
        title: "不满意原因收集"
        scenario:
          given: "用户评分≤2分"
          when: "评分提交后"
          then:
            - "展开反馈收集框，包含预设选项和自由文本输入"
            - "预设选项如：回答不准确、未引用出处、答非所问、内容过于笼统等"
            - "用户可选择多个选项并补充文字说明"
        business_rules:
          - id: "BR-2.1"
            rule: "评分>2分时也可展开反馈框（可选按钮）"
          - id: "BR-2.2"
            rule: "反馈收集框为可选填写，可直接关闭"
        error_handling:
          - scenario: "反馈内容过长"
            code: "400"
            message: "反馈内容不超过500字"
            action: "前端限制输入长度"

      - id: AC3
        title: "问题提交至运营"
        scenario:
          given: "用户对回答不满意"
          when: "用户点击'提交给专家处理'按钮"
          then:
            - "该问题和对话上下文提交到运营后台待处理队列"
            - "用户收到确认提示'您的问题已提交，运营人员将尽快处理'"
        business_rules:
          - id: "BR-3.1"
            rule: "提交时包含：原始问题、智能体回答、对话上下文、用户信息"
          - id: "BR-3.2"
            rule: "同一用户对同一问题不可重复提交"
        error_handling:
          - scenario: "提交失败"
            code: "500"
            message: "提交失败，请稍后重试"
            action: "允许重新提交"

    provides_apis:
      - "POST /api/feedback"
      - "POST /api/feedback/escalate"
    consumes_apis: []
    dependencies: ["3.1"]

  - id: "5.2"
    title: "知识库自进化机制"
    repository_type: monolith
    estimated_complexity: medium
    priority: P1

    acceptance_criteria:
      - id: AC1
        title: "优质问答审核入库"
        scenario:
          given: "运营在后台查看对话日志，发现优质问答"
          when: "运营点击'加入知识库'"
          then:
            - "该问答对进入审核流程"
            - "运营可编辑问答内容后确认入库"
            - "入库后自动向量化，成为后续检索的参考"
        business_rules:
          - id: "BR-1.1"
            rule: "入库的问答需标注来源为'优质问答沉淀'"
          - id: "BR-1.2"
            rule: "入库后立即生效，可被检索"
        error_handling:
          - scenario: "向量化失败"
            code: "500"
            message: "入库失败，请重试"
            action: "允许重试，失败记录日志"

      - id: AC2
        title: "用户问题自动入库"
        scenario:
          given: "用户提出新问题"
          when: "系统检测到该问题不在现有知识库中"
          then:
            - "问题自动进入'待审核'队列"
            - "系统对问题进行去重（与已有问题语义相似度>0.9则判为重复）"
            - "运营可批量审核并决定是否纳入知识库"
        business_rules:
          - id: "BR-2.1"
            rule: "去重基于语义相似度，阈值0.9"
          - id: "BR-2.2"
            rule: "重复问题合并计数，不重复入队"
          - id: "BR-2.3"
            rule: "每日自动批处理去重，非实时"
        error_handling:
          - scenario: "去重处理异常"
            code: "N/A"
            message: "N/A"
            action: "问题直接入队，标记为'未去重'，运营人工处理"

    provides_apis:
      - "POST /api/admin/qa/approve"
      - "GET /api/admin/qa/pending"
      - "GET /api/admin/questions/pending"
    consumes_apis: []
    dependencies: ["3.1", "2.2"]
```

## Epic 6: 运营后台

**Epic Summary:** 构建完整的运营管理后台，包括知识库CRUD管理、文件上传与解析触发、脱敏审核、用户反馈处理、对话日志查看与分析、常用问题和角色推荐内容配置管理。

**Target Repositories:** monolith

```yaml
epic_id: 6
title: "运营后台"
description: |
  构建运营管理后台的完整前端界面和对应API。整合前面各Epic中运营相关的功能，
  提供统一的管理入口。包括：知识库管理、文件上传、脱敏审核、反馈处理、
  对话日志分析、内容配置。

stories:
  - id: "6.1"
    title: "运营后台框架与权限"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "后台登录与权限隔离"
        scenario:
          given: "运营人员账号"
          when: "运营人员登录后台"
          then:
            - "运营后台独立登录入口"
            - "普通用户无法访问后台页面"
            - "后台导航包含所有管理功能模块入口"
        business_rules:
          - id: "BR-1.1"
            rule: "运营账号由超管创建，不支持自行注册"
          - id: "BR-1.2"
            rule: "后台API接口验证运营权限，普通用户Token无法调用"
        error_handling:
          - scenario: "普通用户尝试访问后台"
            code: "403"
            message: "无权限访问"
            action: "重定向到用户端首页"

    provides_apis:
      - "POST /api/admin/auth/login"
    consumes_apis: []
    dependencies: ["1.1"]

  - id: "6.2"
    title: "知识库管理界面"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "知识条目CRUD"
        scenario:
          given: "运营人员登录后台"
          when: "进入知识库管理页"
          then:
            - "显示知识条目列表（支持搜索、筛选、分页），列表中显示每条知识的可见角色标签"
            - "支持新增、编辑、删除知识条目"
            - "支持查看知识条目的来源（文件上传/问答沉淀/用户问题）"
            - "新增/编辑知识条目时须选择可见角色（多选），默认全部角色可见"
        business_rules:
          - id: "BR-1.1"
            rule: "删除操作需二次确认"
          - id: "BR-1.2"
            rule: "编辑后自动重新向量化"
          - id: "BR-1.3"
            rule: "可见角色为多选，可选值为6类用户角色，知识检索时按用户角色过滤"
        error_handling:
          - scenario: "删除已被引用的条目"
            code: "N/A"
            message: "N/A"
            action: "软删除，标记为不可用但保留数据"

      - id: AC2
        title: "文件上传与解析"
        scenario:
          given: "运营人员在知识库管理页"
          when: "上传新的PDF/Word文件"
          then:
            - "文件上传成功后自动触发解析管道"
            - "上传时须选择该文件的可见角色（多选）"
            - "显示解析进度和状态（解析中/成功/失败）"
            - "解析完成后知识条目自动入库，继承文件的可见角色配置"
            - "已上传文件列表提供'预览'按钮，点击可在线查看文件内容（PDF直接预览、Word转PDF预览）"
        business_rules:
          - id: "BR-2.1"
            rule: "支持批量上传，单文件大小≤50MB"
          - id: "BR-2.2"
            rule: "仅支持.pdf和.docx格式"
          - id: "BR-2.3"
            rule: "文件在线预览使用PDF.js或类似方案，Word文件服务端转换为PDF后预览"
        error_handling:
          - scenario: "文件格式不支持"
            code: "422"
            message: "仅支持PDF和Word(.docx)文件"
            action: "拒绝上传，提示正确格式"
          - scenario: "解析失败"
            code: "N/A"
            message: "文件解析失败"
            action: "显示错误原因，允许重新上传"

    provides_apis:
      - "GET /api/admin/knowledge"
      - "POST /api/admin/knowledge"
      - "PUT /api/admin/knowledge/{id}"
      - "DELETE /api/admin/knowledge/{id}"
    consumes_apis:
      - "POST /api/admin/documents/upload"
    dependencies: ["6.1", "2.1"]

  - id: "6.3"
    title: "反馈管理与对话日志"
    repository_type: monolith
    estimated_complexity: medium
    priority: P1

    acceptance_criteria:
      - id: AC1
        title: "用户反馈管理"
        scenario:
          given: "运营人员进入反馈管理页"
          when: "查看用户反馈列表"
          then:
            - "显示所有反馈记录（评分、原因、用户信息、时间）"
            - "支持按评分筛选、按时间排序"
            - "可查看关联的完整对话上下文"
            - "对于用户'提交专家处理'的问题，可标记处理状态"
            - "标记为'已完成'时，必须弹出长文本输入弹框，运营填写处理结果内容后方可完成标记"
            - "处理结果内容将展示给提交反馈的用户（在用户端'反馈记录'页面查看）"
        business_rules:
          - id: "BR-1.1"
            rule: "专家处理状态：待处理、处理中、已完成"
          - id: "BR-1.2"
            rule: "已完成的处理结果可选择同步至知识库，同步时默认状态为'审核通过'（无需二次审核），因为运营已在回复中确认了内容质量"
          - id: "BR-1.3"
            rule: "标记完成时处理结果为必填长文本（不少于10个字符），弹框使用 textarea 支持多行输入"
          - id: "BR-1.5"
            rule: "'加入知识库'按钮仅在运营已回复内容后才可用（必须先回复才能加入知识库）"
          - id: "BR-1.4"
            rule: "用户端反馈记录页显示：反馈状态、原始问题摘要、处理结果文本、处理时间"
        error_handling:
          - scenario: "关联对话已被删除"
            code: "N/A"
            message: "对话记录不可用"
            action: "显示反馈信息，标注对话不可查看"

      - id: AC2
        title: "对话日志与基础分析"
        scenario:
          given: "运营人员进入对话日志页"
          when: "查看对话数据"
          then:
            - "显示对话记录列表（用户、时间、消息条数、评分）"
            - "可展开查看完整对话内容"
            - "提供基础统计：日活用户数、日对话数、平均评分、热门问题Top10"
        business_rules:
          - id: "BR-2.1"
            rule: "日志数据支持按日期范围筛选"
          - id: "BR-2.2"
            rule: "热门问题基于用户问题语义聚类统计"
        error_handling:
          - scenario: "统计数据量过大"
            code: "N/A"
            message: "N/A"
            action: "分页加载，统计数据异步计算并缓存"

    provides_apis:
      - "GET /api/admin/feedback"
      - "PUT /api/admin/feedback/{id}/status"
      - "PUT /api/admin/feedback/{id}/resolve"
      - "GET /api/admin/conversations"
      - "GET /api/admin/analytics"
      - "GET /api/feedback/my-records"
    consumes_apis: []
    dependencies: ["6.1", "5.1"]

  - id: "6.4"
    title: "内容配置管理"
    repository_type: monolith
    estimated_complexity: low
    priority: P1

    acceptance_criteria:
      - id: AC1
        title: "快捷问题配置"
        scenario:
          given: "运营人员进入内容配置页"
          when: "管理常用问题快捷菜单"
          then:
            - "显示当前配置的快捷问题列表"
            - "支持新增、编辑、删除、排序"
            - "修改即时生效（用户刷新后可见）"
        business_rules:
          - id: "BR-1.1"
            rule: "最多配置20个快捷问题，前端展示取前8个"
          - id: "BR-1.2"
            rule: "支持拖拽排序"
        error_handling:
          - scenario: "保存失败"
            code: "500"
            message: "保存失败，请重试"
            action: "保留编辑状态，允许重新提交"

      - id: AC2
        title: "角色推荐内容配置"
        scenario:
          given: "运营人员在内容配置页"
          when: "配置某角色的推荐内容"
          then:
            - "按角色Tab展示各角色的推荐问题和资源列表"
            - "支持为每个角色独立配置推荐内容"
            - "支持新增、编辑、删除、排序"
        business_rules:
          - id: "BR-2.1"
            rule: "6个角色各自独立配置"
          - id: "BR-2.2"
            rule: "每个角色最多配置15条推荐内容"
        error_handling:
          - scenario: "某角色配置为空"
            code: "N/A"
            message: "N/A"
            action: "该角色用户看到通用推荐内容"

    provides_apis:
      - "GET /api/admin/config/quick-questions"
      - "PUT /api/admin/config/quick-questions"
      - "GET /api/admin/config/role-recommendations/{role}"
      - "PUT /api/admin/config/role-recommendations/{role}"
    consumes_apis: []
    dependencies: ["6.1"]

  - id: "6.5"
    title: "推荐问题管理"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "推荐问题CRUD管理"
        scenario:
          given: "运营人员进入推荐问题管理页"
          when: "管理推荐问题列表"
          then:
            - "按角色Tab展示各角色的推荐问题列表"
            - "每条推荐问题包含：问题文本 + 答案文本（一问一答格式）"
            - "支持新增、编辑、删除、排序推荐问题"
            - "支持为每个角色独立配置不同的推荐问题"
        business_rules:
          - id: "BR-1.1"
            rule: "6个用户角色各自独立管理推荐问题"
          - id: "BR-1.2"
            rule: "每个角色最多配置30条推荐问题"
          - id: "BR-1.3"
            rule: "推荐问题在对话框下方区域展示（非侧边栏），默认展示6条，支持'换一批'刷新"
          - id: "BR-1.4"
            rule: "用户点击推荐问题后，直接展示运营预设的标准答案（不触发AI推理）"
        error_handling:
          - scenario: "某角色推荐问题为空"
            code: "N/A"
            message: "N/A"
            action: "该角色用户不显示推荐问题区域"
          - scenario: "保存失败"
            code: "500"
            message: "保存失败，请重试"
            action: "保留编辑状态，允许重新提交"

      - id: AC2
        title: "前端推荐问题展示"
        scenario:
          given: "用户已登录并进入对话界面"
          when: "对话区为空（新会话）"
          then:
            - "对话区域中间展示6个推荐问题卡片"
            - "下方显示'换一批'按钮，点击随机刷新展示另外6条"
            - "用户点击推荐问题后，问题和预设答案以对话气泡形式展示"
        business_rules:
          - id: "BR-2.1"
            rule: "推荐问题按用户角色过滤，展示该角色下的推荐问题"
          - id: "BR-2.2"
            rule: "'换一批'从该角色全部推荐问题中随机选取6条，不与当前展示的重复"
          - id: "BR-2.3"
            rule: "推荐问题不足6条时全部展示，不显示'换一批'按钮"
        error_handling:
          - scenario: "推荐问题加载失败"
            code: "N/A"
            message: "N/A"
            action: "仅显示欢迎语和输入框"

    provides_apis:
      - "GET /api/admin/recommended-questions"
      - "GET /api/admin/recommended-questions/{role}"
      - "POST /api/admin/recommended-questions"
      - "PUT /api/admin/recommended-questions/{id}"
      - "DELETE /api/admin/recommended-questions/{id}"
      - "GET /api/recommended-questions"
    consumes_apis: []
    dependencies: ["6.1"]
```

---

## 7. Checklist Results Report

> 待文档确认后执行PM Checklist验证。

---

## 8. Next Steps

### 8.1 UX Expert Prompt

本项目包含完整的前端界面需求（用户端对话界面+运营后台），请基于本PRD创建前端设计规范文档（Front-End Spec），重点关注：对话界面交互、原文预览跳转、角色化侧边栏、运营后台布局。

### 8.2 Architect Prompt

请基于本PRD创建系统架构文档（Architecture），重点关注：RAG检索引擎设计、文档解析管道、脱敏管道、模型API集成方案、数据库Schema设计（PostgreSQL+pgvector）、Redis会话管理、API接口规范。
