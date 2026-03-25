from __future__ import annotations

import argparse
import json
import shutil
import zipfile
from dataclasses import dataclass
from pathlib import Path

from lxml import etree
from PIL import Image, ImageDraw, ImageFont


PALETTE = {
    "background": "#ffffff",
    "title": "#222222",
    "outline": "#7f7f7f",
    "process_fill": "#eef2f6",
    "decision_fill": "#f8edbf",
    "terminal_fill": "#ddd2ef",
    "branch_label_fill": "#ffffff",
    "branch_label_text": "#555555",
    "text": "#333333",
}

WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
DRAWING_NS = "http://schemas.openxmlformats.org/drawingml/2006/main"
WP_NS = "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
PIC_NS = "http://schemas.openxmlformats.org/drawingml/2006/picture"
R_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
CONTENT_NS = "http://schemas.openxmlformats.org/package/2006/content-types"
NS = {
    "w": WORD_NS,
    "r": R_NS,
    "wp": WP_NS,
    "a": DRAWING_NS,
    "pic": PIC_NS,
    "rel": REL_NS,
    "ct": CONTENT_NS,
}
W = f"{{{WORD_NS}}}"
R = f"{{{R_NS}}}"
WP = f"{{{WP_NS}}}"
A = f"{{{DRAWING_NS}}}"
PIC = f"{{{PIC_NS}}}"
REL = f"{{{REL_NS}}}"
CT = f"{{{CONTENT_NS}}}"
EMU_PER_PIXEL = 9525
MAX_DOCX_WIDTH_EMU = 5_850_000

CANVAS_WIDTH = 1800
CANVAS_HEIGHT = 660
LEFT_MARGIN = 90
RIGHT_MARGIN = 90
TITLE_Y = 64
MAIN_Y = 310
LANE_GAP = 160
BRANCH_Y = MAIN_Y + LANE_GAP

DEFAULT_FONT_PATH = Path(r"C:\Windows\Fonts\msyh.ttc")
SAMPLE_MODULE_NAMES = (
    "邮箱验证码登录",
    "OCR拍照导入错题",
    "获取首页数据",
    "AI解析题目",
    "管理员更新用户状态",
)


@dataclass(frozen=True)
class NodeSpec:
    id: str
    text: str
    kind: str
    column: int
    lane: int = 0


@dataclass(frozen=True)
class EdgeSpec:
    source: str
    target: str
    label: str = ""
    route: str = "auto"


@dataclass(frozen=True)
class FlowSpec:
    module: str
    nodes: tuple[NodeSpec, ...]
    edges: tuple[EdgeSpec, ...]


@dataclass(frozen=True)
class Box:
    x: float
    y: float
    width: float
    height: float

    @property
    def left(self) -> float:
        return self.x

    @property
    def right(self) -> float:
        return self.x + self.width

    @property
    def top(self) -> float:
        return self.y

    @property
    def bottom(self) -> float:
        return self.y + self.height

    @property
    def cx(self) -> float:
        return self.x + self.width / 2

    @property
    def cy(self) -> float:
        return self.y + self.height / 2


@dataclass(frozen=True)
class ModuleDocEntry:
    title: str
    description: str


@dataclass(frozen=True)
class DiagramArtifact:
    spec: FlowSpec
    description: str
    image_path: Path


def flow(module: str, nodes: list[NodeSpec], edges: list[EdgeSpec]) -> FlowSpec:
    return FlowSpec(module=module, nodes=tuple(nodes), edges=tuple(edges))


def start_node() -> NodeSpec:
    return NodeSpec("start", "开始", "start", 0)


def end_node(column: int) -> NodeSpec:
    return NodeSpec("end", "结束", "end", column)


def process_node(node_id: str, text: str, column: int, lane: int = 0) -> NodeSpec:
    return NodeSpec(node_id, text, "process", column, lane)


def decision_node(node_id: str, text: str, column: int, lane: int = 0) -> NodeSpec:
    return NodeSpec(node_id, text, "decision", column, lane)


def one_check_flow(module: str, input_text: str, check_text: str, main_text: str, response_text: str, fail_text: str) -> FlowSpec:
    return flow(
        module,
        [
            start_node(),
            process_node("input", input_text, 1),
            decision_node("check", check_text, 2),
            process_node("main", main_text, 3),
            process_node("response", response_text, 4),
            end_node(5),
            process_node("fail", fail_text, 2, 1),
        ],
        [
            EdgeSpec("start", "input"),
            EdgeSpec("input", "check"),
            EdgeSpec("check", "main", "是"),
            EdgeSpec("main", "response"),
            EdgeSpec("response", "end"),
            EdgeSpec("check", "fail", "否"),
            EdgeSpec("fail", "end", route="to_end"),
        ],
    )


def admin_list_flow(module: str, input_text: str, query_text: str, response_text: str) -> FlowSpec:
    return flow(
        module,
        [
            start_node(),
            process_node("input", input_text, 1),
            decision_node("role", "当前操作者为管理员？", 2),
            process_node("query", query_text, 3),
            process_node("response", response_text, 4),
            end_node(5),
            process_node("forbidden", "返回无权限", 2, 1),
        ],
        [
            EdgeSpec("start", "input"),
            EdgeSpec("input", "role"),
            EdgeSpec("role", "query", "是"),
            EdgeSpec("query", "response"),
            EdgeSpec("response", "end"),
            EdgeSpec("role", "forbidden", "否"),
            EdgeSpec("forbidden", "end", route="to_end"),
        ],
    )


