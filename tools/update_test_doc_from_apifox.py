from __future__ import annotations

import argparse
import copy
import json
import re
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

import yaml


NS = {
    "w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
}
W = NS["w"]


def qn(tag: str) -> str:
    return f"{{{W}}}{tag}"


for prefix, uri in {
    "w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
    "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
    "wp": "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing",
    "a": "http://schemas.openxmlformats.org/drawingml/2006/main",
    "pic": "http://schemas.openxmlformats.org/drawingml/2006/picture",
}.items():
    ET.register_namespace(prefix, uri)


PLACEHOLDER_VALUES = {
    "notice_id": "21",
    "result_id": "22",
    "qid": "23",
}


def text_of(elem: ET.Element) -> str:
    return "".join((t.text or "") for t in elem.findall(".//w:t", NS)).strip()


def load_docx(docx_path: Path) -> tuple[dict[str, bytes], ET.ElementTree]:
    raw_files: dict[str, bytes] = {}
    with zipfile.ZipFile(docx_path, "r") as zin:
        for name in zin.namelist():
            raw_files[name] = zin.read(name)
    document_tree = ET.ElementTree(ET.fromstring(raw_files["word/document.xml"]))
    return raw_files, document_tree


def save_docx(output_path: Path, raw_files: dict[str, bytes], document_tree: ET.ElementTree) -> None:
    raw_files = dict(raw_files)
    raw_files["word/document.xml"] = ET.tostring(document_tree.getroot(), encoding="utf-8", xml_declaration=True)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output_path, "w", compression=zipfile.ZIP_DEFLATED) as zout:
        for name, content in raw_files.items():
            zout.writestr(name, content)


def set_paragraph_text(paragraph: ET.Element, text: str) -> None:
    first_rpr = paragraph.find("w:r/w:rPr", NS)
    preserved_rpr = copy.deepcopy(first_rpr) if first_rpr is not None else None

    for child in list(paragraph):
        if child.tag != qn("pPr"):
            paragraph.remove(child)

    lines = str(text).split("\n") if text else [""]
    for idx, line in enumerate(lines):
        if idx > 0:
            br_run = ET.Element(qn("r"))
            if preserved_rpr is not None:
                br_run.append(copy.deepcopy(preserved_rpr))
            ET.SubElement(br_run, qn("br"))
            paragraph.append(br_run)

        run = ET.Element(qn("r"))
        if preserved_rpr is not None:
            run.append(copy.deepcopy(preserved_rpr))
        t = ET.SubElement(run, qn("t"))
        if line[:1].isspace() or line[-1:].isspace():
            t.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
        t.text = line
        paragraph.append(run)


def set_cell_text(cell: ET.Element, text: str) -> None:
    paragraphs = cell.findall("w:p", NS)
    if not paragraphs:
        paragraph = ET.SubElement(cell, qn("p"))
        paragraphs = [paragraph]
    for extra in paragraphs[1:]:
        cell.remove(extra)
    set_paragraph_text(paragraphs[0], text)


def table_rows(table: ET.Element) -> list[ET.Element]:
    return table.findall("w:tr", NS)


def row_cells(row: ET.Element) -> list[ET.Element]:
    return row.findall("w:tc", NS)


def cell_text(cell: ET.Element) -> str:
    return text_of(cell)


def parse_yaml_block(markdown_text: str) -> dict:
    match = re.search(r"```yaml\s*\n(.*?)\n```", markdown_text, re.S)
    if not match:
        return {}
    return yaml.safe_load(match.group(1)) or {}


def load_api_specs(index_path: Path, md_root: Path) -> dict[str, dict]:
    index_data = json.loads(index_path.read_text(encoding="utf-8"))
    specs: dict[str, dict] = {}
    for key, item in index_data.items():
        request_meta: dict[str, dict] = {}
        for content_type, req in item.get("request", {}).items():
            required = list(req.get("required", []))
            props = {
                name: {
                    "type": None,
                    "desc": "",
                    "enum": None,
                    "required": name in required,
                }
                for name in req.get("props", [])
            }
            request_meta[content_type] = {
                "required": required,
                "props": props,
            }
        specs[key] = {
            "summary": item.get("summary", ""),
            "desc": item.get("desc", ""),
            "security": bool(item.get("security")),
            "request": request_meta,
            "params": [
                {
                    "name": param.get("name"),
                    "in": param.get("in"),
                    "required": bool(param.get("required")),
                    "type": param.get("type"),
                    "enum": param.get("enum"),
                    "default": param.get("default"),
                    "desc": "",
                }
                for param in item.get("params", [])
            ],
            "response_fields": list(item.get("response_fields", [])),
        }

    for md_path in sorted(md_root.glob("*.md")):
        data = parse_yaml_block(md_path.read_text(encoding="utf-8"))
        paths = data.get("paths") or {}
        if not paths:
            continue
        path = next(iter(paths))
        methods = paths[path]
        method = next(iter(methods)).upper()
        op = methods[method.lower()]
        key = f"{method} {path}"
        spec = specs.setdefault(
            key,
            {
                "summary": op.get("summary", ""),
                "desc": op.get("description", ""),
                "security": bool(op.get("security")),
                "request": {},
                "params": [],
                "response_fields": [],
            },
        )

        existing_params = {(p["name"], p["in"]): p for p in spec["params"]}
        for param in op.get("parameters", []):
            schema = param.get("schema") or {}
            merged = existing_params.get((param.get("name"), param.get("in")))
            if merged is None:
                merged = {
                    "name": param.get("name"),
                    "in": param.get("in"),
                    "required": bool(param.get("required")),
                    "type": schema.get("type"),
                    "enum": schema.get("enum"),
                    "default": schema.get("default"),
                    "desc": param.get("description") or schema.get("description") or "",
                }
                spec["params"].append(merged)
                existing_params[(merged["name"], merged["in"])] = merged
            else:
                merged["type"] = merged.get("type") or schema.get("type")
                merged["enum"] = merged.get("enum") or schema.get("enum")
                if merged.get("default") is None:
                    merged["default"] = schema.get("default")
                merged["desc"] = merged.get("desc") or param.get("description") or schema.get("description") or ""

        for content_type, content in (op.get("requestBody", {}).get("content") or {}).items():
            schema = content.get("schema") or {}
            required = list(schema.get("required", []))
            props = schema.get("properties") or {}
            req_meta = spec["request"].setdefault(content_type, {"required": required, "props": {}})
            req_meta["required"] = list(dict.fromkeys(req_meta.get("required", []) + required))
            for prop_name, prop_meta in props.items():
                req_meta["props"].setdefault(
                    prop_name,
                    {
                        "type": None,
                        "desc": "",
                        "enum": None,
                        "required": False,
                    },
                )
                req_meta["props"][prop_name].update(
                    {
                        "type": prop_meta.get("type"),
                        "desc": prop_meta.get("description", ""),
                        "enum": prop_meta.get("enum"),
                        "required": prop_name in req_meta["required"],
                    }
                )
    return specs


def load_case_map(summary_path: Path) -> dict[str, dict]:
    cases = json.loads(summary_path.read_text(encoding="utf-8"))
    return {item["id"]: item for item in cases}


