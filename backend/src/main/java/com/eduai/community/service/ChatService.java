package com.eduai.community.service;

import com.eduai.community.model.entity.Conversation;
import com.eduai.community.model.entity.Message;
import com.eduai.community.repository.ConversationRepository;
import com.eduai.community.repository.MessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final QwenService qwenService;
    private final com.eduai.community.repository.UserRepository userRepository;

    private static final List<Map<String, Object>> QUICK_QUESTIONS = List.of(
            Map.of("id", 1, "text", "校共体的基本组织架构是什么？"),
            Map.of("id", 2, "text", "如何开展城乡学校结对帮扶？"),
            Map.of("id", 3, "text", "校共体教师交流轮岗政策有哪些？"),
            Map.of("id", 4, "text", "如何评估校共体建设成效？"),
            Map.of("id", 5, "text", "校共体数字化教学资源如何共享？"),
            Map.of("id", 6, "text", "校共体经费管理有什么规定？")
    );

    private record MockQA(List<String> keywords, String answer, List<Map<String, Object>> citations) {}

    private static final List<MockQA> MOCK_QA_PAIRS = List.of(
            new MockQA(
                    List.of("组织架构", "架构", "组织", "结构", "基本"),
                    "根据相关政策文件，校共体（学校共同体）的基本组织架构包括以下几个层面[1]：\n\n" +
                    "1. **核心校（牵头校）**：通常由城区优质学校担任，负责统筹协调共同体内各项工作，制定发展规划和年度计划[1]。\n\n" +
                    "2. **成员校**：包括城区普通学校和乡村学校，按照共同体章程参与各项活动，共享教育资源[2]。\n\n" +
                    "3. **管理委员会**：由核心校校长担任主任，各成员校校长担任委员，负责重大事项决策[1]。\n\n" +
                    "4. **专项工作组**：设立教学研究、教师发展、学生活动、资源共享等专项工作组，由各校骨干教师组成[3]。\n\n" +
                    "5. **区域教育行政部门**：负责宏观指导、政策支持和绩效考核，确保校共体建设有序推进[2]。\n\n" +
                    "校共体实行\"核心校引领、成员校协同、行政部门监管\"的三位一体管理模式。",
                    List.of(
                            Map.of("index", 1, "source", "《关于深入推进义务教育学校共同体建设的指导意见》", "section", "第三章 组织架构", "page", 12),
                            Map.of("index", 2, "source", "《城乡义务教育一体化发展实施方案》", "section", "第二章 组织形式", "page", 8),
                            Map.of("index", 3, "source", "《学校共同体建设工作手册》", "section", "第四章 工作机制", "page", 25)
                    )
            ),
            new MockQA(
                    List.of("结对", "帮扶", "城乡", "结对帮扶"),
                    "城乡学校结对帮扶是校共体建设的重要内容，具体开展方式如下[1]：\n\n" +
                    "1. **精准配对**：根据学校特点和需求，采取\"一对一\"或\"一对多\"的方式，将城区优质学校与乡村薄弱学校进行结对[1]。\n\n" +
                    "2. **教师互派**：城区学校每年派出骨干教师到乡村学校支教，乡村学校教师到城区学校跟岗学习，每期不少于一个学期[2]。\n\n" +
                    "3. **同步课堂**：利用信息技术建立远程同步课堂，实现城乡学生同上一节课，每周不少于2节[3]。\n\n" +
                    "4. **联合教研**：每月组织至少一次联合教研活动，开展集体备课、课堂观摩、课题研究等[2]。\n\n" +
                    "5. **资源共享**：建立共享资源库，包括教案、课件、试题、微课等，城乡学校均可使用[3]。\n\n" +
                    "6. **考核评估**：将帮扶成效纳入城区学校年度考核，乡村学校学业水平提升情况作为重要指标[1]。",
                    List.of(
                            Map.of("index", 1, "source", "《城乡学校结对帮扶工作实施办法》", "section", "第二章 帮扶方式", "page", 5),
                            Map.of("index", 2, "source", "《义务教育教师交流轮岗实施细则》", "section", "第三章 交流形式", "page", 15),
                            Map.of("index", 3, "source", "《教育数字化赋能城乡均衡发展行动计划》", "section", "第四章 同步课堂", "page", 22)
                    )
            ),
            new MockQA(
                    List.of("教师", "交流", "轮岗", "政策"),
                    "校共体教师交流轮岗政策主要包括以下内容[1]：\n\n" +
                    "1. **交流对象**：在同一学校连续任教满6年的教师，原则上应进行交流轮岗。校级领导在同一学校连续任职满2届的应进行交流[1]。\n\n" +
                    "2. **交流比例**：每年教师交流轮岗的比例不低于符合条件教师总数的10%，其中骨干教师交流比例不低于交流总数的20%[2]。\n\n" +
                    "3. **待遇保障**：到乡村学校交流的教师享受乡村教师生活补助，交通补贴每月不低于500元，并在职称评审中给予倾斜[2]。\n\n" +
                    "4. **考核激励**：交流轮岗经历作为职称评审、评优评先的必要条件。表现优秀者在岗位晋升中优先考虑[3]。\n\n" +
                    "5. **管理方式**：交流教师人事关系保留在原学校，由接收学校进行日常管理和考核[1]。\n\n" +
                    "这些政策旨在促进优质教师资源的均衡配置，缩小城乡教育差距。",
                    List.of(
                            Map.of("index", 1, "source", "《义务教育教师交流轮岗实施细则》", "section", "第二章 交流范围与对象", "page", 3),
                            Map.of("index", 2, "source", "《关于进一步加强乡村教师队伍建设的意见》", "section", "第五章 待遇保障", "page", 18),
                            Map.of("index", 3, "source", "《教师绩效考核与激励办法》", "section", "第三章 考核指标", "page", 10)
                    )
            ),
            new MockQA(
                    List.of("评估", "成效", "考核", "评价", "指标"),
                    "校共体建设成效评估主要从以下维度进行[1]：\n\n" +
                    "1. **教育质量指标**：成员校学业水平合格率提升幅度、优秀率变化、学科均衡度等[1]。\n\n" +
                    "2. **教师发展指标**：教师交流轮岗完成率、教师培训覆盖率、骨干教师成长数量[2]。\n\n" +
                    "3. **资源共享指标**：共享课程资源数量、同步课堂开设频次、联合教研活动次数[2]。\n\n" +
                    "4. **管理运行指标**：管理制度完善程度、活动计划执行率、经费使用合规性[3]。\n\n" +
                    "5. **满意度指标**：教师满意度、学生满意度、家长满意度，通过问卷调查采集[3]。\n\n" +
                    "6. **创新发展指标**：特色项目建设、教育科研成果、典型经验推广等[1]。\n\n" +
                    "评估采取年度自评和第三方评估相结合的方式，结果纳入区域教育督导考核体系。",
                    List.of(
                            Map.of("index", 1, "source", "《学校共同体建设成效评估指标体系》", "section", "第二章 评估维度", "page", 7),
                            Map.of("index", 2, "source", "《关于深入推进义务教育学校共同体建设的指导意见》", "section", "第六章 考核评价", "page", 30),
                            Map.of("index", 3, "source", "《教育督导评估实施办法》", "section", "第四章 督导内容", "page", 14)
                    )
            ),
            new MockQA(
                    List.of("数字化", "资源", "共享", "信息化", "平台", "技术"),
                    "校共体数字化教学资源共享机制包括以下方面[1]：\n\n" +
                    "1. **统一资源平台**：建设校共体数字资源共享平台，实现教案、课件、微课、试题库等资源的统一上传、分类管理和便捷检索[1]。\n\n" +
                    "2. **同步课堂系统**：部署双向互动的同步课堂设备，支持城乡学校实时同步教学，覆盖主要学科[2]。\n\n" +
                    "3. **在线教研平台**：搭建网络教研空间，支持视频会议、在线研讨、协同备课等功能，降低跨校教研的时间和空间成本[2]。\n\n" +
                    "4. **教师研修系统**：建设在线研修课程库，教师可根据需要自主选学，学习记录纳入继续教育学分管理[3]。\n\n" +
                    "5. **数据分析系统**：利用大数据技术分析学生学情和教师教情，为精准教学和资源配置提供数据支撑[3]。\n\n" +
                    "各校应指定专人负责资源平台管理，核心校每学期上传优质资源不少于50件，成员校不少于20件。",
                    List.of(
                            Map.of("index", 1, "source", "《教育数字化赋能城乡均衡发展行动计划》", "section", "第三章 平台建设", "page", 15),
                            Map.of("index", 2, "source", "《智慧教育示范区建设方案》", "section", "第二章 基础设施", "page", 9),
                            Map.of("index", 3, "source", "《教师信息技术能力提升工程2.0实施方案》", "section", "第四章 应用场景", "page", 20)
                    )
            ),
            new MockQA(
                    List.of("经费", "管理", "资金", "财务", "预算"),
                    "校共体经费管理主要遵循以下规定[1]：\n\n" +
                    "1. **经费来源**：校共体建设经费由区域教育财政专项拨款、成员校配套资金和社会捐赠等多渠道组成。区级财政每年安排专项资金不少于50万元[1]。\n\n" +
                    "2. **预算管理**：校共体应编制年度经费预算，经管理委员会审批后执行。预算调整超过10%的需重新审批[2]。\n\n" +
                    "3. **使用范围**：经费主要用于教师交流差旅、联合教研活动、资源建设、设备购置、培训研修和教育科研等方面[1]。\n\n" +
                    "4. **审批流程**：单笔支出5000元以下由核心校校长审批，5000-20000元由管理委员会审批，20000元以上需报上级教育行政部门备案[2]。\n\n" +
                    "5. **财务公开**：每学期向全体成员校公开经费收支情况，接受审计监督[3]。\n\n" +
                    "6. **绩效评估**：将经费使用效益纳入年度考核，确保资金使用的规范性和有效性[3]。",
                    List.of(
                            Map.of("index", 1, "source", "《学校共同体建设经费管理办法》", "section", "第二章 经费来源", "page", 4),
                            Map.of("index", 2, "source", "《义务教育经费使用管理规定》", "section", "第三章 审批流程", "page", 11),
                            Map.of("index", 3, "source", "《教育经费绩效评估实施细则》", "section", "第二章 评估标准", "page", 6)
                    )
            )
    );

    private static final String DEFAULT_ANSWER = "感谢您的提问！关于您咨询的问题，根据校共体相关政策文件[1]：\n\n" +
            "校共体（学校共同体）是推进义务教育优质均衡发展的重要举措，旨在通过城乡学校结对、资源共享、教师交流等方式，" +
            "缩小城乡教育差距，促进教育公平[1]。\n\n" +
            "建议您可以从以下几个方面了解更多信息：\n" +
            "- 校共体的组织架构与管理模式\n" +
            "- 教师交流轮岗政策\n" +
            "- 数字化资源共享机制\n" +
            "- 建设成效评估体系\n\n" +
            "您可以进一步提出具体问题，我将为您提供更详细的政策解读[2]。";

    private static final List<Map<String, Object>> DEFAULT_CITATIONS = List.of(
            Map.of("index", 1, "source", "《关于深入推进义务教育学校共同体建设的指导意见》", "section", "总则", "page", 1),
            Map.of("index", 2, "source", "《城乡义务教育一体化发展实施方案》", "section", "第一章 总体要求", "page", 3)
    );

    public List<Map<String, Object>> getQuickQuestions() {
        return QUICK_QUESTIONS;
    }

    @Transactional
    public Map<String, Object> chat(Long userId, Long conversationId, String userMessage) {
        Conversation conversation;
        if (conversationId == null) {
            String title = userMessage.length() > 30 ? userMessage.substring(0, 30) + "..." : userMessage;
            conversation = Conversation.builder()
                    .userId(userId)
                    .title(title)
                    .build();
            conversation = conversationRepository.save(conversation);
        } else {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("对话不存在"));
            if (!conversation.getUserId().equals(userId)) {
                throw new IllegalArgumentException("无权访问该对话");
            }
            conversation.setUpdatedAt(null);
            conversationRepository.save(conversation);
        }

        Message userMsg = Message.builder()
                .conversationId(conversation.getId())
                .role(Message.Role.USER.name())
                .content(userMessage)
                .build();
        messageRepository.save(userMsg);

        String answerContent;
        String citationsJson;

        // 优先调用本地部署的 Qwen3 模型
        if (qwenService.isEnabled()) {
            answerContent = callQwen3(userId, conversation.getId(), userMessage);
            // Qwen3 回复中已包含引用，暂不单独提取 citations 结构化数据
            citationsJson = "[]";

            // 如果 Qwen3 调用失败，降级到 Mock 响应
            if (answerContent == null) {
                log.warn("Qwen3 调用失败，降级到 Mock 响应");
                MockQA matchedQA = findBestMatch(userMessage);
                answerContent = matchedQA != null ? matchedQA.answer() : DEFAULT_ANSWER;
                try {
                    citationsJson = objectMapper.writeValueAsString(
                            matchedQA != null ? matchedQA.citations() : DEFAULT_CITATIONS);
                } catch (JsonProcessingException e) {
                    citationsJson = "[]";
                }
            }
        } else {
            // Qwen3 未启用，使用 Mock 响应
            MockQA matchedQA = findBestMatch(userMessage);
            answerContent = matchedQA != null ? matchedQA.answer() : DEFAULT_ANSWER;
            try {
                citationsJson = objectMapper.writeValueAsString(
                        matchedQA != null ? matchedQA.citations() : DEFAULT_CITATIONS);
            } catch (JsonProcessingException e) {
                citationsJson = "[]";
            }
        }

        Message assistantMsg = Message.builder()
                .conversationId(conversation.getId())
                .role(Message.Role.ASSISTANT.name())
                .content(answerContent)
                .citations(citationsJson)
                .build();
        assistantMsg = messageRepository.save(assistantMsg);

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversation.getId());
        result.put("messageId", assistantMsg.getId());
        result.put("content", assistantMsg.getContent());
        try {
            result.put("citations", objectMapper.readValue(citationsJson, List.class));
        } catch (JsonProcessingException e) {
            result.put("citations", List.of());
        }

        return result;
    }

    /**
     * 调用本地 Qwen3 模型生成回复
     */
    private String callQwen3(Long userId, Long conversationId, String userMessage) {
        // 获取用户角色
        String userRole = userRepository.findById(userId)
                .map(user -> user.getRole())
                .orElse("OTHER");

        // 构建系统提示词
        String systemPrompt = qwenService.buildSystemPrompt(userRole);

        // 获取最近5轮对话历史作为上下文
        List<Message> recentMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<Map<String, String>> chatHistory = new ArrayList<>();

        int startIdx = Math.max(0, recentMessages.size() - 10); // 最近5轮 = 10条消息
        for (int i = startIdx; i < recentMessages.size(); i++) {
            Message msg = recentMessages.get(i);
            chatHistory.add(Map.of(
                    "role", msg.getRole().equalsIgnoreCase("USER") ? "user" : "assistant",
                    "content", msg.getContent()
            ));
        }

        // 添加当前用户消息
        chatHistory.add(Map.of("role", "user", "content", userMessage));

        return qwenService.chatCompletion(systemPrompt, chatHistory);
    }

    private MockQA findBestMatch(String userMessage) {
        MockQA bestMatch = null;
        int bestScore = 0;

        for (MockQA qa : MOCK_QA_PAIRS) {
            int score = 0;
            for (String keyword : qa.keywords()) {
                if (userMessage.contains(keyword)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestMatch = qa;
            }
        }

        return bestScore > 0 ? bestMatch : null;
    }

    public List<Map<String, Object>> listConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Conversation conv : conversations) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", conv.getId());
            item.put("title", conv.getTitle());
            item.put("createdAt", conv.getCreatedAt());
            item.put("updatedAt", conv.getUpdatedAt());
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getMessages(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("对话不存在"));
        if (!conversation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该对话");
        }

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", msg.getId());
            item.put("role", msg.getRole());
            item.put("content", msg.getContent());
            if (msg.getCitations() != null) {
                try {
                    item.put("citations", objectMapper.readValue(msg.getCitations(), List.class));
                } catch (JsonProcessingException e) {
                    item.put("citations", List.of());
                }
            }
            item.put("createdAt", msg.getCreatedAt());
            result.add(item);
        }
        return result;
    }
}