def admin_action_flow(module: str, input_text: str, target_check_text: str, action_text: str, response_text: str, fail_text: str) -> FlowSpec:
    return flow(
        module,
        [
            start_node(),
            process_node("input", input_text, 1),
            decision_node("role", "当前操作者为管理员？", 2),
            decision_node("target", target_check_text, 3),
            process_node("action", action_text, 4),
            process_node("response", response_text, 5),
            end_node(6),
            process_node("forbidden", "返回无权限", 2, 1),
            process_node("fail", fail_text, 3, 1),
        ],
        [
            EdgeSpec("start", "input"),
            EdgeSpec("input", "role"),
            EdgeSpec("role", "target", "是"),
            EdgeSpec("target", "action", "是"),
            EdgeSpec("action", "response"),
            EdgeSpec("response", "end"),
            EdgeSpec("role", "forbidden", "否"),
            EdgeSpec("target", "fail", "否"),
            EdgeSpec("forbidden", "end", route="to_end"),
            EdgeSpec("fail", "end", route="to_end"),
        ],
    )


def decision_result_flow(module: str, input_text: str, middle_text: str, decision_text: str, success_text: str, fallback_text: str) -> FlowSpec:
    return flow(
        module,
        [
            start_node(),
            process_node("input", input_text, 1),
            process_node("middle", middle_text, 2),
            decision_node("check", decision_text, 3),
            process_node("success", success_text, 4),
            end_node(5),
            process_node("fallback", fallback_text, 4, 1),
        ],
        [
            EdgeSpec("start", "input"),
            EdgeSpec("input", "middle"),
            EdgeSpec("middle", "check"),
            EdgeSpec("check", "success", "是"),
            EdgeSpec("success", "end"),
            EdgeSpec("check", "fallback", "否"),
            EdgeSpec("fallback", "end", route="to_end"),
        ],
    )


def upload_parse_flow(module: str, upload_text: str, file_check_text: str, parse_text: str, result_check_text: str, save_text: str, response_text: str, upload_fail_text: str, parse_fail_text: str) -> FlowSpec:
    return flow(
        module,
        [
            start_node(),
            process_node("upload", upload_text, 1),
            decision_node("file", file_check_text, 2),
            process_node("parse", parse_text, 3),
            decision_node("result", result_check_text, 4),
            process_node("save", save_text, 5),
            process_node("response", response_text, 6),
            end_node(7),
            process_node("upload_fail", upload_fail_text, 2, 1),
            process_node("parse_fail", parse_fail_text, 4, 1),
        ],
        [
            EdgeSpec("start", "upload"),
            EdgeSpec("upload", "file"),
            EdgeSpec("file", "parse", "是"),
            EdgeSpec("parse", "result"),
            EdgeSpec("result", "save", "是"),
            EdgeSpec("save", "response"),
            EdgeSpec("response", "end"),
            EdgeSpec("file", "upload_fail", "否"),
            EdgeSpec("result", "parse_fail", "否"),
            EdgeSpec("upload_fail", "end", route="to_end"),
            EdgeSpec("parse_fail", "end", route="to_end"),
        ],
    )


def ai_response_flow(module: str, input_text: str, check_text: str, ai_text: str, result_text: str, response_text: str, fail_text: str, ai_fail_text: str) -> FlowSpec:
    return flow(
        module,
        [
            start_node(),
            process_node("input", input_text, 1),
            decision_node("check", check_text, 2),
            process_node("ai", ai_text, 3),
            decision_node("result", result_text, 4),
            process_node("response", response_text, 5),
            end_node(6),
            process_node("fail", fail_text, 2, 1),
            process_node("ai_fail", ai_fail_text, 4, 1),
        ],
        [
            EdgeSpec("start", "input"),
            EdgeSpec("input", "check"),
            EdgeSpec("check", "ai", "是"),
            EdgeSpec("ai", "result"),
            EdgeSpec("result", "response", "是"),
            EdgeSpec("response", "end"),
            EdgeSpec("check", "fail", "否"),
            EdgeSpec("result", "ai_fail", "否"),
            EdgeSpec("fail", "end", route="to_end"),
            EdgeSpec("ai_fail", "end", route="to_end"),
        ],
    )


def choice_merge_flow(module: str, input_text: str, validate_text: str, choice_text: str, main_action_text: str, branch_action_text: str, response_text: str, fail_text: str) -> FlowSpec:
    return flow(
        module,
        [
            start_node(),
            process_node("input", input_text, 1),
            decision_node("validate", validate_text, 2),
            decision_node("choice", choice_text, 3),
            process_node("main_action", main_action_text, 4),
            process_node("response", response_text, 5),
            end_node(6),
            process_node("fail", fail_text, 2, 1),
            process_node("branch_action", branch_action_text, 4, 1),
        ],
        [
            EdgeSpec("start", "input"),
            EdgeSpec("input", "validate"),
            EdgeSpec("validate", "choice", "是"),
            EdgeSpec("choice", "main_action", "是"),
            EdgeSpec("main_action", "response"),
            EdgeSpec("response", "end"),
            EdgeSpec("validate", "fail", "否"),
            EdgeSpec("choice", "branch_action", "否"),
            EdgeSpec("fail", "end", route="to_end"),
            EdgeSpec("branch_action", "response", route="return_up"),
        ],
    )


def toggle_flow(module: str, input_text: str, exists_text: str, state_text: str, activate_text: str, deactivate_text: str, response_text: str, not_found_text: str) -> FlowSpec:
    return flow(
        module,
        [
            start_node(),
            process_node("input", input_text, 1),
            decision_node("exists", exists_text, 2),
            decision_node("state", state_text, 3),
            process_node("activate", activate_text, 4),
            process_node("response", response_text, 5),
            end_node(6),
            process_node("not_found", not_found_text, 2, 1),
            process_node("deactivate", deactivate_text, 4, 1),
        ],
        [
            EdgeSpec("start", "input"),
            EdgeSpec("input", "exists"),
            EdgeSpec("exists", "state", "是"),
            EdgeSpec("state", "activate", "否"),
            EdgeSpec("activate", "response"),
            EdgeSpec("response", "end"),
            EdgeSpec("exists", "not_found", "否"),
            EdgeSpec("state", "deactivate", "是"),
            EdgeSpec("not_found", "end", route="to_end"),
            EdgeSpec("deactivate", "response", route="return_up"),
        ],
    )