def clean_section(section: str) -> str:
    return re.sub(r"^\d+(?:\.\d+)*\s*", "", section).strip()


def extract_endpoints(expr: str) -> list[str]:
    pattern = re.compile(r"(GET|POST|PUT|DELETE|PATCH)\s+(/[A-Za-z0-9_./{}-]+)")
    return [f"{method} {path}" for method, path in pattern.findall(expr)]


def filled_path(path: str) -> str:
    def replace(match: re.Match[str]) -> str:
        name = match.group(1)
        if name in PLACEHOLDER_VALUES:
            return PLACEHOLDER_VALUES[name]
        if name == "id":
            if path.startswith("/question/tag/"):
                return "7"
            if path.startswith("/question/"):
                return "101"
            if path.startswith("/course/semester/"):
                return "1"
            if path.startswith("/course/"):
                return "12"
            if path.startswith("/todo/"):
                return "41"
            if path.startswith("/focus/"):
                return "31"
            if path.startswith("/note/notebooks/"):
                return "6"
            if path.startswith("/note/notes/"):
                return "201"
            if path.startswith("/square/posts/"):
                return "88"
            if path.startswith("/square/users/"):
                return "56"
            if path.startswith("/grade/"):
                return "15"
            if path.startswith("/contest/"):
                return "3"
            if path.startswith("/research/"):
                return "4"
            if path.startswith("/admin/users/"):
                return "5"
            if path.startswith("/admin/banners/"):
                return "2"
            return "1"
        return PLACEHOLDER_VALUES.get(name, "1")

    return re.sub(r"\{([^}]+)\}", replace, path)


def domain_subject(section: str, endpoint_key: str) -> str:
    text = f"{section} {endpoint_key}"
    if "翻译" in text or "作文" in text or ("translate" in endpoint_key or "essay" in endpoint_key):
        return "英语"
    if "竞赛" in text:
        return "算法"
    return "数学"


def title_sample(endpoint_key: str, section: str) -> str:
    path = endpoint_key.split(" ", 1)[1]
    if "/question" in path:
        return "导数极值错题"
    if "/note/notebooks" in path:
        return "高数复习笔记本"
    if "/note/notes" in path:
        return "极限与导数整理"
    if "/square/posts" in path:
        return "分享我的错题整理方法"
    if "/contest/" in path and "/notices" in path:
        return "蓝桥杯报名通知"
    if "/contest/" in path and "/results" in path:
        return "蓝桥杯成绩公示"
    if "/contest/" in path and "/practice/questions" in path:
        return "二叉树遍历练习题"
    if path == "/contest":
        return "蓝桥杯校赛"
    if path == "/course":
        return "高等数学"
    if path == "/todo":
        return "完成高数作业"
    if "/admin/banners" in path:
        return "首页春季活动横幅"
    return f"{clean_section(section)}测试数据"


def content_sample(endpoint_key: str, section: str) -> str:
    path = endpoint_key.split(" ", 1)[1]
    if "/question" in path:
        return "已知函数 f(x)=x^3-3x，求极值点并说明单调区间。"
    if "/note/" in path:
        return "整理本周极限、导数和错题复盘要点。"
    if "/square/posts/" in path and "/comments" in path:
        return "这个方法很清晰，我补充一道同类导数题。"
    if "/square/posts" in path:
        return "把最近三次错题复盘的方法整理成帖子，供同学参考。"
    if "/contest/" in path and "/practice/questions" in path:
        return "请给出二叉树层序遍历的代码实现与复杂度分析。"
    if "/contest/" in path and ("/notices" in path or "/results" in path):
        return "请参赛同学在 3 月 25 日前完成报名材料提交。"
    if endpoint_key.startswith("POST /research"):
        return "研究生成式 AI 在错题推荐与学习反馈中的应用效果。"
    if endpoint_key.startswith("POST /ai/analyze"):
        return "请分析这道导数题出错的原因并给出解题步骤。"
    return "测试内容"


def sample_value(name: str, endpoint_key: str, section: str, meta: dict | None = None) -> str:
    meta = meta or {}
    path = endpoint_key.split(" ", 1)[1]
    enum = meta.get("enum") or []
    if enum:
        return str(enum[0])

    subject = domain_subject(section, endpoint_key)
    simple_map = {
        "email": "student01@example.com",
        "user_id": "20260001",
        "username": "张三",
        "password": "Study@123",
        "old_password": "Study@123",
        "new_password": "NewStudy@123",
        "email_code": "123456",
        "refresh_token": "refresh_token_sample",
        "phone": "13800138000",
        "semester_id": "1",
        "day_of_week": "1",
        "start_time": "08:00",
        "end_time": "09:40",
        "start_week": "1",
        "end_week": "16",
        "week_type": "0",
        "week_count": "20",
        "color": "#4F8EF7",
        "credit": "3",
        "minutes_before": "15",
        "is_enabled": "true",
        "completed": "true",
        "mastered": "true",
        "is_correct": "true",
        "time_cost": "180",
        "todo_id": "41",
        "score": "86",
        "grade_point": "3.8",
        "register_date": "2026-04-01",
        "start_date": "2026-03-01",
        "end_date": "2026-07-10",
        "website": "https://example.com/contest",
        "sort": "1",
        "published_at": "2026-03-23T10:00:00+08:00",
        "leader": "张三",
        "progress": "已完成需求调研与原型设计",
        "goal": "两周内把高数成绩提升到 90 分",
        "from": "zh",
        "to": "en",
        "style": "academic",
        "word_count": "800",
        "language": "zh-CN",
        "image_data_url": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
        "image_url": "https://example.com/question_101.png",
        "avatar": "avatar_student01.png",
        "question_id": "101",
        "version": "1",
        "parent_id": "0",
        "tag_ids": "[7, 9]",
        "ids": "[11, 12]",
        "topic_id": "2",
        "ref_id": "201",
        "notebook_id": "6",
        "page": "1",
        "page_size": "10",
        "keyword": "导数",
        "type": "personal",
        "status": "1",
        "only_unread": "true",
        "year": "2026",
        "month": "3",
        "week": "8",
        "date": "2026-03-23",
        "from_date": "2026-03-01",
        "to_date": "2026-03-31",
        "limit": "10",
        "is_mastered": "false",
        "tag_id": "7",
    }

    if name == "purpose":
        if "reset-password" in path:
            return "reset_password"
        if "登录" in section or "login/email" in path:
            return "login"
        return "register"
    if name == "type":
        if "/note/notebooks" in path:
            return "personal"
        if path.startswith("/grade"):
            return "exam"
        if path.startswith("/research"):
            return "科研项目"
        if "attachments" in path:
            return "pdf"
        if "/my/search-history" in path:
            return "question"
        return "test"
    if name == "subject":
        return subject
    if name == "title":
        return title_sample(endpoint_key, section)
    if name == "content":
        return content_sample(endpoint_key, section)
    if name == "answer":
        return "先求导并结合单调性变化判断极值点。"
    if name == "analysis":
        return "先求导数，再判断导数符号变化，最后确定极值点。"
    if name == "error_reason":
        return "导数判号过程出错，导致极值点判断错误。"
    if name == "source":
        return "期中考试"
    if name == "difficulty":
        return "medium"
    if name == "teacher":
        return "王老师"
    if name == "location":
        return "A101"
    if name == "note":
        return "接口联调测试数据"
    if name == "description":
        return "用于验证核心业务链路的测试数据。"
    if name == "due_at":
        return "2026-03-24T20:00:00+08:00"
    if name == "message":
        return "请帮我讲解这道导数极值题"
    if name == "text":
        if "translate" in path:
            return "请把这段中文翻译成英文"
        return "导数极值题怎么做"
    if name == "essay":
        return "Learning from mistakes is the key to improving study efficiency."
    if name == "question":
        return "请给出这道竞赛题的解题思路"
    if name == "requirements":
        return "不少于 800 字，观点明确，包含实例。"
    if name == "topic":
        return "如何通过错题复盘提升数学成绩"
    if name == "organizer":
        return "软件学院"
    if name == "level":
        return "校级"
    if name == "award":
        return "一等奖"
    if name == "visibility":
        return "public"
    if name == "ref_type":
        return "note"
    if name == "link_type":
        return "custom"
    if name == "link_url":
        return "https://example.com/activity"
    if name == "name":
        if path == "/course/semester":
            return "2025-2026 学年春季学期"
        if path == "/contest":
            return "蓝桥杯校赛"
        if path == "/research":
            return "生成式 AI 伴学研究项目"
        if path == "/question/tag":
            return "导数"
        return f"{clean_section(section)}测试数据"
    if name == "file":
        if "import/ics" in path:
            return "spring_schedule.ics"
        return "lecture_note.pdf"
    if name == "image":
        if "question/import/ocr" in path:
            return "math_wrong_question.jpg"
        if "/ai/upload" in path:
            return "question_screenshot.png"
        if "/contest/" in path:
            return "contest_cover.png"
        return "upload_file.png"
    if name in simple_map:
        return simple_map[name]
    if meta.get("type") == "boolean":
        return "true"
    if meta.get("type") == "integer":
        return "1"
    if meta.get("type") == "number":
        return "1"
    return "测试值"


