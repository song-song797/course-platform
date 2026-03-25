INSERT IGNORE INTO sys_user (id, username, password_hash, display_name, role, first_login_reset_required) VALUES
(1, 'admin', 'admin', '系统管理员', 'ADMIN', 0),
(2, 't001', 't001', '王老师', 'TEACHER', 0),
(7, 't002', 't002', '刘老师', 'TEACHER', 0),
(8, 't003', 't003', '周老师', 'TEACHER', 0),
(3, 's001', 's001', '张同学', 'STUDENT', 1),
(4, 's002', 's002', '李同学', 'STUDENT', 0),
(5, 's003', 's003', '陈同学', 'STUDENT', 0),
(6, 's004', 's004', '赵同学', 'STUDENT', 0),
(9, 's005', 's005', '吴同学', 'STUDENT', 0),
(10, 's006', 's006', '郑同学', 'STUDENT', 0),
(11, 's007', 's007', '孙同学', 'STUDENT', 0),
(12, 's008', 's008', '钱同学', 'STUDENT', 0),
(13, 's009', 's009', '蒋同学', 'STUDENT', 0),
(14, 's010', 's010', '何同学', 'STUDENT', 0);

INSERT IGNORE INTO course (id, code, name, term) VALUES
(101, 'SE2026', '软件工程课程设计', '2026 春'),
(102, 'WEB2026', 'Web 应用开发', '2026 春'),
(103, 'DV2026', '数据可视化专题', '2026 春');

INSERT IGNORE INTO course_member (id, course_id, user_id, course_role) VALUES
(1001, 101, 2, 'TEACHER'),
(1006, 101, 7, 'TEACHER'),
(1002, 101, 3, 'STUDENT'),
(1003, 101, 4, 'STUDENT'),
(1004, 101, 5, 'STUDENT'),
(1005, 101, 6, 'STUDENT'),
(1007, 101, 9, 'STUDENT'),
(1008, 101, 10, 'STUDENT'),
(1101, 102, 2, 'TEACHER'),
(1102, 102, 7, 'TEACHER'),
(1103, 102, 3, 'STUDENT'),
(1104, 102, 4, 'STUDENT'),
(1105, 102, 9, 'STUDENT'),
(1106, 102, 10, 'STUDENT'),
(1107, 102, 11, 'STUDENT'),
(1201, 103, 8, 'TEACHER'),
(1202, 103, 5, 'STUDENT'),
(1203, 103, 6, 'STUDENT'),
(1204, 103, 12, 'STUDENT'),
(1205, 103, 13, 'STUDENT'),
(1206, 103, 14, 'STUDENT');

INSERT IGNORE INTO assignment (
    id, course_id, title, mode, description, deadline, allow_late, peer_weight, teacher_weight, status,
    results_published, results_published_at
) VALUES
(1001, 101, '课程项目 Demo', 'GROUP', '支持项目提交、开放互评与 Rubric 评分的课程项目', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 HOUR), 0, 40, 60, 'REVIEWING', 0, NULL),
(1002, 101, '个人展示页', 'INDIVIDUAL', '个人作业模式预留', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 5 DAY), 1, 30, 70, 'SUBMITTING', 0, NULL),
(1003, 102, '企业协作平台', 'GROUP', '小组协作平台开发与开放互评', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 HOUR), 0, 50, 50, 'REVIEWING', 0, NULL),
(1004, 102, '交互作品集', 'INDIVIDUAL', '个人交互作品展示页', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 9 DAY), 1, 30, 70, 'SUBMITTING', 0, NULL),
(1005, 103, '可视化数据故事', 'GROUP', '围绕真实数据集完成可视化叙事', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 54 HOUR), 0, 40, 60, 'CLOSED', 0, NULL);

INSERT IGNORE INTO rubric (id, assignment_id, version_no, is_active) VALUES
(4001, 1001, 1, 1),
(4002, 1002, 1, 1),
(4003, 1003, 1, 1),
(4004, 1004, 1, 1),
(4005, 1005, 1, 1);