def build_all_flow_specs() -> tuple[FlowSpec, ...]:
    specs: list[FlowSpec] = []
    specs.extend(
        [
            one_check_flow("健康检查", "接收探活请求", "服务状态正常？", "生成健康状态信息", "返回健康检查结果", "返回异常状态"),
            flow(
                "发送邮箱验证码",
                [
                    start_node(),
                    process_node("input", "接收邮箱与用途", 1),
                    decision_node("param", "邮箱与用途合法？", 2),
                    decision_node("limit", "发送频率允许？", 3),
                    process_node("send", "生成验证码并发送", 4),
                    process_node("response", "返回发送结果", 5),
                    end_node(6),
                    process_node("param_fail", "返回参数错误", 2, 1),
                    process_node("limit_fail", "返回频率限制", 3, 1),
                ],
                [
                    EdgeSpec("start", "input"),
                    EdgeSpec("input", "param"),
                    EdgeSpec("param", "limit", "是"),
                    EdgeSpec("limit", "send", "是"),
                    EdgeSpec("send", "response"),
                    EdgeSpec("response", "end"),
                    EdgeSpec("param", "param_fail", "否"),
                    EdgeSpec("limit", "limit_fail", "否"),
                    EdgeSpec("param_fail", "end", route="to_end"),
                    EdgeSpec("limit_fail", "end", route="to_end"),
                ],
            ),
            flow(
                "邮箱验证码登录",
                [
                    start_node(),
                    process_node("input", "接收邮箱与验证码", 1),
                    decision_node("verify_code", "验证码有效且用途匹配？", 2),
                    decision_node("find_user", "用户是否已注册？", 3),
                    process_node("issue_token", "生成访问/刷新令牌", 4),
                    process_node("response", "返回登录结果", 5),
                    end_node(6),
                    process_node("code_error", "返回验证码错误", 2, 1),
                    process_node("need_register", "返回need_register", 3, 1),
                ],
                [
                    EdgeSpec("start", "input"),
                    EdgeSpec("input", "verify_code"),
                    EdgeSpec("verify_code", "find_user", "是"),
                    EdgeSpec("find_user", "issue_token", "是"),
                    EdgeSpec("issue_token", "response"),
                    EdgeSpec("response", "end"),
                    EdgeSpec("verify_code", "code_error", "否"),
                    EdgeSpec("find_user", "need_register", "否"),
                    EdgeSpec("code_error", "end", route="to_end"),
                    EdgeSpec("need_register", "end", route="to_end"),
                ],
            ),
            one_check_flow("刷新令牌", "接收refresh_token", "refresh_token有效？", "生成新令牌对", "返回新令牌", "返回令牌失效"),
            one_check_flow("重置密码", "接收邮箱/验证码/新密码", "验证码有效？", "更新账户密码", "返回重置结果", "返回验证码错误"),
            one_check_flow("更新个人资料", "接收资料变更内容", "存在可更新字段？", "更新用户资料", "返回最新资料", "返回参数错误"),
            one_check_flow("更新头像", "上传头像文件", "格式与大小合法？", "存储头像并更新资料", "返回头像地址", "返回文件错误"),
            one_check_flow("修改密码", "接收旧密码与新密码", "旧密码校验通过？", "更新登录密码", "返回修改结果", "返回密码错误"),
            one_check_flow("获取错题列表", "接收分页与筛选条件", "查询参数合法？", "查询错题记录", "返回错题分页", "返回参数错误"),
            one_check_flow("搜索错题", "接收搜索关键词", "关键词有效？", "执行全文检索", "返回匹配错题", "返回参数错误"),
            decision_result_flow("获取错题统计", "获取当前用户", "汇总错题统计", "统计结果生成完成？", "返回统计数据", "返回默认统计"),
            decision_result_flow("获取今日待复习列表", "获取当前用户", "计算今日复习计划", "存在待复习题？", "返回待复习列表", "返回空列表"),
            upload_parse_flow("OCR拍照导入错题", "上传题目图片", "文件格式与大小合法？", "执行OCR识别", "识别结果可用？", "生成错题记录", "返回导入结果", "返回文件错误", "返回识别失败"),
            one_check_flow("AI导入错题", "接收AI解析结果", "题目信息完整？", "保存错题记录", "返回导入结果", "返回导入失败"),
            one_check_flow("复习错题", "接收复习结果", "错题存在且可复习？", "更新复习记录与计划", "返回复习结果", "返回题目不存在"),
        ]
    )
    specs.extend(
        [
            one_check_flow("获取课程列表", "接收学期参数", "学期参数有效？", "查询课程列表", "返回课程列表", "返回参数错误"),
            one_check_flow("设置当前学期", "接收学期ID", "目标学期存在？", "更新当前学期", "返回设置结果", "返回学期不存在"),
            one_check_flow("获取周课表", "接收学期与周次", "参数合法？", "查询周课表", "返回周课表", "返回参数错误"),
            upload_parse_flow("导入ICS课表", "上传ICS文件", "文件格式合法？", "解析日历事件", "解析结果有效？", "创建课程记录", "返回导入结果", "返回文件错误", "返回解析失败"),
            one_check_flow("更新课程提醒设置", "接收提醒配置", "课程存在？", "更新提醒参数", "返回设置结果", "返回课程不存在"),
            one_check_flow("获取专注历史", "接收分页条件", "分页参数合法？", "查询专注历史", "返回历史分页", "返回参数错误"),
            one_check_flow("获取学习排行", "接收时间维度", "榜单维度合法？", "汇总学习时长排行", "返回排行榜", "返回参数错误"),
            decision_result_flow("获取签到状态", "获取当前用户", "查询签到记录", "今日已签到？", "返回已签到状态", "返回未签到状态"),
            one_check_flow("获取笔记列表", "接收分页与笔记本", "查询参数合法？", "查询笔记列表", "返回笔记分页", "返回参数错误"),
            one_check_flow("创建草稿", "接收草稿内容", "内容可保存？", "创建草稿记录", "返回草稿信息", "返回参数错误"),
            one_check_flow("上传附件", "上传附件文件", "文件类型合法？", "存储附件资源", "返回附件信息", "返回文件错误"),
            one_check_flow("添加协作者", "接收笔记与协作者", "当前用户为笔记所有者？", "添加协作者关系", "返回处理结果", "返回无权限"),
            one_check_flow("获取广场信息流", "接收分页/模式/话题", "查询参数合法？", "查询信息流", "返回帖子列表", "返回参数错误"),
            choice_merge_flow("发布帖子", "接收帖子内容", "帖子内容合法？", "直接发布？", "保存已发布帖子", "保存草稿帖子", "返回帖子结果", "返回参数错误"),
            toggle_flow("切换点赞", "接收帖子ID", "帖子存在？", "当前已点赞？", "新增点赞记录", "取消点赞记录", "返回点赞状态", "返回帖子不存在"),
        ]
    )
    specs.extend(
        [
            one_check_flow("关注用户", "接收目标用户", "目标用户可关注？", "创建关注关系", "返回关注结果", "返回关注失败"),
            choice_merge_flow("发表评论", "接收评论内容", "评论内容合法？", "是否回复评论？", "创建回复评论", "创建帖子评论", "返回评论结果", "返回参数错误"),
            one_check_flow("获取通知列表", "接收分页与状态", "查询参数合法？", "查询通知列表", "返回通知分页", "返回参数错误"),
            one_check_flow("标记已读", "接收通知ID列表", "存在可更新通知？", "批量标记已读", "返回处理结果", "返回通知不存在"),
            decision_result_flow("导出成绩单pdf", "接收学期参数", "查询成绩数据", "存在可导出成绩？", "生成并返回PDF", "返回无数据提示"),
            one_check_flow("导出选中错题pdf", "接收错题ID列表", "导出列表合法？", "生成PDF文件", "返回导出文件", "返回参数错误"),
            one_check_flow("获取关注列表", "接收分页与搜索类型", "查询参数合法？", "查询搜题记录", "返回记录列表", "返回参数错误"),
            one_check_flow("获取粉丝列表", "接收分页与关键词", "查询参数合法？", "查询粉丝列表", "返回粉丝分页", "返回参数错误"),
            decision_result_flow("获取反馈邮箱", "读取系统配置", "获取反馈邮箱配置", "反馈邮箱已配置？", "返回反馈邮箱", "返回默认联系邮箱"),
            one_check_flow("获取成绩列表", "接收学期参数", "学期参数有效？", "查询成绩列表", "返回成绩列表", "返回参数错误"),
            decision_result_flow("获取成绩仪表板", "接收学期参数", "汇总图表数据", "存在成绩记录？", "返回仪表板数据", "返回空图表数据"),
            one_check_flow("获取成绩分析", "接收分析范围", "分析参数合法？", "计算趋势与强弱项", "返回分析结果", "返回参数错误"),
            one_check_flow("获取竞赛列表", "接收分页与状态", "查询参数合法？", "查询竞赛项目", "返回竞赛分页", "返回参数错误"),
            one_check_flow("获取练习题列表", "接收竞赛ID", "目标竞赛存在？", "查询练习题库", "返回题库列表", "返回竞赛不存在"),
            one_check_flow("获取科研项目列表", "接收分页与状态", "查询参数合法？", "查询科研项目", "返回项目分页", "返回参数错误"),
        ]
    )
    specs.extend(
        [
            one_check_flow("创建科研项目", "接收项目资料", "必填信息完整？", "创建科研项目", "返回创建结果", "返回参数错误"),
            ai_response_flow("AI对话", "接收消息与上下文", "输入内容合法？", "组装上下文并调用AI", "AI回复生成成功？", "返回对话结果", "返回参数错误", "返回对话失败"),
            ai_response_flow("AI解析题目", "上传题图或文本", "输入内容合法？", "调用AI解析服务", "解析结果生成成功？", "返回题目/答案/思路", "返回参数错误", "返回解析失败"),
            ai_response_flow("AI翻译", "接收原文与目标语言", "翻译参数合法？", "调用AI翻译", "翻译结果生成成功？", "返回翻译结果", "返回参数错误", "返回翻译失败"),
            ai_response_flow("搜单题", "上传题图或文本", "输入内容合法？", "识别题目并搜索答案", "检索结果生成成功？", "返回单题结果", "返回参数错误", "返回搜索失败"),
            ai_response_flow("搜整页", "上传整页题图", "输入内容合法？", "识别多题并逐题搜索", "整页结果生成成功？", "返回整页结果", "返回参数错误", "返回搜索失败"),
            ai_response_flow("批改作文", "提交作文文本", "作文内容合法？", "调用AI批改作文", "批改结果生成成功？", "返回评分与建议", "返回参数错误", "返回批改失败"),
            flow(
                "获取首页数据",
                [
                    start_node(),
                    process_node("auth", "识别当前用户", 1),
                    decision_node("check_login", "登录态有效？", 2),
                    process_node("aggregate", "聚合课程/复习/待办/专注/签到", 3),
                    decision_node("check_data", "关键数据拉取完成？", 4),
                    process_node("compose", "组装首页数据", 5),
                    process_node("response", "返回首页信息", 6),
                    end_node(7),
                    process_node("auth_error", "返回鉴权失败", 2, 1),
                    process_node("fallback", "缺失项按默认值降级", 4, 1),
                ],
                [
                    EdgeSpec("start", "auth"),
                    EdgeSpec("auth", "check_login"),
                    EdgeSpec("check_login", "aggregate", "是"),
                    EdgeSpec("aggregate", "check_data"),
                    EdgeSpec("check_data", "compose", "是"),
                    EdgeSpec("compose", "response"),
                    EdgeSpec("response", "end"),
                    EdgeSpec("check_login", "auth_error", "否"),
                    EdgeSpec("check_data", "fallback", "否"),
                    EdgeSpec("auth_error", "end", route="to_end"),
                    EdgeSpec("fallback", "compose", route="return_up"),
                ],
            ),
            admin_action_flow("管理：创建横幅", "接收横幅配置", "横幅配置合法？", "创建横幅记录", "返回创建结果", "返回配置错误"),
            admin_list_flow("管理员获取用户列表", "接收分页与筛选", "查询用户列表", "返回用户分页"),
            admin_action_flow("管理员更新用户状态", "接收用户ID与目标状态", "目标用户存在？", "更新启用/禁用状态", "返回处理结果", "返回用户不存在"),
            admin_action_flow("管理员重置密码", "接收目标用户", "目标用户存在？", "重置为默认密码", "返回重置结果", "返回用户不存在"),
            admin_list_flow("管理员获取成绩列表", "接收筛选条件", "查询成绩记录", "返回成绩分页"),
        ]
    )
    return tuple(specs)