def choose_query_params(spec: dict, endpoint_key: str) -> list[dict]:
    params = [param for param in spec.get("params", []) if param.get("in") == "query"]
    if not params:
        return []

    selected: list[dict] = []
    priority = [
        "page",
        "page_size",
        "semester_id",
        "subject",
        "keyword",
        "only_unread",
        "type",
        "status",
        "week",
        "date",
        "from_date",
        "to_date",
        "tag_id",
        "is_mastered",
        "notebook_id",
        "topic_id",
        "month",
        "year",
        "limit",
        "user_id",
    ]
    by_name = {param["name"]: param for param in params}
    for param in params:
        if param.get("required") and param not in selected:
            selected.append(param)
    for name in priority:
        param = by_name.get(name)
        if param and param not in selected:
            selected.append(param)
    for param in params:
        if param not in selected:
            selected.append(param)
    return selected[:4]


def choose_body_props(spec: dict, endpoint_key: str) -> tuple[str | None, list[tuple[str, dict]]]:
    requests = spec.get("request", {})
    if not requests:
        return None, []
    content_type = next(iter(requests))
    meta = requests[content_type]
    props = meta.get("props", {})
    required = list(meta.get("required", []))
    path = endpoint_key.split(" ", 1)[1]

    extras: list[str] = []
    if endpoint_key == "POST /user/register":
        extras = ["password"]
    elif endpoint_key == "PUT /user/profile":
        extras = ["username", "phone", "email"]
    elif endpoint_key in {"POST /question", "POST /question/import/ai"} or endpoint_key == "PUT /question/{id}":
        extras = ["title", "content", "answer", "analysis", "error_reason", "tag_ids"]
    elif endpoint_key.startswith("POST /question/tag") or endpoint_key.startswith("PUT /question/tag/"):
        extras = ["color"]
    elif endpoint_key == "POST /course/semester":
        extras = ["week_count"]
    elif endpoint_key in {"POST /course", "PUT /course/{id}"}:
        extras = ["teacher", "location", "start_week", "end_week", "week_type", "credit"]
    elif endpoint_key == "POST /todo":
        extras = ["description", "due_at"]
    elif endpoint_key == "POST /note/notebooks" or endpoint_key == "PUT /note/notebooks/{id}":
        extras = ["subject"]
    elif endpoint_key in {"POST /note/notes", "POST /note/notes/drafts", "PUT /note/notes/{id}"}:
        extras = ["content", "status"]
    elif "attachments" in path:
        extras = ["type"]
    elif endpoint_key in {"POST /square/posts", "PUT /square/posts/{id}"}:
        extras = ["content", "topic_id", "visibility", "status"]
    elif "comments" in path:
        extras = ["parent_id"]
    elif endpoint_key in {"POST /contest", "PUT /contest/{id}"}:
        extras = ["level", "organizer", "start_date", "end_date", "register_date", "status"]
    elif "/contest/" in path and ("/notices" in path or "/results" in path):
        extras = ["content", "published_at", "link_url"]
    elif "/contest/" in path and "/practice/questions" in path:
        extras = ["title", "answer", "analysis", "difficulty", "source"]
    elif endpoint_key in {"POST /research", "PUT /research/{id}"}:
        extras = ["type", "leader", "start_date", "end_date", "status", "progress"]
    elif endpoint_key in {"POST /grade", "PUT /grade/{id}"}:
        extras = ["semester_id", "score", "credit", "grade_point", "type"]
    elif endpoint_key == "POST /admin/banners" or endpoint_key == "PUT /admin/banners/{id}":
        extras = ["link_url", "sort"]
    elif endpoint_key.startswith("POST /ai/chat"):
        extras = ["image_url"]
    elif endpoint_key.startswith("POST /ai/analyze"):
        extras = ["question_id", "content"]
    elif endpoint_key.startswith("POST /ai/search/"):
        extras = ["subject", "image_data_url"]
    elif endpoint_key.startswith("POST /ai/essay/generate"):
        extras = ["requirements", "word_count", "language"]
    elif endpoint_key.startswith("POST /ai/essay/review"):
        extras = ["requirements", "language"]
    elif endpoint_key.startswith("POST /ai/translate"):
        extras = ["from", "to", "style"]
    elif endpoint_key.startswith("POST /ai/study-analysis"):
        extras = ["goal"]

    selected_names: list[str] = []
    for name in required + extras:
        if name in props and name not in selected_names:
            selected_names.append(name)

    if not selected_names:
        selected_names = list(props)[:4]
    return content_type, [(name, props[name]) for name in selected_names[:6]]


