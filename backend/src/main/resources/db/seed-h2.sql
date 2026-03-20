MERGE INTO sys_user (id, username, password_hash, display_name, role, first_login_reset_required)
KEY (id)
VALUES
(1, 'admin', 'admin', '系统管理员', 'ADMIN', FALSE),
(2, 't001', 't001', '王老师', 'TEACHER', FALSE),
(3, 's001', 's001', '张同学', 'STUDENT', TRUE),
(4, 's002', 's002', '李同学', 'STUDENT', FALSE),
(5, 's003', 's003', '陈同学', 'STUDENT', FALSE),
(6, 's004', 's004', '赵同学', 'STUDENT', FALSE),
(7, 't002', 't002', '刘老师', 'TEACHER', FALSE),
(8, 't003', 't003', '周老师', 'TEACHER', FALSE),
(9, 's005', 's005', '吴同学', 'STUDENT', FALSE),
(10, 's006', 's006', '郑同学', 'STUDENT', FALSE),
(11, 's007', 's007', '孙同学', 'STUDENT', FALSE),
(12, 's008', 's008', '钱同学', 'STUDENT', FALSE),
(13, 's009', 's009', '董同学', 'STUDENT', FALSE),
(14, 's010', 's010', '何同学', 'STUDENT', FALSE);

MERGE INTO course (id, code, name, term)
KEY (id)
VALUES
(101, 'SE2026', '软件工程课程设计', '2026 春'),
(102, 'WEB2026', 'Web 应用开发', '2026 春'),
(103, 'DV2026', '数据可视化专题', '2026 春');

ALTER TABLE sys_user ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE course ALTER COLUMN id RESTART WITH 1000;