ALL_FLOW_SPECS = build_all_flow_specs()
FLOW_SPECS_BY_TITLE = {spec.module: spec for spec in ALL_FLOW_SPECS}


def get_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    if DEFAULT_FONT_PATH.exists():
        return ImageFont.truetype(str(DEFAULT_FONT_PATH), size)
    return ImageFont.load_default()


def measure_text(font: ImageFont.ImageFont, text: str) -> int:
    bbox = font.getbbox(text)
    return bbox[2] - bbox[0]


def wrap_text(font: ImageFont.ImageFont, text: str, max_width: int) -> list[str]:
    if measure_text(font, text) <= max_width:
        return [text]

    lines: list[str] = []
    current = ""
    for char in text:
        candidate = f"{current}{char}"
        if current and measure_text(font, candidate) > max_width:
            lines.append(current)
            current = char
        else:
            current = candidate
    if current:
        lines.append(current)
    return lines or [text]


def node_dimensions(kind: str) -> tuple[int, int]:
    if kind in {"start", "end"}:
        return 136, 78
    if kind == "decision":
        return 210, 124
    return 194, 94


def lane_y(lane: int) -> int:
    return MAIN_Y if lane == 0 else BRANCH_Y


def compute_boxes(spec: FlowSpec) -> dict[str, Box]:
    max_column = max(node.column for node in spec.nodes)
    usable_width = CANVAS_WIDTH - LEFT_MARGIN - RIGHT_MARGIN
    gap = usable_width / max(max_column, 1)
    boxes: dict[str, Box] = {}
    for node in spec.nodes:
        width, height = node_dimensions(node.kind)
        center_x = LEFT_MARGIN + node.column * gap
        center_y = lane_y(node.lane)
        boxes[node.id] = Box(center_x - width / 2, center_y - height / 2, width, height)
    return boxes