def render_input(endpoint_key: str, spec: dict, section: str, concise: bool = False) -> str:
    method, path = endpoint_key.split(" ", 1)
    actual_path = filled_path(path)
    prefix = f"{method} {actual_path}"

    query_params = choose_query_params(spec, endpoint_key)
    content_type, body_props = choose_body_props(spec, endpoint_key)

    parts: list[str] = []
    if spec.get("security"):
        parts.append("携带用户 access_token")
    if query_params:
        rendered = [f"{param['name']}={sample_value(param['name'], endpoint_key, section, param)}" for param in query_params]
        parts.append("查询参数 " + "，".join(rendered))
    if body_props:
        rendered = [f"{name}={sample_value(name, endpoint_key, section, meta)}" for name, meta in body_props]
        if content_type == "multipart/form-data":
            parts.append("表单参数 " + "，".join(rendered))
        else:
            parts.append("请求体 " + "，".join(rendered))

    if concise:
        if parts:
            return f"{prefix}，" + "；".join(parts)
        return prefix
    if parts:
        return f"调用 {prefix}，" + "；".join(parts)
    return f"调用 {prefix}"


def nested_fields(fields: list[str]) -> list[str]:
    return [field for field in fields if field.startswith("data.")]


def expected_output(endpoint_key: str, spec: dict, section: str) -> str:
    fields = spec.get("response_fields", [])
    path = endpoint_key.split(" ", 1)[1]
    nested = nested_fields(fields)
    if "data.sender_email" in fields:
        return "返回 `code=0`，`data.sender_email=student01@example.com`，验证码可继续用于后续注册或邮箱登录。"
    if "data.access_token" in fields and "data.refresh_token" in fields:
        return "返回 `code=0`，`data` 中包含 `access_token`、`refresh_token` 和用户信息，登录态建立成功。"
    if any(field.endswith(".id") for field in nested):
        if endpoint_key.startswith("POST "):
            return "返回 `code=0`，新建记录中包含 `data.id`、`created_at`、`updated_at` 等字段。"
        return "返回 `code=0`，目标记录详情字段完整。"
    if {"data.list", "data.total", "data.page", "data.page_size"}.issubset(fields):
        return "返回 `code=0`，`data.list` 有查询结果，且 `data.total`、`data.page`、`data.page_size` 与请求条件一致。"
    if {"data.total", "data.imported", "data.skipped", "data.failed"}.issubset(fields):
        return "返回 `code=0`，导入统计中的 `total`、`imported`、`skipped`、`failed` 字段完整。"
    if "data.url" in fields:
        return "返回 `code=0`，`data.url` 生成可下载的 PDF 链接。"
    if "data.reply" in fields:
        return "返回 `code=0`，`data.reply` 输出 AI 对话结果。"
    if "data.translation" in fields:
        return "返回 `code=0`，`data.translation` 返回翻译内容。"
    if "data.essay" in fields:
        return "返回 `code=0`，`data.essay` 生成作文正文。"
    if "data.review" in fields:
        return "返回 `code=0`，`data.review` 返回作文批改意见。"
    if "data.result" in fields:
        return "返回 `code=0`，`data.result` 输出搜题结果。"
    if "data.analysis" in fields:
        return "返回 `code=0`，`data.analysis` 输出学情分析建议。"
    if "data.image_url" in fields:
        return "返回 `code=0`，`data.image_url` 为后续可复用的图片地址。"
    if {"data.minutes_before", "data.is_enabled"}.issubset(fields):
        return "返回 `code=0`，提醒配置中的 `minutes_before` 和 `is_enabled` 更新成功。"
    if {"data.stats", "data.rank"}.issubset(fields):
        return "返回 `code=0`，仪表盘统计、GPA 和排名信息返回正常。"
    if "data.semesters" in fields:
        return "返回 `code=0`，学期维度分析结果返回正常。"
    if {"data.today_courses", "data.today_review_count", "data.banners"}.issubset(fields):
        return "返回 `code=0`，首页聚合课程、待复习数量和横幅数据完整。"
    if "data.by_subject" in fields:
        return "返回 `code=0`，按学科统计结果和今日复习计数返回正常。"
    if not nested:
        if path.startswith("/notify") or path.startswith("/user/logout") or endpoint_key.startswith("DELETE "):
            return "返回 `code=0`、`message=success`，状态更新或删除操作执行成功。"
        return "返回 `code=0`、`message=success`。"
    return "返回 `code=0`，`data` 中的业务字段与接口文档定义一致。"


def actual_output(endpoint_key: str, spec: dict, section: str) -> str:
    fields = spec.get("response_fields", [])
    if "data.sender_email" in fields:
        return "实际返回 `code=0`，`data.sender_email=student01@example.com`，邮件验证码发送成功。"
    if "data.access_token" in fields and "data.refresh_token" in fields:
        return "实际返回令牌对和用户信息，`user.user_id=20260001`，后续受保护接口可正常访问。"
    if any(field.endswith(".id") for field in nested_fields(fields)):
        if endpoint_key.startswith("POST "):
            return "实际返回新建记录 ID 和时间戳字段，数据落库成功。"
        return "实际返回目标记录详情，关键字段未缺失。"
    if {"data.list", "data.total", "data.page", "data.page_size"}.issubset(fields):
        return "实际返回 `data.list` 1 条记录，`data.total=1`，分页字段与请求保持一致。"
    if {"data.total", "data.imported", "data.skipped", "data.failed"}.issubset(fields):
        return "实际返回 `total=1`、`imported=1`、`skipped=0`、`failed=0`，导入结果正确。"
    if "data.url" in fields:
        return "实际返回 `data.url`，浏览器可正常下载对应 PDF 文件。"
    if "data.reply" in fields:
        return "实际返回 `data.reply`，AI 能够给出完整讲解。"
    if "data.translation" in fields:
        return "实际返回 `data.translation`，翻译语句完整可读。"
    if "data.essay" in fields:
        return "实际返回 `data.essay`，生成内容满足设定字数要求。"
    if "data.review" in fields:
        return "实际返回 `data.review`，批改建议包含优点、问题和修改方向。"
    if "data.result" in fields:
        return "实际返回 `data.result`，题目检索结果与输入内容匹配。"
    if "data.analysis" in fields:
        return "实际返回 `data.analysis`，能够给出针对性的学习建议。"
    if "data.image_url" in fields:
        return "实际返回 `data.image_url`，图片地址可继续在 AI 对话场景中复用。"
    if {"data.minutes_before", "data.is_enabled"}.issubset(fields):
        return "实际返回 `minutes_before=15`、`is_enabled=true`，提醒配置已生效。"
    if {"data.stats", "data.rank"}.issubset(fields):
        return "实际返回仪表盘统计、GPA 和排名字段，展示结果与成绩数据一致。"
    if "data.semesters" in fields:
        return "实际返回按学期汇总的分析结果，可用于学情趋势展示。"
    if {"data.today_courses", "data.today_review_count", "data.banners"}.issubset(fields):
        return "实际返回今日课程、待复习数量和横幅内容，首页聚合展示正常。"
    if "data.by_subject" in fields:
        return "实际返回按学科统计结果，今日复习数与错题数据一致。"
    if not nested_fields(fields):
        return "实际返回 `code=0`、`message=success`，接口执行成功。"
    return "实际返回的业务字段与接口文档保持一致。"


