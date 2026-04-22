# Epic 2: 知识库与文档处理引擎

**Epic Summary:** 构建文档解析管道（支持PDF/Word含表格和流程图）、文本切分与向量化入库、混合检索引擎（向量+关键词+Qwen3-Reranker重排序）、原文定位索引，以及校共体网站平台数据实时同步与向量化入库，为RAG问答提供高质量的知识检索基础。

**Target Repositories:** monolith

```yaml
epic_id: 2
title: "知识库与文档处理引擎"
description: |
  实现完整的文档处理管道和检索引擎：支持PDF/Word文件的结构化解析（含表格和流程图），
  按章-条-款语义切分，向量化后入库PostgreSQL+pgvector。构建混合检索引擎（向量语义检索
  +关键词检索+Qwen3-Reranker重排序），并建立片段→原文位置映射索引，支持原文跳转。
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
            - "切分长度控制在300-500 token范围内（适配Qwen3-32B 5500上下文），过长时递归拆分"
        business_rules:
          - id: "BR-1.1"
            rule: "优先按文档结构（标题层级）切分，次选按段落切分"
          - id: "BR-1.2"
            rule: "切分片段之间保留50 token重叠，确保上下文连贯"
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
            - "同时建立关键词倒排索引（zhparser tsvector，用于混合检索）"
        business_rules:
          - id: "BR-2.1"
            rule: "向量维度4096（Qwen3-Embedding-8B已确认）"
          - id: "BR-2.2"
            rule: "入库后建立HNSW索引加速向量检索，GIN索引加速全文检索"
        error_handling:
          - scenario: "Embedding API调用失败"
            code: "N/A"
            message: "N/A"
            action: "重试3次，仍失败则标记该片段为pending，记录日志"

    provides_apis: []
    consumes_apis: []
    dependencies: ["2.1"]

  - id: "2.3"
    title: "混合检索引擎与Rerank重排序"
    repository_type: monolith
    estimated_complexity: high
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "混合检索 + Rerank"
        scenario:
          given: "知识库已有向量化和全文索引的文档片段"
          when: "接收到用户查询"
          then:
            - "同时执行向量语义检索（pgvector Top-20）和关键词检索（zhparser Top-20）"
            - "合并两路检索结果（RRF加权融合）"
            - "按用户角色过滤可见性"
            - "调用Qwen3-Reranker-8B（POST /v1/score）对候选逐条评分"
            - "按Rerank分数降序取Top-3最相关的文档片段（适配5500 token上下文）"
        business_rules:
          - id: "BR-1.1"
            rule: "向量检索Top-20 + 关键词检索Top-20，RRF融合后Rerank精排取Top-3"
          - id: "BR-1.2"
            rule: "检索结果必须包含：片段文本、文件名、章节标题、页码、Rerank分数"
          - id: "BR-1.3"
            rule: "检索+Rerank总延迟控制在3秒内"
        error_handling:
          - scenario: "向量检索失败"
            code: "N/A"
            message: "N/A"
            action: "降级为纯关键词检索+Rerank，记录日志"
          - scenario: "Rerank API调用失败"
            code: "N/A"
            message: "N/A"
            action: "降级使用RRF融合分数排序取Top-3，记录日志"

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