def draw_wrapped_text(
    draw: ImageDraw.ImageDraw,
    box: Box,
    text: str,
    *,
    max_width: int,
    max_height: int,
    font_size: int = 24,
    fill: str = PALETTE["text"],
) -> None:
    candidate_sizes = [font_size, 22, 20, 18, 16, 14]
    font = get_font(candidate_sizes[-1])
    lines = wrap_text(font, text, max_width)
    line_height = candidate_sizes[-1] + 6

    for size in candidate_sizes:
        font = get_font(size)
        lines = wrap_text(font, text, max_width)
        line_height = size + 6
        total_height = line_height * len(lines) - 6
        if total_height <= max_height:
            break

    total_height = line_height * len(lines) - 6
    start_y = box.cy - total_height / 2
    for index, line in enumerate(lines):
        line_width = measure_text(font, line)
        x = box.cx - line_width / 2
        y = start_y + index * line_height
        draw.text((x, y), line, font=font, fill=fill)


def draw_branch_label(draw: ImageDraw.ImageDraw, x: float, y: float, text: str) -> None:
    font = get_font(18)
    text_width = measure_text(font, text)
    padding_x = 14
    padding_y = 8
    draw.rounded_rectangle(
        (
            x - text_width / 2 - padding_x,
            y - 18 - padding_y,
            x + text_width / 2 + padding_x,
            y + 4 + padding_y,
        ),
        radius=12,
        fill=PALETTE["branch_label_fill"],
        outline="#d1d1d1",
        width=2,
    )
    draw.text((x - text_width / 2, y - 18), text, font=font, fill=PALETTE["branch_label_text"])


def anchor(box: Box, side: str) -> tuple[float, float]:
    if side == "left":
        return box.left, box.cy
    if side == "right":
        return box.right, box.cy
    if side == "top":
        return box.cx, box.top
    if side == "bottom":
        return box.cx, box.bottom
    raise ValueError(f"Unsupported anchor side: {side}")