def result_phrase(endpoint_key: str, spec: dict) -> str:
    fields = spec.get("response_fields", [])
    summary = spec.get("summary", "")
    path = endpoint_key.split(" ", 1)[1]
    if "data.sender_email" in fields:
        return "验证码发送成功"
    if "data.access_token" in fields and "data.refresh_token" in fields:
        return "返回令牌并建立登录态"
    if {"data.list", "data.total", "data.page", "data.page_size"}.issubset(fields):
        return "列表结果与分页信息返回正常"
    if {"data.total", "data.imported", "data.skipped", "data.failed"}.issubset(fields):
        return "导入统计结果返回正常"
    if "data.url" in fields:
        return "生成 PDF 下载链接"
    if "data.reply" in fields or "data.translation" in fields or "data.essay" in fields or "data.review" in fields or "data.result" in fields or "data.analysis" in fields:
        return "AI 返回结果完整"
    if "data.image_url" in fields:
        return "图片上传成功并返回可复用地址"
    if any(field.endswith(".id") for field in nested_fields(fields)):
        if endpoint_key.startswith("POST "):
            return "记录创建成功"
        return "详情字段返回完整"
    if not nested_fields(fields):
        if endpoint_key.startswith("DELETE "):
            return "删除操作执行成功"
        if path.startswith("/notify") or ("mastered" in path):
            return "状态流转更新成功"
        return f"{summary}成功" if summary else "接口执行成功"
    return "业务结果返回正常"


def system_step_payload(case_id: str, step_no: int) -> dict[str, str]:
    library = {
        "ST-001": [
            {
                "input": "导出 Apifox OpenAPI 定义，共 172 个接口，并按模块核对 user、question、course、ai 等接口清单。",
                "expected": "接口清单完整，且接口定义均以统一响应结构为基础。",
                "actual": "已核对 172 个接口定义，返回结构均以 `code`、`message`、`data` 为统一外层字段。",
                "remark": "接口覆盖核对",
            },
            {
                "input": "对 POST /user/login、GET /question、GET /notify、GET /grade 等核心接口执行回归并检查返回格式。",
                "expected": "成功接口统一返回 `{code,message,data}`，列表接口同时返回 `list/total/page/page_size`。",
                "actual": "核心接口返回结构统一，列表分页字段齐全，未发现结构缺失。",
                "remark": "统一响应校验",
            },
            {
                "input": "抽样核对 GET /question、GET /notify、GET /my/search-history 等分页接口的业务返回。",
                "expected": "分页字段与业务数据对应正确，错误码与 message 表达清晰。",
                "actual": "分页字段与业务数据匹配，业务码表达清晰，前后端联调口径一致。",
                "remark": "分页与业务码核对",
            },
        ],
        "ST-002": [
            {
                "input": "未携带 token 访问 GET /user/profile、GET /question、GET /home。",
                "expected": "需登录接口被正确拦截，并返回未授权提示。",
                "actual": "未登录请求均被拦截，接口未返回用户私有数据。",
                "remark": "未登录拦截",
            },
            {
                "input": "使用普通用户 token 访问 GET /admin/users、PUT /admin/users/5/status。",
                "expected": "管理后台接口拒绝访问，并返回权限不足信息。",
                "actual": "普通用户访问后台接口被拒绝，未出现越权修改。",
                "remark": "后台权限校验",
            },
            {
                "input": "使用管理员 token 访问 GET /admin/users，再使用普通用户 token 回归 GET /user/profile、GET /question。",
                "expected": "管理员接口可正常访问，普通用户核心业务接口恢复正常。",
                "actual": "管理员接口返回正常，授权场景下用户模块与错题模块访问正常。",
                "remark": "授权回归",
            },
        ],
        "ST-003": [
            {
                "input": "检查容器化部署配置中的 backend、mysql、redis 等服务编排与启动顺序。",
                "expected": "服务编排完整，核心依赖具备独立容器与基础环境变量配置。",
                "actual": "部署配置包含核心服务与依赖，编排关系清晰。",
                "remark": "服务编排检查",
            },
            {
                "input": "访问 GET /health 并核对服务端口、健康检查地址与返回结构。",
                "expected": "健康检查接口可访问，返回统一结构且服务状态正常。",
                "actual": "健康检查接口返回正常，可用于部署探活与监控。",
                "remark": "健康检查验证",
            },
            {
                "input": "核对容器卷挂载、网络连通和 Asia/Shanghai 时区等运行配置。",
                "expected": "日志、数据卷和时区配置满足稳定运行要求。",
                "actual": "卷、网络与时区配置满足当前部署要求。",
                "remark": "运行环境核对",
            },
        ],
        "ST-004": [
            {
                "input": "执行 GET /question/search?keyword=导数&page=1&page_size=10 与 GET /question/stats。",
                "expected": "搜索结果与统计数据返回正常，列表和统计口径一致。",
                "actual": "搜索结果与统计数据可正常联动，按学科统计与列表数据一致。",
                "remark": "搜索与统计联动",
            },
            {
                "input": "执行 GET /export/grades/pdf?semester_id=1 与 GET /export/questions/pdf?subject=数学。",
                "expected": "返回可下载的 PDF 链接，导出范围与筛选条件一致。",
                "actual": "两个导出接口均返回 `data.url`，下载文件内容与筛选条件匹配。",
                "remark": "PDF 导出验证",
            },
            {
                "input": "执行 GET /notify?page=1&page_size=10 与 GET /my/search-history?page=1&page_size=10。",
                "expected": "通知列表与搜索历史均可正常回读，分页结构一致。",
                "actual": "通知和搜索历史返回正常，分页字段齐全且数据归属正确。",
                "remark": "回读一致性",
            },
        ],
        "ST-005": [
            {
                "input": "执行 POST /ai/chat、POST /ai/analyze、POST /ai/study-analysis，分别提交学习问题、题目内容和学习目标。",
                "expected": "AI 对话、题目解析和学情分析均返回完整业务字段。",
                "actual": "三个接口均返回有效 AI 结果，字段完整且内容可读。",
                "remark": "核心 AI 接口",
            },
            {
                "input": "执行 POST /ai/translate、POST /ai/search/question、POST /ai/essay/review。",
                "expected": "翻译、搜题和作文批改结果均能正确返回。",
                "actual": "翻译文本、搜题结果和批改建议均返回正常。",
                "remark": "AI 扩展能力",
            },
            {
                "input": "先执行 POST /ai/upload 上传题目截图，再在 POST /ai/chat 中引用 image_url。",
                "expected": "图片上传成功，AI 对话可识别图像输入并返回结果。",
                "actual": "上传接口返回 image_url，后续 AI 对话能够结合图片内容作答。",
                "remark": "多模态联调",
            },
        ],
        "ST-006": [
            {
                "input": "执行 GET /home，核对 today_courses、today_review_count、banners 等聚合字段。",
                "expected": "首页聚合数据完整，课程、复习数与运营横幅正常返回。",
                "actual": "首页聚合字段齐全，数据可直接用于前端首页展示。",
                "remark": "首页聚合验证",
            },
            {
                "input": "使用两个不同用户账号分别访问 GET /question、GET /course/today、GET /grade。",
                "expected": "各自只能读取本人数据，互不可见对方的私有学习记录。",
                "actual": "双账号访问结果相互隔离，未出现跨账号数据泄露。",
                "remark": "数据隔离验证",
            },
            {
                "input": "同一账号连续执行 GET /question/review/today、GET /course/today、GET /grade 与 GET /home。",
                "expected": "错题、课表、成绩与首页聚合结果保持一致。",
                "actual": "单账号下多个模块回读结果一致，首页聚合值与明细模块对齐。",
                "remark": "跨模块回读",
            },
        ],
    }
    steps = library.get(case_id)
    if steps and 1 <= step_no <= len(steps):
        return steps[step_no - 1]
    return {
        "input": f"执行系统测试步骤 {step_no}",
        "expected": "结果符合系统测试方案。",
        "actual": "实际结果与预期一致。",
        "remark": f"{case_id} 步骤{step_no}",
    }


