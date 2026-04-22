# Epic 5: 用户反馈与知识自进化

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