def arrow_head(draw: ImageDraw.ImageDraw, start: tuple[float, float], end: tuple[float, float], *, size: int = 16) -> None:
    dx = end[0] - start[0]
    dy = end[1] - start[1]
    if dx == 0 and dy == 0:
        return
    if abs(dx) >= abs(dy):
        if dx >= 0:
            points = [end, (end[0] - size, end[1] - size / 2), (end[0] - size, end[1] + size / 2)]
        else:
            points = [end, (end[0] + size, end[1] - size / 2), (end[0] + size, end[1] + size / 2)]
    else:
        if dy >= 0:
            points = [end, (end[0] - size / 2, end[1] - size), (end[0] + size / 2, end[1] - size)]
        else:
            points = [end, (end[0] - size / 2, end[1] + size), (end[0] + size / 2, end[1] + size)]
    draw.polygon(points, fill=PALETTE["outline"])


def draw_polyline(draw: ImageDraw.ImageDraw, points: list[tuple[float, float]], *, width: int = 4) -> None:
    for start, end in zip(points, points[1:]):
        draw.line((start, end), fill=PALETTE["outline"], width=width)
    arrow_head(draw, points[-2], points[-1])


def edge_points(boxes: dict[str, Box], nodes: dict[str, NodeSpec], edge: EdgeSpec) -> list[tuple[float, float]]:
    source = nodes[edge.source]
    target = nodes[edge.target]
    source_box = boxes[edge.source]
    target_box = boxes[edge.target]

    if edge.route == "to_end":
        start = anchor(source_box, "right")
        end = anchor(target_box, "bottom")
        return [start, (end[0], start[1]), end]

    if edge.route == "return_up":
        start = anchor(source_box, "top")
        end = anchor(target_box, "bottom")
        via_y = source_box.top - 28
        return [start, (start[0], via_y), (end[0], via_y), end]

    if source.lane == target.lane == 0:
        return [anchor(source_box, "right"), anchor(target_box, "left")]

    if source.lane == 0 and target.lane == 1:
        return [anchor(source_box, "bottom"), anchor(target_box, "top")]

    if source.lane == 1 and target.lane == 0:
        start = anchor(source_box, "top")
        end = anchor(target_box, "bottom")
        via_y = source_box.top - 24
        return [start, (start[0], via_y), (end[0], via_y), end]

    return [anchor(source_box, "right"), anchor(target_box, "left")]


def draw_edge_label(draw: ImageDraw.ImageDraw, points: list[tuple[float, float]], label: str) -> None:
    if not label:
        return
    start = points[0]
    end = points[1]
    if abs(start[0] - end[0]) >= abs(start[1] - end[1]):
        label_x = (start[0] + end[0]) / 2
        label_y = min(start[1], end[1]) - 10
    else:
        label_x = max(start[0], end[0]) + 28
        label_y = (start[1] + end[1]) / 2
    draw_branch_label(draw, label_x, label_y, label)


def draw_node(draw: ImageDraw.ImageDraw, node: NodeSpec, box: Box) -> None:
    outline_width = 3
    if node.kind in {"start", "end"}:
        draw.ellipse((box.left, box.top, box.right, box.bottom), fill=PALETTE["terminal_fill"], outline=PALETTE["outline"], width=outline_width)
        draw_wrapped_text(draw, box, node.text, max_width=int(box.width - 32), max_height=int(box.height - 24), font_size=24)
        return

    if node.kind == "decision":
        draw.polygon(
            [(box.cx, box.top), (box.right, box.cy), (box.cx, box.bottom), (box.left, box.cy)],
            fill=PALETTE["decision_fill"],
            outline=PALETTE["outline"],
            width=outline_width,
        )
        draw_wrapped_text(draw, box, node.text, max_width=int(box.width * 0.52), max_height=int(box.height * 0.58), font_size=24)
        return

    draw.rounded_rectangle((box.left, box.top, box.right, box.bottom), radius=18, fill=PALETTE["process_fill"], outline=PALETTE["outline"], width=outline_width)
    draw_wrapped_text(draw, box, node.text, max_width=int(box.width - 36), max_height=int(box.height - 24), font_size=24)


def render_flow_chart(spec: FlowSpec, output_path: Path) -> None:
    image = Image.new("RGB", (CANVAS_WIDTH, CANVAS_HEIGHT), PALETTE["background"])
    draw = ImageDraw.Draw(image)
    title_font = get_font(42)
    draw.text((LEFT_MARGIN - 10, TITLE_Y), spec.module, font=title_font, fill=PALETTE["title"])

    boxes = compute_boxes(spec)
    nodes = {node.id: node for node in spec.nodes}

    for edge in spec.edges:
        points = edge_points(boxes, nodes, edge)
        draw_polyline(draw, points)
        draw_edge_label(draw, points, edge.label)

    for node in spec.nodes:
        draw_node(draw, node, boxes[node.id])

    output_path.parent.mkdir(parents=True, exist_ok=True)
    image.save(output_path, format="PNG")


def paragraph_text(paragraph: etree._Element) -> str:
    texts = paragraph.xpath(".//w:t/text()", namespaces=NS)
    return "".join(texts).strip()


def extract_module_entries(docx_path: Path) -> list[ModuleDocEntry]:
    with zipfile.ZipFile(docx_path) as archive:
        document = etree.fromstring(archive.read("word/document.xml"))

    paragraphs = document.xpath("//w:body/w:p", namespaces=NS)
    lines = [paragraph_text(paragraph) for paragraph in paragraphs]

    entries: list[ModuleDocEntry] = []
    for index in range(len(lines) - 2):
        if lines[index + 1] == "功能描述":
            entries.append(ModuleDocEntry(title=lines[index], description=lines[index + 2]))
    return entries


def next_relationship_id(rels_root: etree._Element) -> str:
    max_index = 0
    for rel in rels_root.findall(f"{REL}Relationship"):
        rel_id = rel.get("Id", "")
        if rel_id.startswith("rId") and rel_id[3:].isdigit():
            max_index = max(max_index, int(rel_id[3:]))
    return f"rId{max_index + 1}"