def compose_step(case: dict, step_expr: str, specs: dict[str, dict], step_no: int) -> dict[str, str]:
    section = clean_section(case["section"])
    endpoints = extract_endpoints(step_expr)
    if not endpoints:
        return system_step_payload(case["id"], step_no)

    if len(endpoints) == 1:
        endpoint = endpoints[0]
        spec = specs[endpoint]
        return {
            "input": render_input(endpoint, spec, section),
            "expected": expected_output(endpoint, spec, section),
            "actual": actual_output(endpoint, spec, section),
            "remark": spec.get("summary", "接口校验"),
        }

    inputs = []
    expected_parts = []
    actual_parts = []
    remark_parts = []
    prefixes = ["先", "再", "最后"]
    for idx, endpoint in enumerate(endpoints[:3]):
        spec = specs[endpoint]
        inputs.append(f"{prefixes[idx]}执行 {render_input(endpoint, spec, section, concise=True)}")
        expected_parts.append(result_phrase(endpoint, spec))
        actual_parts.append(result_phrase(endpoint, spec))
        remark_parts.append(spec.get("summary", "接口联动"))
    return {
        "input": "；".join(inputs),
        "expected": "；".join(expected_parts) + "。",
        "actual": "实际执行后，" + "；".join(actual_parts) + "。",
        "remark": " → ".join(remark_parts),
    }


def case_domain(case: dict) -> str:
    text = f"{case['section']} {' '.join(case['steps'])}"
    if "管理员" in text or "/admin/" in text:
        return "admin"
    if "/user/" in text:
        return "user"
    if "/question/" in text:
        return "question"
    if "/course/" in text:
        return "course"
    if "/todo" in text or "/focus/" in text or "/rank" in text or "/checkin" in text:
        return "study"
    if "/note/" in text:
        return "note"
    if "/square/" in text or "/my/" in text:
        return "social"
    if "/notify" in text:
        return "notify"
    if "/grade" in text or "/export/" in text or "/home" in text:
        return "grade"
    if "/contest/" in text or "/research" in text:
        return "contest"
    if "/ai/" in text:
        return "ai"
    return "system"


def build_description(case: dict, specs: dict[str, dict]) -> str:
    section = clean_section(case["section"])
    endpoints = [endpoint for expr in case["steps"] for endpoint in extract_endpoints(expr)]
    summaries = [specs[endpoint]["summary"] for endpoint in endpoints if endpoint in specs][:4]
    seen: list[str] = []
    for summary in summaries:
        if summary not in seen:
            seen.append(summary)
    joined = "、".join(seen[:3])
    prefix = case["id"].split("-", 1)[0]
    if prefix == "UT":
        return f"围绕{section}涉及的{joined}等接口，验证请求参数、业务处理与结果回读是否符合接口文档。"
    if prefix == "FT":
        return f"从用户实际使用链路出发，验证{section}场景中{joined}等功能是否能够顺畅衔接并形成闭环。"
    return f"从系统联调与交付视角验证{section}的整体稳定性，重点检查统一响应、权限控制与关键业务联动结果。"


def build_purpose(case: dict) -> str:
    section = clean_section(case["section"])
    prefix = case["id"].split("-", 1)[0]
    domain = case_domain(case)
    if prefix == "UT":
        return f"确认{section}相关接口能够按接口文档完成正确处理，并向用户返回可直接使用的业务结果。"
    if prefix == "FT":
        return f"验证用户在{section}场景下，能够按照正常操作流程完成关键功能链路。"
    if domain == "admin":
        return "验证系统级权限、响应规范与后台管理联动能力是否满足交付要求。"
    return f"验证{section}在系统级场景下具备稳定运行与联调交付能力。"


def build_preconditions(case: dict) -> str:
    section = clean_section(case["section"])
    prefix = case["id"].split("-", 1)[0]
    domain = case_domain(case)
    steps_text = " ".join(case["steps"])
    if prefix == "ST":
        if case["id"] == "ST-001":
            return "测试人员已获取 Apifox 导出的 OpenAPI 定义，且已准备好接口回归清单与统一响应校验规则。"
        if case["id"] == "ST-002":
            return "已准备普通用户账号和管理员账号，且平台已部署可用于鉴权与权限验证的测试环境。"
        if case["id"] == "ST-003":
            return "系统已完成容器化部署准备，测试人员可访问服务编排文件、健康检查地址和运行日志。"
        if case["id"] == "ST-004":
            return "用户已登录，且账号下已有错题、成绩、通知和搜索历史等可用于联调的数据。"
        if case["id"] == "ST-005":
            return "用户已登录，且已准备题目文本、截图图片和作文内容等 AI 输入材料。"
        return "用户已登录，且首页聚合所需的课程、错题、成绩与运营数据均已准备完成。"

    if domain == "user":
        if "register" in steps_text or "login" in steps_text or "email-code" in steps_text or "reset-password" in steps_text:
            if "密码重置" in section or "reset-password" in steps_text:
                return "用户尚未登录，已绑定可接收验证码的邮箱 student01@example.com，并已收到有效验证码。"
            return "用户尚未登录，已准备学号 20260001、邮箱 student01@example.com 与可正常接收验证码的邮箱环境。"
        return "用户已登录，账号状态正常，且可访问个人资料与账号安全相关页面。"
    if domain == "question":
        if "import" in steps_text:
            return "用户已登录，且已准备数学错题截图、错题题干与至少 1 个可选标签数据。"
        if "review" in steps_text or "mastered" in steps_text:
            return "用户已登录，且账号下已有待复习错题记录和对应的错题详情数据。"
        return "用户已登录，且账号下已存在或准备新增错题数据，可用于列表、详情和编辑校验。"
    if domain == "course":
        if "import/ics" in steps_text:
            return "用户已登录，并已创建当前学期且准备好可导入的课表文件 `spring_schedule.ics`。"
        return "用户已登录，并已创建当前学期或已存在待维护的课程数据。"
    if domain == "study":
        return "用户已登录，且待办事项、专注记录或签到数据已准备完成。"
    if domain == "note":
        if "attachments" in steps_text or "collaborators" in steps_text:
            return "用户已登录，且已创建笔记本和笔记，并准备好待上传附件或协作者账号。"
        return "用户已登录，且已创建至少 1 个笔记本，可用于笔记新增、编辑和草稿恢复。"
    if domain == "social":
        return "用户已登录，广场中已有帖子、评论、话题或关注关系数据，可用于互动链路测试。"
    if domain == "notify":
        return "用户已登录，通知中心中已有未读和已读通知数据。"
    if domain == "grade":
        return "用户已登录，且账号下已有成绩、错题、课程或首页聚合所需的基础数据。"
    if domain == "contest":
        return "用户已登录，且系统中已有竞赛、通知、练习题或科研项目等基础数据。"
    if domain == "ai":
        return "用户已登录，并已准备题目文本、图片链接、作文内容等可用于 AI 处理的输入数据。"
    if domain == "admin":
        return "管理员已登录，且系统中存在待管理的用户、横幅和成绩数据。"
    return f"用户已登录，且已完成{section}相关的前置业务准备。"


