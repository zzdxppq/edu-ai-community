# Epic 3: 核心对话与政策问答

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
            - "调用Qwen3-32B（上下文限制5500 tokens），将检索到的Top-3片段作为上下文生成回答"
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
            - "传入LLM的上下文窗口保持最近3轮对话历史（适配5500 token限制）"
            - "对话历史缓存在Redis中"
        business_rules:
          - id: "BR-1.1"
            rule: "传入LLM的上下文窗口为最近3轮（6条消息：3条用户+3条助手），Token≤1500，适配Qwen3-32B 5500上下文"
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