INSERT IGNORE INTO rubric_item (id, rubric_id, item_name, description, weight) VALUES
(2001, 4001, '完成度', '功能是否完整', 30),
(2002, 4001, '工程质量', '代码结构与协作规范', 25),
(2003, 4001, '创新性', '设计亮点与创意', 20),
(2004, 4001, '展示表达', '说明文档和演示效果', 25),
(2101, 4002, '完成度', '功能是否完整', 35),
(2102, 4002, '工程质量', '代码结构与协作规范', 30),
(2103, 4002, '创新性', '设计亮点与创意', 15),
(2104, 4002, '展示表达', '说明文档和演示效果', 20),
(2201, 4003, '业务完成度', '是否覆盖题目核心场景', 30),
(2202, 4003, '协作质量', '分工与协作表现', 20),
(2203, 4003, '工程质量', '架构、规范与稳定性', 30),
(2204, 4003, '展示表达', '答辩材料与演示效果', 20),
(2301, 4004, '视觉完成度', '页面完整性与细节', 30),
(2302, 4004, '交互设计', '交互体验与可用性', 25),
(2303, 4004, '代码质量', '工程结构与可维护性', 25),
(2304, 4004, '表达说明', '文档与讲解', 20),
(2401, 4005, '故事表达', '叙事完整性与洞察', 30),
(2402, 4005, '图表设计', '可视化表达质量', 25),
(2403, 4005, '数据处理', '数据清洗与分析质量', 25),
(2404, 4005, '工程实现', '交付质量与稳定性', 20);

INSERT IGNORE INTO assignment_group (id, assignment_id, group_name) VALUES
(3001, 1001, 'Campus Pair'),
(3002, 1001, 'Sprint Board'),
(3003, 1001, 'Studio Review'),
(3004, 1003, 'Flow Forge'),
(3005, 1003, 'Task Bridge'),
(3006, 1003, 'Pixel Crew'),
(3007, 1005, 'Chart Narrative'),
(3008, 1005, 'Insight Lab');

INSERT IGNORE INTO assignment_group_member (id, assignment_id, group_id, user_id) VALUES
(5001, 1001, 3001, 3),
(5002, 1001, 3001, 4),
(5003, 1001, 3002, 5),
(5004, 1001, 3003, 6),
(5005, 1003, 3004, 3),
(5006, 1003, 3004, 9),
(5007, 1003, 3005, 4),
(5008, 1003, 3005, 10),
(5009, 1003, 3006, 11),
(5010, 1005, 3007, 5),
(5011, 1005, 3007, 12),
(5012, 1005, 3008, 6),
(5013, 1005, 3008, 13),
(5014, 1005, 3008, 14);

INSERT IGNORE INTO submission (id, assignment_id, group_id, project_name, repo_url, video_url, preview_url, doc_url, attachment_url, description, submitted_by, submitted_at, is_late) VALUES
(5001, 1001, 3001, 'Campus Pair', 'https://github.com/demo/campus-pair', 'https://www.bilibili.com/video/BV1xx411c7mD', 'https://example.com/campus-pair', 'https://example.com/docs/campus-pair', 'https://example.com/files/campus-pair.pdf', '校园互助结对项目', 3, '2026-03-15 18:00:00', 0),
(5002, 1001, 3002, 'Sprint Board', 'https://github.com/demo/sprint-board', 'https://www.bilibili.com/video/BV1xx411c7mD', 'https://example.com/sprint-board', 'https://example.com/docs/sprint-board', 'https://example.com/files/sprint-board.pdf', '敏捷任务板', 5, '2026-03-15 18:30:00', 0),
(5003, 1001, 3003, 'Studio Review', 'https://github.com/demo/studio-review', 'https://www.bilibili.com/video/BV1xx411c7mD', 'https://example.com/studio-review', 'https://example.com/docs/studio-review', 'https://example.com/files/studio-review.pdf', '开放互评实验室', 6, '2026-03-15 19:00:00', 0),
(5004, 1003, 3004, 'Flow Forge', 'https://github.com/demo/flow-forge', 'https://www.bilibili.com/video/BV1xx411c7mD', 'https://example.com/flow-forge', 'https://example.com/docs/flow-forge', 'https://example.com/files/flow-forge.pdf', '企业流程协作平台', 3, '2026-03-16 10:30:00', 0),
(5005, 1003, 3005, 'Task Bridge', 'https://github.com/demo/task-bridge', 'https://www.bilibili.com/video/BV1xx411c7mD', 'https://example.com/task-bridge', 'https://example.com/docs/task-bridge', 'https://example.com/files/task-bridge.pdf', '跨团队任务协同平台', 4, '2026-03-16 11:10:00', 0),
(5006, 1003, 3006, 'Pixel Crew', 'https://github.com/demo/pixel-crew', 'https://www.bilibili.com/video/BV1xx411c7mD', 'https://example.com/pixel-crew', 'https://example.com/docs/pixel-crew', 'https://example.com/files/pixel-crew.pdf', '视觉化协作工作台', 11, '2026-03-16 11:45:00', 1),
(5007, 1005, 3007, 'Chart Narrative', 'https://github.com/demo/chart-narrative', 'https://www.bilibili.com/video/BV1xx411c7mD', 'https://example.com/chart-narrative', 'https://example.com/docs/chart-narrative', 'https://example.com/files/chart-narrative.pdf', '面向教学数据的可视化叙事项目', 5, '2026-03-14 20:20:00', 0),
(5008, 1005, 3008, 'Insight Lab', 'https://github.com/demo/insight-lab', 'https://www.bilibili.com/video/BV1xx411c7mD', 'https://example.com/insight-lab', 'https://example.com/docs/insight-lab', 'https://example.com/files/insight-lab.pdf', '数据洞察与图表实验室', 6, '2026-03-14 21:00:00', 0);