def next_docpr_id(document_root: etree._Element) -> int:
    ids = [int(value) for value in document_root.xpath("//wp:docPr/@id", namespaces=NS) if value.isdigit()]
    return max(ids, default=0) + 1


def ensure_png_content_type(content_root: etree._Element) -> None:
    for node in content_root.findall(f"{CT}Default"):
        if node.get("Extension", "").lower() == "png":
            return
    default = etree.Element(f"{CT}Default")
    default.set("Extension", "png")
    default.set("ContentType", "image/png")
    content_root.append(default)


def build_image_paragraph(rel_id: str, image_name: str, width_px: int, height_px: int, docpr_id: int) -> etree._Element:
    width_emu = width_px * EMU_PER_PIXEL
    height_emu = height_px * EMU_PER_PIXEL
    if width_emu > MAX_DOCX_WIDTH_EMU:
        scale = MAX_DOCX_WIDTH_EMU / width_emu
        width_emu = int(width_emu * scale)
        height_emu = int(height_emu * scale)

    paragraph = etree.Element(f"{W}p")
    p_pr = etree.SubElement(paragraph, f"{W}pPr")
    etree.SubElement(p_pr, f"{W}jc", {f"{W}val": "center"})
    etree.SubElement(p_pr, f"{W}spacing", {f"{W}before": "80", f"{W}after": "120", f"{W}line": "240", f"{W}lineRule": "auto"})

    run = etree.SubElement(paragraph, f"{W}r")
    drawing = etree.SubElement(run, f"{W}drawing")
    inline = etree.SubElement(drawing, f"{WP}inline", distT="0", distB="0", distL="0", distR="0")
    etree.SubElement(inline, f"{WP}extent", cx=str(width_emu), cy=str(height_emu))
    etree.SubElement(inline, f"{WP}docPr", id=str(docpr_id), name=image_name)
    frame_pr = etree.SubElement(inline, f"{WP}cNvGraphicFramePr")
    etree.SubElement(frame_pr, f"{A}graphicFrameLocks", noChangeAspect="1")

    graphic = etree.SubElement(inline, f"{A}graphic")
    graphic_data = etree.SubElement(graphic, f"{A}graphicData", uri=PIC_NS)
    picture = etree.SubElement(graphic_data, f"{PIC}pic")

    nv_pic_pr = etree.SubElement(picture, f"{PIC}nvPicPr")
    etree.SubElement(nv_pic_pr, f"{PIC}cNvPr", id="0", name=image_name)
    etree.SubElement(nv_pic_pr, f"{PIC}cNvPicPr")

    blip_fill = etree.SubElement(picture, f"{PIC}blipFill")
    etree.SubElement(blip_fill, f"{A}blip", {f"{R}embed": rel_id})
    stretch = etree.SubElement(blip_fill, f"{A}stretch")
    etree.SubElement(stretch, f"{A}fillRect")

    shape_pr = etree.SubElement(picture, f"{PIC}spPr")
    xfrm = etree.SubElement(shape_pr, f"{A}xfrm")
    etree.SubElement(xfrm, f"{A}off", x="0", y="0")
    etree.SubElement(xfrm, f"{A}ext", cx=str(width_emu), cy=str(height_emu))
    preset_geom = etree.SubElement(shape_pr, f"{A}prstGeom", prst="rect")
    etree.SubElement(preset_geom, f"{A}avLst")
    return paragraph


def find_program_logic_anchor(document_root: etree._Element, module_name: str) -> etree._Element:
    paragraphs = document_root.xpath("//w:body/w:p", namespaces=NS)
    module_index = None
    for index, paragraph in enumerate(paragraphs):
        if paragraph_text(paragraph) == module_name:
            module_index = index
            break
    if module_index is None:
        raise ValueError(f"Document does not contain module title: {module_name}")

    for paragraph in paragraphs[module_index + 1 :]:
        text = paragraph_text(paragraph)
        if text == "程序逻辑":
            return paragraph
        if text == "限制条件":
            break
    raise ValueError(f"Document does not contain a 程序逻辑 paragraph for module: {module_name}")