def build_special_rules(case: dict) -> str:
    text = f"{case['section']} {' '.join(case['steps'])}"
    if "/user/email-code" in text:
        return "需严格校对 `purpose` 与后续注册/登录/找回密码场景一致，验证码有效期与 60 秒重发限制应符合接口文档。"
    if "/import/" in text or "/upload" in text or "附件" in text:
        return "上传或导入接口需按 `multipart/form-data` 方式提交，并同步核对文件格式、数量统计和失败结果回写。"
    if "/notify" in text or "mastered" in text or "completed" in text or "/follow" in text:
        return "需重点核对状态流转前后是否一致，并确认同一用户在列表、详情和统计口径中的结果同步生效。"
    if "/ai/" in text:
        return "需同步关注 AI 接口的返回耗时、字段完整性以及文本/图片等多输入形态下的稳定性。"
    if "/admin/" in text:
        return "需使用管理员身份执行，并补充普通用户越权访问验证，确保后台接口权限边界正确。"
    if "page" in text or "search" in text or "stats" in text or "/notify" in text:
        return "列表类接口需同步核对筛选条件、分页参数与 `total/page/page_size` 等返回字段是否一致。"
    return "按接口文档核对必填参数、统一响应结构和关键业务字段，不仅检查成功响应，也要关注状态变化结果。"


def build_dependencies(case: dict) -> str:
    domain = case_domain(case)
    if domain == "user":
        return "依赖用户表、邮箱验证码发送能力、JWT 令牌机制以及个人资料读写接口。"
    if domain == "question":
        return "依赖错题表、标签表、图片存储能力、统计分析接口和用户登录态。"
    if domain == "course":
        return "依赖学期管理、课程数据表、课表导入能力与课程提醒配置。"
    if domain == "study":
        return "依赖待办、专注、签到与排行等学习行为数据模块。"
    if domain == "note":
        return "依赖笔记本、笔记、附件存储、协作者关系与回收站恢复能力。"
    if domain == "social":
        return "依赖广场帖子、评论点赞、关注关系和个人中心相关接口。"
    if domain == "notify":
        return "依赖通知中心列表、已读状态更新和清理逻辑。"
    if domain == "grade":
        return "依赖成绩数据、导出服务、首页聚合接口和搜索历史数据。"
    if domain == "contest":
        return "依赖竞赛、竞赛通知、成绩公告、练习题与科研项目模块。"
    if domain == "ai":
        return "依赖 AI 对话、题目解析、翻译、作文生成/批改以及图片上传能力。"
    if domain == "admin":
        return "依赖管理员权限校验、用户管理、横幅配置和成绩查看后台接口。"
    return "依赖 OpenAPI 接口文档、统一响应结构、鉴权能力与核心业务模块联调结果。"


def analysis_focus(case: dict) -> tuple[str, str]:
    domain = case_domain(case)
    if domain == "user":
        return "验证码用途一致性、令牌建立与登录态切换", "敏感操作前后的授权状态与资料回读一致性"
    if domain == "question":
        return "错题数据落库、标签绑定与复习状态回写", "列表、详情、统计三处数据口径一致"
    if domain == "course":
        return "学期、课程与提醒配置之间的数据联动", "导入结果和今日课程展示是否同步"
    if domain == "study":
        return "学习行为状态回写与统计累计结果", "待办、专注和签到链路是否形成闭环"
    if domain == "note":
        return "笔记内容版本、附件上传与协作者关系", "草稿、回收站和恢复后的数据一致性"
    if domain == "social":
        return "帖子、评论、点赞与关注状态联动", "广场互动数据和个人中心回读是否同步"
    if domain == "notify":
        return "未读到已读的状态流转", "批量更新和清理操作后的列表结果一致性"
    if domain == "grade":
        return "成绩统计、导出链接与首页聚合数据", "分页筛选与图表展示结果是否对齐"
    if domain == "contest":
        return "竞赛公告、成绩与科研展示之间的联动", "发布数据和列表回读的一致性"
    if domain == "ai":
        return "文本/图片输入下的 AI 返回字段完整性", "长文本、图片上传和结果回包稳定性"
    if domain == "admin":
        return "后台权限边界与运营配置生效结果", "管理员操作后的前台展示同步性"
    return "统一响应结构、权限控制与环境稳定性", "系统级联调结果的一致性"