INSERT IGNORE INTO evaluation (id, assignment_id, submission_id, evaluator_user_id, evaluator_role, total_score, comment, is_abnormal, abnormal_reason, is_excluded, review_status, created_at) VALUES
(8001, 1001, 5001, 2, 'TEACHER', 88.00, '整体结构比较完整。', 0, NULL, 0, 'PENDING', '2026-03-16 10:00:00'),
(8002, 1001, 5001, 5, 'STUDENT', 84.00, '展示很清晰。', 0, NULL, 0, 'PENDING', '2026-03-16 12:00:00'),
(8003, 1001, 5002, 2, 'TEACHER', 92.00, '功能完整，节奏控制很好。', 0, NULL, 0, 'PENDING', '2026-03-16 12:10:00'),
(8004, 1001, 5002, 7, 'TEACHER', 90.00, '小组分工比较清晰。', 0, NULL, 0, 'PENDING', '2026-03-16 12:12:00'),
(8005, 1001, 5002, 3, 'STUDENT', 90.00, '交互细节做得不错。', 0, NULL, 0, 'PENDING', '2026-03-16 13:00:00'),
(8006, 1001, 5002, 4, 'STUDENT', 91.00, '功能覆盖比较全。', 0, NULL, 0, 'PENDING', '2026-03-16 13:02:00'),
(8007, 1001, 5002, 6, 'STUDENT', 74.00, '展示略显仓促。', 1, '与其他学生评分均值偏差 16.5 分', 0, 'PENDING', '2026-03-16 13:05:00'),
(8008, 1001, 5002, 9, 'STUDENT', 88.00, '文档整理比较到位。', 0, NULL, 0, 'PENDING', '2026-03-16 13:08:00'),
(8009, 1001, 5003, 2, 'TEACHER', 86.00, '整体完成度尚可。', 0, NULL, 0, 'PENDING', '2026-03-16 13:10:00'),
(8010, 1001, 5003, 3, 'STUDENT', 82.00, '亮点不错，但节奏一般。', 0, NULL, 0, 'PENDING', '2026-03-16 13:12:00'),
(8011, 1001, 5003, 4, 'STUDENT', 85.00, '工程结构比较整齐。', 0, NULL, 0, 'PENDING', '2026-03-16 13:15:00'),
(8012, 1001, 5003, 5, 'STUDENT', 80.00, '文档可以再打磨。', 0, NULL, 0, 'PENDING', '2026-03-16 13:18:00'),
(8013, 1001, 5003, 9, 'STUDENT', 99.00, '展示效果特别好。', 1, '与其他学生评分均值偏差 16.67 分', 0, 'PENDING', '2026-03-16 13:21:00'),
(8014, 1001, 5001, 6, 'STUDENT', 79.00, '完成度可以再提升。', 0, NULL, 0, 'PENDING', '2026-03-16 13:25:00'),
(8015, 1001, 5001, 9, 'STUDENT', 95.00, '整体表现非常稳定。', 0, NULL, 0, 'PENDING', '2026-03-16 13:28:00'),
(8016, 1001, 5001, 7, 'TEACHER', 87.00, '细节把控还不错。', 0, NULL, 0, 'PENDING', '2026-03-16 13:32:00'),
(8020, 1003, 5004, 2, 'TEACHER', 90.00, '整体协作节奏流畅。', 0, NULL, 0, 'PENDING', '2026-03-16 14:00:00'),
(8021, 1003, 5005, 7, 'TEACHER', 88.00, '方案比较完整。', 0, NULL, 0, 'PENDING', '2026-03-16 14:05:00'),
(8022, 1003, 5005, 3, 'STUDENT', 86.00, '演示逻辑很顺。', 0, NULL, 0, 'PENDING', '2026-03-16 14:08:00'),
(8023, 1003, 5005, 9, 'STUDENT', 84.00, '工程结构比较干净。', 0, NULL, 0, 'PENDING', '2026-03-16 14:10:00'),
(8024, 1003, 5005, 11, 'STUDENT', 89.00, '整体表现均衡。', 0, NULL, 0, 'PENDING', '2026-03-16 14:12:00'),
(8025, 1003, 5004, 10, 'STUDENT', 87.00, '业务流程设计合理。', 0, NULL, 0, 'PENDING', '2026-03-16 14:15:00'),
(8030, 1005, 5007, 8, 'TEACHER', 91.00, '可视化叙事完成度高。', 0, NULL, 0, 'PENDING', '2026-03-16 14:30:00'),
(8031, 1005, 5008, 8, 'TEACHER', 85.00, '图表表现较稳。', 0, NULL, 0, 'PENDING', '2026-03-16 14:35:00'),
(8032, 1005, 5007, 6, 'STUDENT', 88.00, '故事线组织得很好。', 0, NULL, 0, 'PENDING', '2026-03-16 14:38:00'),
(8033, 1005, 5007, 13, 'STUDENT', 86.00, '图表和文字搭配不错。', 0, NULL, 0, 'PENDING', '2026-03-16 14:41:00'),
(8034, 1005, 5007, 14, 'STUDENT', 84.00, '展示很完整。', 0, NULL, 0, 'PENDING', '2026-03-16 14:44:00'),
(8035, 1005, 5008, 5, 'STUDENT', 83.00, '数据处理还可以再细化。', 0, NULL, 0, 'PENDING', '2026-03-16 14:48:00'),
(8036, 1005, 5008, 12, 'STUDENT', 87.00, '整体节奏比较好。', 0, NULL, 0, 'PENDING', '2026-03-16 14:52:00');