def insert_images(source_docx: Path, output_docx: Path, artifacts: list[DiagramArtifact]) -> None:
    output_docx.parent.mkdir(parents=True, exist_ok=True)
    work_dir = output_docx.parent / "_program_logic_docx_work"
    if work_dir.exists():
        shutil.rmtree(work_dir, ignore_errors=True)
    work_dir.mkdir(parents=True, exist_ok=True)

    try:
        with zipfile.ZipFile(source_docx, "r") as archive:
            archive.extractall(str(work_dir))

        document_path = work_dir / "word" / "document.xml"
        rels_path = work_dir / "word" / "_rels" / "document.xml.rels"
        content_types_path = work_dir / "[Content_Types].xml"
        media_dir = work_dir / "word" / "media"
        media_dir.mkdir(parents=True, exist_ok=True)

        document_tree = etree.parse(str(document_path))
        rels_tree = etree.parse(str(rels_path))
        content_tree = etree.parse(str(content_types_path))
        document_root = document_tree.getroot()
        rels_root = rels_tree.getroot()
        content_root = content_tree.getroot()

        docpr_id = next_docpr_id(document_root)

        for index, artifact in enumerate(artifacts, start=1):
            rel_id = next_relationship_id(rels_root)
            internal_name = f"program-logic-{index:02d}.png"
            shutil.copy2(artifact.image_path, media_dir / internal_name)

            rel = etree.Element(f"{REL}Relationship")
            rel.set("Id", rel_id)
            rel.set("Type", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image")
            rel.set("Target", f"media/{internal_name}")
            rels_root.append(rel)

            image = Image.open(artifact.image_path)
            try:
                paragraph = build_image_paragraph(rel_id, internal_name, image.width, image.height, docpr_id)
            finally:
                image.close()

            anchor_paragraph = find_program_logic_anchor(document_root, artifact.spec.module)
            anchor_paragraph.addnext(paragraph)
            docpr_id += 1

        ensure_png_content_type(content_root)
        document_tree.write(str(document_path), xml_declaration=True, encoding="UTF-8", standalone="yes")
        rels_tree.write(str(rels_path), xml_declaration=True, encoding="UTF-8", standalone="yes")
        content_tree.write(str(content_types_path), xml_declaration=True, encoding="UTF-8", standalone="yes")

        with zipfile.ZipFile(output_docx, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            for path in sorted(work_dir.rglob("*")):
                if path.is_file():
                    archive.write(path, path.relative_to(work_dir).as_posix())
    finally:
        shutil.rmtree(work_dir, ignore_errors=True)


def image_name(index: int, module_name: str) -> str:
    safe = module_name.replace("：", "-").replace("/", "-")
    return f"{index:02d}-{safe}.png"


def build_manifest(input_docx: Path, output_docx: Path, artifacts: list[DiagramArtifact], manifest_path: Path) -> None:
    manifest = {
        "input_docx": str(input_docx.resolve()),
        "output_docx": str(output_docx.resolve()),
        "diagram_count": len(artifacts),
        "diagrams": [
            {
                "module": artifact.spec.module,
                "description": artifact.description,
                "image": str(artifact.image_path.resolve()),
            }
            for artifact in artifacts
        ],
    }
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")


def verify_output_docx(output_docx: Path, artifacts: list[DiagramArtifact]) -> None:
    with zipfile.ZipFile(output_docx) as archive:
        document = etree.fromstring(archive.read("word/document.xml"))
        paragraphs = document.xpath("//w:body/w:p", namespaces=NS)
        texts = [paragraph_text(paragraph) for paragraph in paragraphs]

        for artifact in artifacts:
            title_index = texts.index(artifact.spec.module)
            logic_index = next(index for index in range(title_index + 1, len(texts)) if texts[index] == "程序逻辑")
            limit_index = next(index for index in range(logic_index + 1, len(texts)) if texts[index] == "限制条件")
            middle_paragraphs = paragraphs[logic_index + 1 : limit_index]
            has_drawing = any(paragraph.xpath(".//w:drawing", namespaces=NS) for paragraph in middle_paragraphs)
            if not has_drawing:
                raise ValueError(f"Inserted image position is invalid for module: {artifact.spec.module}")

        media = [name for name in archive.namelist() if name.startswith("word/media/program-logic-")]
        if len(media) != len(artifacts):
            raise ValueError(f"Expected {len(artifacts)} generated media files, but found {len(media)}")


def validate_flow_coverage(doc_entries: list[ModuleDocEntry]) -> None:
    doc_titles = [entry.title for entry in doc_entries]
    flow_titles = list(FLOW_SPECS_BY_TITLE)

    missing = [title for title in doc_titles if title not in FLOW_SPECS_BY_TITLE]
    extra = [title for title in flow_titles if title not in doc_titles]
    if missing or extra:
        raise ValueError(f"Flow spec coverage mismatch. missing={missing}, extra={extra}")
    if len(flow_titles) != len(doc_titles):
        raise ValueError(f"Flow spec count mismatch. expected={len(doc_titles)}, actual={len(flow_titles)}")


def build_artifacts(entries: list[ModuleDocEntry], image_dir: Path) -> list[DiagramArtifact]:
    artifacts: list[DiagramArtifact] = []
    for index, entry in enumerate(entries, start=1):
        image_path = image_dir / image_name(index, entry.title)
        spec = FLOW_SPECS_BY_TITLE[entry.title]
        render_flow_chart(spec, image_path)
        artifacts.append(DiagramArtifact(spec=spec, description=entry.description, image_path=image_path))
    return artifacts


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate program logic diagrams and insert them into a DOCX copy.")
    parser.add_argument("--input-docx", default=r"C:\Users\Administrator\Desktop\详细设计.docx", help="Source DOCX path.")
    parser.add_argument("--output-docx", default=r"docs\详细设计-程序逻辑图版.docx", help="Output DOCX path inside the workspace.")
    parser.add_argument("--image-dir", default=r"docs\program-logic-diagrams", help="Directory for generated PNG diagrams.")
    parser.add_argument("--scope", choices=("sample", "all"), default="all", help="Generate the five sample diagrams or all diagrams.")
    args = parser.parse_args()

    input_docx = Path(args.input_docx)
    output_docx = Path(args.output_docx)
    image_dir = Path(args.image_dir)

    if not input_docx.exists():
        raise FileNotFoundError(f"Source docx not found: {input_docx}")

    doc_entries = extract_module_entries(input_docx)
    validate_flow_coverage(doc_entries)

    if args.scope == "sample":
        selected_entries = [entry for entry in doc_entries if entry.title in SAMPLE_MODULE_NAMES]
        manifest_name = "sample-manifest.json"
    else:
        selected_entries = doc_entries
        manifest_name = "manifest.json"

    artifacts = build_artifacts(selected_entries, image_dir)
    insert_images(input_docx, output_docx, artifacts)
    build_manifest(input_docx, output_docx, artifacts, image_dir / manifest_name)
    verify_output_docx(output_docx, artifacts)

    print(f"Generated {len(artifacts)} diagrams.")
    print(f"Output DOCX: {output_docx}")
    for artifact in artifacts:
        print(f"{artifact.spec.module}: {artifact.image_path}")


if __name__ == "__main__":
    main()