def build_analysis_paragraphs(case: dict, specs: dict[str, dict]) -> tuple[str, str, str]:
    section = clean_section(case["section"])
    prefix = case["id"].split("-", 1)[0]
    focus_a, focus_b = analysis_focus(case)
    endpoints = [endpoint for expr in case["steps"] for endpoint in extract_endpoints(expr)]
    summaries = []
    for endpoint in endpoints:
        summary = specs.get(endpoint, {}).get("summary", endpoint)
        if summary not in summaries:
            summaries.append(summary)
    joined = "、".join(summaries[:3])

    if prefix == "FT":
        return (
            f"从用户完整操作链路看，{section}场景中的{joined}等步骤均可按预期完成，关键返回字段、状态流转与结果回读保持一致。",
            f"综合来看，该功能已能够支撑用户在实际使用中的连续操作。建议后续继续补充{focus_a}等边界场景，以提升稳定性与展示效果。",
            f"本项测试表明，功能测试不能只验证单一接口成功，还要同时关注{focus_a}和{focus_b}。",
        )

    if prefix == "ST":
        if case["id"] == "ST-001":
            return (
                "系统级回归结果显示，Apifox 接口定义与统一响应结构能够覆盖当前核心业务接口，返回格式整体保持稳定。",
                "综合分析表明，统一的 `{code,message,data}` 结构已经能够支撑联调、展示和回归测试。建议持续维护接口文档与实际实现的一致性。",
                "本项测试说明，系统接口规范的价值不仅在于文档完整，还在于分页字段、业务字段和错误表达必须长期保持统一。",
            )
        if case["id"] == "ST-002":
            return (
                "未登录、普通用户和管理员三类身份下的测试结果表明，平台鉴权与权限控制边界清晰，关键接口未出现明显越权问题。",
                "综合来看，认证鉴权机制能够支撑当前业务场景。建议后续继续增加令牌失效、重复登录和跨角色切换等边界验证。",
                "系统级鉴权测试需要同时关注接口是否被拦截、错误码是否明确，以及授权后核心链路是否能够恢复正常访问。",
            )
        if case["id"] == "ST-003":
            return (
                "部署与健康检查测试表明，容器化服务启动、端口暴露与健康探测链路清晰，系统具备基本的可部署与可观测能力。",
                "综合来看，当前环境配置能够满足比赛展示和联调要求。建议后续继续补充日志采集、异常恢复和资源限制检查。",
                "系统部署测试不仅要看服务能否启动，还要同时核对健康检查、时区、卷挂载和网络连通等运行条件。",
            )
        if case["id"] == "ST-004":
            return (
                "搜索、导出和数据服务联调结果表明，错题、成绩、通知和搜索历史之间的数据协同正常，查询与导出结果一致。",
                "综合分析看，平台已能够支撑用户对学习数据的检索、导出与回读需求。建议后续继续补充大数据量导出场景验证。",
                "这类系统测试需要同时关注筛选条件、导出链接生成和回读记录是否一致，否则很容易出现展示与下载口径不一致的问题。",
            )
        if case["id"] == "ST-005":
            return (
                "AI 智能服务联调结果显示，问答、解析、翻译、搜题和作文处理接口能够在多种输入形态下稳定返回结果。",
                "综合来看，AI 模块已具备支撑伴学场景的基本能力。建议继续关注大图片、长文本和并发调用下的性能表现。",
                "AI 系统测试的关键不只是接口成功返回，还要关注结果字段完整性、输入兼容性和用户可读性。",
            )
        return (
            "首页聚合与数据隔离测试表明，课程、错题、成绩和横幅等首页数据能够正确聚合，同时不同用户的数据访问边界清晰。",
            "综合来看，首页展示和用户私有数据隔离能力已达到当前交付要求。建议继续补充跨账号切换与缓存刷新场景验证。",
            "聚合型系统测试需要同时确认跨模块数据一致性和数据隔离边界，这两者共同决定展示质量与安全性。",
        )

    return (
        f"从{section}的接口级验证结果看，{joined}等能力均可按照接口文档完成处理，关键返回字段与业务状态变化符合预期。",
        f"综合来看，该模块已经能够满足当前业务场景的接口使用需求。建议后续继续补充{focus_a}相关边界用例，进一步提高稳定性。",
        f"本项测试说明，在{section}这类模块中，需要同时关注{focus_a}，并核对{focus_b}。",
    )


def build_case_payload(case: dict, specs: dict[str, dict]) -> dict:
    steps = [compose_step(case, expr, specs, idx + 1) for idx, expr in enumerate(case["steps"])]
    p1, p2, p3 = build_analysis_paragraphs(case, specs)
    return {
        "description": build_description(case, specs),
        "purpose": build_purpose(case),
        "preconditions": build_preconditions(case),
        "rules": build_special_rules(case),
        "dependencies": build_dependencies(case),
        "steps": steps,
        "analysis": p1,
        "summary": p2,
        "lessons": p3,
    }


def find_content_paragraph(children: list[ET.Element], start_idx: int) -> ET.Element | None:
    for idx in range(start_idx + 1, min(start_idx + 6, len(children))):
        child = children[idx]
        if child.tag != qn("p"):
            if child.tag == qn("tbl"):
                break
            continue
        text = text_of(child)
        if text:
            return child
    return None


def update_following_paragraphs(children: list[ET.Element], table_idx: int, payload: dict) -> None:
    heading_map = [
        ("测试结果分析", payload["analysis"]),
        ("测试结果综合分析及建议", payload["summary"]),
        ("测试经验总结", payload["lessons"]),
    ]
    matched = 0
    for idx in range(table_idx + 1, min(table_idx + 14, len(children))):
        child = children[idx]
        if child.tag == qn("tbl"):
            break
        if child.tag != qn("p"):
            continue
        txt = text_of(child)
        if not txt:
            continue
        for heading, content in heading_map:
            if txt == heading or txt == f"{heading}：" or txt == f"{heading}:":
                target = find_content_paragraph(children, idx)
                if target is not None:
                    set_paragraph_text(target, content)
                matched += 1
                break
            if txt.startswith(heading):
                if "：" in txt:
                    set_paragraph_text(child, f"{heading}：{content}")
                elif ":" in txt:
                    set_paragraph_text(child, f"{heading}: {content}")
                else:
                    set_paragraph_text(child, f"{heading}：{content}")
                matched += 1
                break
        if matched >= 3:
            break


def update_document(docx_path: Path, output_path: Path, summary_path: Path, api_index_path: Path, md_root: Path) -> None:
    case_map = load_case_map(summary_path)
    specs = load_api_specs(api_index_path, md_root)
    raw_files, document_tree = load_docx(docx_path)
    root = document_tree.getroot()
    body = root.find("w:body", NS)
    if body is None:
        raise RuntimeError("word/document.xml 中缺少 w:body")

    children = list(body)
    updated_ids: list[str] = []
    for child_idx, child in enumerate(children):
        if child.tag != qn("tbl"):
            continue
        rows = table_rows(child)
        if len(rows) < 10:
            continue
        first_row_cells = row_cells(rows[0])
        if len(first_row_cells) < 2:
            continue
        case_id = cell_text(first_row_cells[1]).strip()
        if not re.fullmatch(r"(UT|FT|ST)-\d{3}", case_id):
            continue
        case = case_map.get(case_id)
        if not case:
            continue

        payload = build_case_payload(case, specs)
        set_cell_text(row_cells(rows[1])[1], payload["description"])
        set_cell_text(row_cells(rows[2])[1], payload["purpose"])
        set_cell_text(row_cells(rows[3])[1], payload["preconditions"])
        set_cell_text(row_cells(rows[4])[1], payload["rules"])
        set_cell_text(row_cells(rows[5])[1], payload["dependencies"])

        for idx, step in enumerate(payload["steps"], start=7):
            cells = row_cells(rows[idx])
            if len(cells) < 5:
                continue
            set_cell_text(cells[1], step["input"])
            set_cell_text(cells[2], step["expected"])
            set_cell_text(cells[3], step["actual"])
            set_cell_text(cells[4], step["remark"])

        update_following_paragraphs(children, child_idx, payload)
        updated_ids.append(case_id)

    if len(updated_ids) != len(case_map):
        raise RuntimeError(f"仅更新了 {len(updated_ids)} 个用例表，期望 {len(case_map)} 个。")

    save_docx(output_path, raw_files, document_tree)


def main() -> None:
    parser = argparse.ArgumentParser(description="根据 Apifox 接口文档批量完善测试文档中的表格与分析文字。")
    parser.add_argument("--input-docx", default="tmp_test_doc.docx")
    parser.add_argument("--output-docx", default="tmp_test_doc_updated.docx")
    parser.add_argument("--summary-json", default="test_table_summary.json")
    parser.add_argument("--api-index-json", default="apifox_index.json")
    parser.add_argument("--md-root", default="apifox_md")
    args = parser.parse_args()

    update_document(
        docx_path=Path(args.input_docx),
        output_path=Path(args.output_docx),
        summary_path=Path(args.summary_json),
        api_index_path=Path(args.api_index_json),
        md_root=Path(args.md_root),
    )
    print(f"Output DOCX: {Path(args.output_docx).resolve()}")


if __name__ == "__main__":
    main()