INSERT IGNORE INTO evaluation_item (id, evaluation_id, rubric_item_id, score, comment) VALUES
(9001, 8001, 2001, 8.5, '完成度较高'),
(9002, 8001, 2002, 9.0, '工程结构清楚'),
(9003, 8001, 2003, 8.5, '亮点明确'),
(9004, 8001, 2004, 9.2, '演示表达稳定'),
(9005, 8002, 2001, 8.0, '功能完整'),
(9006, 8002, 2002, 8.5, '代码看起来比较规范'),
(9007, 8002, 2003, 8.0, '有一定创意'),
(9008, 8002, 2004, 9.1, '讲解很顺畅'),
(9009, 8003, 2001, 9.3, '覆盖全面'),
(9010, 8003, 2002, 9.2, '工程质量稳定'),
(9011, 8003, 2003, 8.8, '方案有亮点'),
(9012, 8003, 2004, 9.4, '演示很顺畅'),
(9013, 8005, 2001, 9.0, '功能很全'),
(9014, 8005, 2002, 8.9, '代码结构不错'),
(9015, 8005, 2003, 9.1, '有一定创新'),
(9016, 8005, 2004, 9.0, '表达清晰'),
(9017, 8030, 2401, 9.2, '叙事逻辑完整'),
(9018, 8030, 2402, 9.0, '图表设计成熟'),
(9019, 8030, 2403, 9.1, '分析细致'),
(9020, 8030, 2404, 9.0, '工程交付稳定');

INSERT IGNORE INTO evaluation_blacklist (id, assignment_id, evaluator_user_id, target_submission_id, created_at) VALUES
(7001, 1001, 10, 5001, '2026-03-16 15:10:00');
