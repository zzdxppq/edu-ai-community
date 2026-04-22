# Epic 4: 角色化服务与智能推荐

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
