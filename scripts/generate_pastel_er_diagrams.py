from __future__ import annotations

import html
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


PALETTE = {
    "bg": "#f7f8fc",
    "grid": "#e8edf5",
    "title_fill": "#496b8d",
    "title_stroke": "#395672",
    "title_text": "#f8fbff",
    "entity_fill": "#f3dbe7",
    "entity_stroke": "#c99fb1",
    "attribute_fill": "#dceaf8",
    "attribute_stroke": "#8fb2d3",
    "relationship_fill": "#e8e0f7",
    "relationship_stroke": "#ab9ec9",
    "line": "#6c7587",
    "cardinality": "#cf6b7a",
    "text": "#243447",
    "muted": "#607085",
}

FONT_FAMILY = "'Microsoft YaHei','PingFang SC','Noto Sans SC',sans-serif"


@dataclass
class Box:
    x: float
    y: float
    w: float
    h: float

    @property
    def cx(self) -> float:
        return self.x + self.w / 2

    @property
    def cy(self) -> float:
        return self.y + self.h / 2


def escape(value: str) -> str:
    return html.escape(value, quote=True)


def estimate_text_width(text: str, font_size: float) -> float:
    return max(font_size * 2.4, len(text) * font_size * 0.58)


def point_on_rect(box: Box, target_x: float, target_y: float) -> tuple[float, float]:
    dx = target_x - box.cx
    dy = target_y - box.cy
    if dx == 0 and dy == 0:
        return box.cx, box.cy

    half_w = box.w / 2
    half_h = box.h / 2
    scale_x = float("inf") if dx == 0 else half_w / abs(dx)
    scale_y = float("inf") if dy == 0 else half_h / abs(dy)
    scale = min(scale_x, scale_y)
    return box.cx + dx * scale, box.cy + dy * scale


def point_on_diamond(box: Box, target_x: float, target_y: float) -> tuple[float, float]:
    dx = target_x - box.cx
    dy = target_y - box.cy
    if dx == 0 and dy == 0:
        return box.cx, box.cy

    half_w = box.w / 2
    half_h = box.h / 2
    scale = 1 / ((abs(dx) / half_w) + (abs(dy) / half_h))
    return box.cx + dx * scale, box.cy + dy * scale


def add_text(
    parts: list[str],
    x: float,
    y: float,
    text: str,
    font_size: float,
    *,
    fill: str | None = None,
    anchor: str = "middle",
    weight: int = 500,
    dy: float = 0,
) -> None:
    parts.append(
        f"<text x='{x:.1f}' y='{y + dy:.1f}' text-anchor='{anchor}' "
        f"font-family={escape(FONT_FAMILY)!r} font-size='{font_size:.1f}' "
        f"font-weight='{weight}' fill='{fill or PALETTE['text']}'>{escape(text)}</text>"
    )


def draw_background(parts: list[str], width: int, height: int) -> None:
    parts.append(
        "<defs>"
        f"<pattern id='grid' width='56' height='56' patternUnits='userSpaceOnUse'>"
        f"<rect width='56' height='56' fill='{PALETTE['bg']}' />"
        f"<path d='M 56 0 L 0 0 0 56' fill='none' stroke='{PALETTE['grid']}' stroke-width='1' />"
        "</pattern>"
        "</defs>"
    )
    parts.append(f"<rect width='{width}' height='{height}' fill='url(#grid)' />")


def draw_title(parts: list[str], width: int, title: str, subtitle: str) -> None:
    title_box = Box(width / 2 - 560, 40, 1120, 110)
    parts.append(
        f"<rect x='{title_box.x:.1f}' y='{title_box.y:.1f}' width='{title_box.w:.1f}' height='{title_box.h:.1f}' "
        f"rx='22' fill='{PALETTE['title_fill']}' stroke='{PALETTE['title_stroke']}' stroke-width='2.5' />"
    )
    add_text(parts, title_box.cx, title_box.y + 44, title, 30, fill=PALETTE["title_text"], weight=700)
    add_text(parts, title_box.cx, title_box.y + 82, subtitle, 18, fill="#dfeaf5", weight=500)


def draw_legend(parts: list[str], width: int, y: int = 90) -> None:
    x = width - 360
    legend = Box(x, y, 280, 260)
    parts.append(
        f"<rect x='{legend.x:.1f}' y='{legend.y:.1f}' width='{legend.w:.1f}' height='{legend.h:.1f}' "
        "rx='18' fill='#ffffffdd' stroke='#90a0b7' stroke-width='1.8' />"
    )
    add_text(parts, legend.cx, legend.y + 34, "图例", 20, weight=700)
    parts.append(
        f"<rect x='{legend.x + 36:.1f}' y='{legend.y + 62:.1f}' width='170' height='42' rx='12' "
        f"fill='{PALETTE['entity_fill']}' stroke='{PALETTE['entity_stroke']}' stroke-width='1.6' />"
    )
    add_text(parts, legend.x + 121, legend.y + 90, "实体", 18, weight=700)
    parts.append(
        f"<ellipse cx='{legend.x + 121:.1f}' cy='{legend.y + 144:.1f}' rx='82' ry='24' "
        f"fill='{PALETTE['attribute_fill']}' stroke='{PALETTE['attribute_stroke']}' stroke-width='1.6' />"
    )
    add_text(parts, legend.x + 121, legend.y + 150, "属性", 18, weight=700)
    diamond_x = legend.x + 121
    diamond_y = legend.y + 208
    parts.append(
        f"<polygon points='{diamond_x:.1f},{diamond_y - 28:.1f} {diamond_x + 82:.1f},{diamond_y:.1f} "
        f"{diamond_x:.1f},{diamond_y + 28:.1f} {diamond_x - 82:.1f},{diamond_y:.1f}' "
        f"fill='{PALETTE['relationship_fill']}' stroke='{PALETTE['relationship_stroke']}' stroke-width='1.6' />"
    )
    add_text(parts, diamond_x, diamond_y + 5, "关系", 18, weight=700)


def attribute_side_distribution(attributes: list[str]) -> dict[str, list[str]]:
    groups = {"top": [], "right": [], "bottom": [], "left": []}
    for index, attr in enumerate(attributes):
        side = ("left", "right", "top", "bottom")[index % 4]
        groups[side].append(attr)
    return groups


def draw_entity(parts: list[str], name: str, box: Box, attributes: list[str], side_map: dict[str, list[str]] | None = None) -> dict[str, object]:
    side_map = side_map or attribute_side_distribution(attributes)
    parts.append(
        f"<rect x='{box.x:.1f}' y='{box.y:.1f}' width='{box.w:.1f}' height='{box.h:.1f}' rx='20' "
        f"fill='{PALETTE['entity_fill']}' stroke='{PALETTE['entity_stroke']}' stroke-width='2.2' />"
    )
    font_size = 28 if len(name) <= 10 else 24
    add_text(parts, box.cx, box.cy + 10, name, font_size, weight=700)

    attr_boxes: dict[str, Box] = {}
    side_gap = 82
    line_gap = 80

    def draw_side(side: str, items: list[str]) -> None:
        if not items:
            return
        if side in {"left", "right"}:
            total_span = (len(items) - 1) * line_gap
            start_y = box.cy - total_span / 2
            for idx, label in enumerate(items):
                cy = start_y + idx * line_gap
                rx = max(72, estimate_text_width(label, 18) / 2 + 20)
                ry = 26
                cx = box.x - side_gap - rx if side == "left" else box.x + box.w + side_gap + rx
                attr_box = Box(cx - rx, cy - ry, rx * 2, ry * 2)
                attr_boxes[label] = attr_box
                rect_point = point_on_rect(box, cx, cy)
                parts.append(
                    f"<line x1='{rect_point[0]:.1f}' y1='{rect_point[1]:.1f}' "
                    f"x2='{cx:.1f}' y2='{cy:.1f}' stroke='{PALETTE['line']}' stroke-width='1.7' />"
                )
                parts.append(
                    f"<ellipse cx='{cx:.1f}' cy='{cy:.1f}' rx='{rx:.1f}' ry='{ry:.1f}' "
                    f"fill='{PALETTE['attribute_fill']}' stroke='{PALETTE['attribute_stroke']}' stroke-width='1.6' />"
                )
                add_text(parts, cx, cy + 6, label, 18, weight=600)
        else:
            total_span = (len(items) - 1) * 188
            start_x = box.cx - total_span / 2
            for idx, label in enumerate(items):
                cx = start_x + idx * 188
                rx = max(72, estimate_text_width(label, 18) / 2 + 18)
                ry = 26
                cy = box.y - side_gap - ry if side == "top" else box.y + box.h + side_gap + ry
                attr_box = Box(cx - rx, cy - ry, rx * 2, ry * 2)
                attr_boxes[label] = attr_box
                rect_point = point_on_rect(box, cx, cy)
                parts.append(
                    f"<line x1='{rect_point[0]:.1f}' y1='{rect_point[1]:.1f}' "
                    f"x2='{cx:.1f}' y2='{cy:.1f}' stroke='{PALETTE['line']}' stroke-width='1.7' />"
                )
                parts.append(
                    f"<ellipse cx='{cx:.1f}' cy='{cy:.1f}' rx='{rx:.1f}' ry='{ry:.1f}' "
                    f"fill='{PALETTE['attribute_fill']}' stroke='{PALETTE['attribute_stroke']}' stroke-width='1.6' />"
                )
                add_text(parts, cx, cy + 6, label, 18, weight=600)

    for side in ("top", "right", "bottom", "left"):
        draw_side(side, side_map.get(side, []))

    return {"name": name, "box": box, "attrs": attr_boxes}


def draw_relationship(
    parts: list[str],
    name: str,
    box: Box,
    connections: list[dict[str, object]],
    entities: dict[str, dict[str, object]],
) -> None:
    points = (
        f"{box.cx:.1f},{box.y:.1f} {box.x + box.w:.1f},{box.cy:.1f} "
        f"{box.cx:.1f},{box.y + box.h:.1f} {box.x:.1f},{box.cy:.1f}"
    )
    parts.append(
        f"<polygon points='{points}' fill='{PALETTE['relationship_fill']}' "
        f"stroke='{PALETTE['relationship_stroke']}' stroke-width='2' />"
    )
    add_text(parts, box.cx, box.cy + 6, name, 20, weight=700)

    for conn in connections:
        entity_name = str(conn["entity"])
        entity_box: Box = entities[entity_name]["box"]  # type: ignore[index]
        entity_point = point_on_rect(entity_box, box.cx, box.cy)
        diamond_point = point_on_diamond(box, entity_box.cx, entity_box.cy)
        parts.append(
            f"<line x1='{diamond_point[0]:.1f}' y1='{diamond_point[1]:.1f}' "
            f"x2='{entity_point[0]:.1f}' y2='{entity_point[1]:.1f}' "
            f"stroke='{PALETTE['line']}' stroke-width='2' />"
        )
        cardinality = str(conn.get("cardinality", ""))
        if cardinality:
            tx = entity_point[0] * 0.78 + diamond_point[0] * 0.22
            ty = entity_point[1] * 0.78 + diamond_point[1] * 0.22 - 8
            add_text(parts, tx, ty, cardinality, 18, fill=PALETTE["cardinality"], weight=700)
        via_text = str(conn.get("via", "") or "")
        if via_text:
            tx = entity_point[0] * 0.40 + diamond_point[0] * 0.60
            ty = entity_point[1] * 0.40 + diamond_point[1] * 0.60 - 14
            add_text(parts, tx, ty, via_text, 15, fill=PALETTE["muted"], weight=600)


def draw_loop_relationship(parts: list[str], entity_box: Box, diamond_box: Box, label: str, near_cardinality: str, far_cardinality: str) -> None:
    points = (
        f"{diamond_box.cx:.1f},{diamond_box.y:.1f} {diamond_box.x + diamond_box.w:.1f},{diamond_box.cy:.1f} "
        f"{diamond_box.cx:.1f},{diamond_box.y + diamond_box.h:.1f} {diamond_box.x:.1f},{diamond_box.cy:.1f}"
    )
    parts.append(
        f"<polygon points='{points}' fill='{PALETTE['relationship_fill']}' "
        f"stroke='{PALETTE['relationship_stroke']}' stroke-width='2' />"
    )
    add_text(parts, diamond_box.cx, diamond_box.cy + 6, label, 20, weight=700)

    start_x = entity_box.x + entity_box.w
    start_y = entity_box.cy - 32
    end_x = entity_box.x + entity_box.w
    end_y = entity_box.cy + 32
    mid_x = diamond_box.cx + 110
    top_y = entity_box.y + 40
    bottom_y = entity_box.y + entity_box.h - 40
    path = (
        f"M {start_x:.1f} {start_y:.1f} "
        f"C {mid_x - 40:.1f} {start_y:.1f}, {mid_x:.1f} {top_y:.1f}, {mid_x:.1f} {diamond_box.cy:.1f} "
        f"S {mid_x - 24:.1f} {bottom_y:.1f}, {end_x:.1f} {end_y:.1f}"
    )
    parts.append(f"<path d='{path}' fill='none' stroke='{PALETTE['line']}' stroke-width='2' />")
    parts.append(
        f"<line x1='{diamond_box.cx - diamond_box.w / 2:.1f}' y1='{diamond_box.cy:.1f}' "
        f"x2='{mid_x:.1f}' y2='{diamond_box.cy:.1f}' stroke='{PALETTE['line']}' stroke-width='2' />"
    )
    add_text(parts, start_x + 30, start_y - 14, near_cardinality, 18, fill=PALETTE["cardinality"], weight=700)
    add_text(parts, end_x + 30, end_y + 4, far_cardinality, 18, fill=PALETTE["cardinality"], weight=700)


def render_svg(diagram: dict[str, object], output_file: Path) -> None:
    width = int(diagram["width"])
    height = int(diagram["height"])
    parts = [
        f"<svg xmlns='http://www.w3.org/2000/svg' width='{width}' height='{height}' viewBox='0 0 {width} {height}'>"
    ]
    draw_background(parts, width, height)
    draw_title(parts, width, str(diagram["title"]), str(diagram["subtitle"]))
    draw_legend(parts, width)

    entities: dict[str, dict[str, object]] = {}
    for entity in diagram["entities"]:  # type: ignore[assignment]
        entity_name = entity["name"]
        box = Box(entity["x"], entity["y"], entity["w"], entity["h"])
        entities[entity_name] = draw_entity(
            parts,
            entity_name,
            box,
            entity["attributes"],
            entity.get("sides"),
        )

    for relationship in diagram["relationships"]:  # type: ignore[assignment]
        box = Box(relationship["x"], relationship["y"], relationship["w"], relationship["h"])
        draw_relationship(parts, relationship["name"], box, relationship["connections"], entities)

    for loop in diagram.get("loops", []):  # type: ignore[assignment]
        entity_box: Box = entities[loop["entity"]]["box"]  # type: ignore[index]
        diamond_box = Box(loop["x"], loop["y"], loop["w"], loop["h"])
        draw_loop_relationship(
            parts,
            entity_box,
            diamond_box,
            loop["name"],
            loop["near_cardinality"],
            loop["far_cardinality"],
        )

    parts.append("</svg>")
    output_file.write_text("\n".join(parts), encoding="utf-8")


def build_diagrams() -> list[dict[str, object]]:
    users_attrs = [
        "student_id",
        "password",
        "password_changed",
        "nickname",
        "avatar",
        "phone",
        "email",
        "role",
        "last_login",
        "status",
        "created_at",
        "updated_at",
        "deleted_at",
        "id",
    ]

    return [
        {
            "slug": "diagram-01-notes",
            "title": "图 1  笔记模块 ER 图",
            "subtitle": "users / notebooks / notes",
            "width": 2800,
            "height": 1780,
            "entities": [
                {
                    "name": "users",
                    "x": 1120,
                    "y": 260,
                    "w": 240,
                    "h": 86,
                    "attributes": users_attrs,
                    "sides": {
                        "left": ["student_id", "password", "password_changed", "nickname"],
                        "right": ["avatar", "phone", "email", "role"],
                        "top": ["last_login", "status", "created_at"],
                        "bottom": ["updated_at", "deleted_at", "id"],
                    },
                },
                {
                    "name": "notebooks",
                    "x": 300,
                    "y": 1040,
                    "w": 260,
                    "h": 86,
                    "attributes": ["owner_id", "type", "subject", "title", "created_at", "updated_at", "deleted_at", "id"],
                    "sides": {
                        "left": ["owner_id", "type"],
                        "right": ["subject", "title"],
                        "top": ["created_at", "updated_at"],
                        "bottom": ["deleted_at", "id"],
                    },
                },
                {
                    "name": "notes",
                    "x": 2040,
                    "y": 1040,
                    "w": 240,
                    "h": 86,
                    "attributes": [
                        "notebook_id",
                        "owner_id",
                        "title",
                        "content",
                        "status",
                        "version",
                        "created_at",
                        "updated_at",
                        "deleted_at",
                        "id",
                    ],
                    "sides": {
                        "left": ["notebook_id", "owner_id", "title"],
                        "right": ["content", "status", "version"],
                        "top": ["created_at", "updated_at"],
                        "bottom": ["deleted_at", "id"],
                    },
                },
            ],
            "relationships": [
                {
                    "name": "拥有",
                    "x": 660,
                    "y": 640,
                    "w": 150,
                    "h": 90,
                    "connections": [
                        {"entity": "users", "cardinality": "1"},
                        {"entity": "notebooks", "cardinality": "n", "via": "owner_id"},
                    ],
                },
                {
                    "name": "创建",
                    "x": 1820,
                    "y": 640,
                    "w": 150,
                    "h": 90,
                    "connections": [
                        {"entity": "users", "cardinality": "1"},
                        {"entity": "notes", "cardinality": "n", "via": "owner_id"},
                    ],
                },
                {
                    "name": "包含",
                    "x": 1240,
                    "y": 1260,
                    "w": 150,
                    "h": 90,
                    "connections": [
                        {"entity": "notebooks", "cardinality": "1"},
                        {"entity": "notes", "cardinality": "n", "via": "notebook_id"},
                    ],
                },
            ],
        },
        {
            "slug": "diagram-02-productivity-social",
            "title": "图 2  动态 / 专注 / 通知模块 ER 图",
            "subtitle": "post_topics / posts / users / todos / focus_sessions / notifications",
            "width": 3200,
            "height": 2120,
            "entities": [
                {
                    "name": "post_topics",
                    "x": 160,
                    "y": 760,
                    "w": 280,
                    "h": 86,
                    "attributes": ["name", "description", "created_at", "updated_at", "deleted_at", "id"],
                    "sides": {
                        "left": ["name", "description"],
                        "right": ["created_at", "updated_at"],
                        "bottom": ["deleted_at", "id"],
                    },
                },
                {
                    "name": "posts",
                    "x": 760,
                    "y": 760,
                    "w": 230,
                    "h": 86,
                    "attributes": [
                        "user_id",
                        "topic_id",
                        "title",
                        "content",
                        "visibility",
                        "status",
                        "ref_type",
                        "ref_id",
                        "created_at",
                        "updated_at",
                        "deleted_at",
                        "id",
                    ],
                    "sides": {
                        "left": ["user_id", "topic_id", "title"],
                        "right": ["content", "visibility", "status"],
                        "top": ["ref_type", "ref_id", "created_at"],
                        "bottom": ["updated_at", "deleted_at", "id"],
                    },
                },
                {
                    "name": "users",
                    "x": 1420,
                    "y": 650,
                    "w": 240,
                    "h": 86,
                    "attributes": users_attrs,
                    "sides": {
                        "left": ["student_id", "password", "password_changed", "nickname"],
                        "right": ["avatar", "phone", "email", "role"],
                        "top": ["last_login", "status", "created_at"],
                        "bottom": ["updated_at", "deleted_at", "id"],
                    },
                },
                {
                    "name": "todos",
                    "x": 1890,
                    "y": 280,
                    "w": 230,
                    "h": 86,
                    "attributes": [
                        "user_id",
                        "title",
                        "description",
                        "due_at",
                        "status",
                        "completed_at",
                        "created_at",
                        "updated_at",
                        "deleted_at",
                        "id",
                    ],
                    "sides": {
                        "left": ["user_id", "title", "description"],
                        "right": ["due_at", "status", "completed_at"],
                        "bottom": ["created_at", "updated_at", "deleted_at", "id"],
                    },
                },
                {
                    "name": "focus_sessions",
                    "x": 2480,
                    "y": 760,
                    "w": 320,
                    "h": 86,
                    "attributes": [
                        "user_id",
                        "todo_id",
                        "started_at",
                        "ended_at",
                        "duration_s",
                        "status",
                        "created_at",
                        "updated_at",
                        "deleted_at",
                        "id",
                    ],
                    "sides": {
                        "left": ["user_id", "todo_id", "started_at"],
                        "right": ["ended_at", "duration_s", "status"],
                        "top": ["created_at", "updated_at"],
                        "bottom": ["deleted_at", "id"],
                    },
                },
                {
                    "name": "notifications",
                    "x": 1360,
                    "y": 1450,
                    "w": 360,
                    "h": 86,
                    "attributes": [
                        "user_id",
                        "type",
                        "title",
                        "content",
                        "from_user_id",
                        "post_id",
                        "is_read",
                        "read_at",
                        "created_at",
                        "updated_at",
                        "deleted_at",
                        "id",
                    ],
                    "sides": {
                        "left": ["user_id", "type", "title"],
                        "right": ["content", "from_user_id", "post_id"],
                        "top": ["is_read", "read_at", "created_at"],
                        "bottom": ["updated_at", "deleted_at", "id"],
                    },
                },
            ],
            "relationships": [
                {
                    "name": "分类",
                    "x": 520,
                    "y": 760,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "post_topics", "cardinality": "1"},
                        {"entity": "posts", "cardinality": "n", "via": "topic_id"},
                    ],
                },
                {
                    "name": "发布",
                    "x": 1120,
                    "y": 760,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "posts", "cardinality": "n", "via": "user_id"},
                        {"entity": "users", "cardinality": "1"},
                    ],
                },
                {
                    "name": "创建",
                    "x": 1770,
                    "y": 470,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "users", "cardinality": "1"},
                        {"entity": "todos", "cardinality": "n", "via": "user_id"},
                    ],
                },
                {
                    "name": "发起",
                    "x": 2230,
                    "y": 760,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "users", "cardinality": "1"},
                        {"entity": "focus_sessions", "cardinality": "n", "via": "user_id"},
                    ],
                },
                {
                    "name": "关联",
                    "x": 2330,
                    "y": 470,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "todos", "cardinality": "1"},
                        {"entity": "focus_sessions", "cardinality": "n", "via": "todo_id"},
                    ],
                },
                {
                    "name": "接收",
                    "x": 1530,
                    "y": 1140,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "users", "cardinality": "1"},
                        {"entity": "notifications", "cardinality": "n", "via": "user_id"},
                    ],
                },
                {
                    "name": "发送",
                    "x": 1210,
                    "y": 1140,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "users", "cardinality": "1"},
                        {"entity": "notifications", "cardinality": "n", "via": "from_user_id"},
                    ],
                },
                {
                    "name": "关联帖子",
                    "x": 1080,
                    "y": 1360,
                    "w": 180,
                    "h": 90,
                    "connections": [
                        {"entity": "posts", "cardinality": "1"},
                        {"entity": "notifications", "cardinality": "n", "via": "post_id"},
                    ],
                },
            ],
        },
        {
            "slug": "diagram-03-social-interaction",
            "title": "图 3  关注 / 评论模块 ER 图",
            "subtitle": "user_follows / users / posts / post_topics / post_comments",
            "width": 3200,
            "height": 2000,
            "entities": [
                {
                    "name": "user_follows",
                    "x": 120,
                    "y": 760,
                    "w": 320,
                    "h": 86,
                    "attributes": ["follower_id", "following_id", "created_at", "updated_at", "deleted_at", "id"],
                    "sides": {
                        "left": ["follower_id", "following_id"],
                        "right": ["created_at", "updated_at"],
                        "bottom": ["deleted_at", "id"],
                    },
                },
                {
                    "name": "users",
                    "x": 1020,
                    "y": 270,
                    "w": 240,
                    "h": 86,
                    "attributes": users_attrs,
                    "sides": {
                        "left": ["student_id", "password", "password_changed", "nickname"],
                        "right": ["avatar", "phone", "email", "role"],
                        "top": ["last_login", "status", "created_at"],
                        "bottom": ["updated_at", "deleted_at", "id"],
                    },
                },
                {
                    "name": "posts",
                    "x": 1780,
                    "y": 270,
                    "w": 230,
                    "h": 86,
                    "attributes": [
                        "user_id",
                        "topic_id",
                        "title",
                        "content",
                        "visibility",
                        "status",
                        "ref_type",
                        "ref_id",
                        "created_at",
                        "updated_at",
                        "deleted_at",
                        "id",
                    ],
                    "sides": {
                        "left": ["user_id", "topic_id", "title"],
                        "right": ["content", "visibility", "status"],
                        "top": ["ref_type", "ref_id", "created_at"],
                        "bottom": ["updated_at", "deleted_at", "id"],
                    },
                },
                {
                    "name": "post_topics",
                    "x": 2560,
                    "y": 270,
                    "w": 280,
                    "h": 86,
                    "attributes": ["name", "description", "created_at", "updated_at", "deleted_at", "id"],
                    "sides": {
                        "left": ["name", "description"],
                        "right": ["created_at", "updated_at"],
                        "bottom": ["deleted_at", "id"],
                    },
                },
                {
                    "name": "post_comments",
                    "x": 1780,
                    "y": 1270,
                    "w": 330,
                    "h": 86,
                    "attributes": ["post_id", "user_id", "parent_id", "content", "created_at", "updated_at", "deleted_at", "id"],
                    "sides": {
                        "left": ["post_id", "user_id", "parent_id"],
                        "right": ["content", "created_at", "updated_at"],
                        "bottom": ["deleted_at", "id"],
                    },
                },
            ],
            "relationships": [
                {
                    "name": "关注者",
                    "x": 610,
                    "y": 520,
                    "w": 160,
                    "h": 90,
                    "connections": [
                        {"entity": "user_follows", "cardinality": "n", "via": "follower_id"},
                        {"entity": "users", "cardinality": "1"},
                    ],
                },
                {
                    "name": "被关注者",
                    "x": 610,
                    "y": 900,
                    "w": 170,
                    "h": 90,
                    "connections": [
                        {"entity": "user_follows", "cardinality": "n", "via": "following_id"},
                        {"entity": "users", "cardinality": "1"},
                    ],
                },
                {
                    "name": "发布",
                    "x": 1450,
                    "y": 270,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "users", "cardinality": "1"},
                        {"entity": "posts", "cardinality": "n", "via": "user_id"},
                    ],
                },
                {
                    "name": "属于",
                    "x": 2220,
                    "y": 270,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "posts", "cardinality": "n", "via": "topic_id"},
                        {"entity": "post_topics", "cardinality": "1"},
                    ],
                },
                {
                    "name": "评论者",
                    "x": 1340,
                    "y": 1120,
                    "w": 160,
                    "h": 90,
                    "connections": [
                        {"entity": "users", "cardinality": "1"},
                        {"entity": "post_comments", "cardinality": "n", "via": "user_id"},
                    ],
                },
                {
                    "name": "评论",
                    "x": 1900,
                    "y": 820,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "posts", "cardinality": "1"},
                        {"entity": "post_comments", "cardinality": "n", "via": "post_id"},
                    ],
                },
            ],
            "loops": [
                {
                    "entity": "post_comments",
                    "name": "回复",
                    "x": 2410,
                    "y": 1290,
                    "w": 140,
                    "h": 90,
                    "near_cardinality": "1",
                    "far_cardinality": "n",
                }
            ],
        },
        {
            "slug": "diagram-04-review-system",
            "title": "图 4  题库复习模块 ER 图",
            "subtitle": "users / tags / review_records / questions / question_tags",
            "width": 3200,
            "height": 2260,
            "entities": [
                {
                    "name": "users",
                    "x": 1280,
                    "y": 200,
                    "w": 240,
                    "h": 86,
                    "attributes": users_attrs,
                    "sides": {
                        "left": ["student_id", "password", "password_changed", "nickname"],
                        "right": ["avatar", "phone", "email", "role"],
                        "top": ["last_login", "status", "created_at"],
                        "bottom": ["updated_at", "deleted_at", "id"],
                    },
                },
                {
                    "name": "tags",
                    "x": 220,
                    "y": 920,
                    "w": 220,
                    "h": 86,
                    "attributes": ["user_id", "name", "color", "created_at", "updated_at", "deleted_at", "id"],
                    "sides": {
                        "left": ["user_id", "name"],
                        "right": ["color", "created_at"],
                        "bottom": ["updated_at", "deleted_at", "id"],
                    },
                },
                {
                    "name": "review_records",
                    "x": 1140,
                    "y": 920,
                    "w": 330,
                    "h": 86,
                    "attributes": [
                        "user_id",
                        "question_id",
                        "is_correct",
                        "reviewed_at",
                        "time_cost",
                        "created_at",
                        "updated_at",
                        "deleted_at",
                        "id",
                    ],
                    "sides": {
                        "left": ["user_id", "question_id", "is_correct"],
                        "right": ["reviewed_at", "time_cost", "created_at"],
                        "bottom": ["updated_at", "deleted_at", "id"],
                    },
                },
                {
                    "name": "questions",
                    "x": 2250,
                    "y": 770,
                    "w": 280,
                    "h": 86,
                    "attributes": [
                        "user_id",
                        "subject",
                        "title",
                        "content",
                        "image_url",
                        "answer",
                        "analysis",
                        "ai_analysis",
                        "error_reason",
                        "source",
                        "difficulty",
                        "review_count",
                        "next_review_at",
                        "review_stage",
                        "is_mastered",
                        "created_at",
                        "updated_at",
                        "deleted_at",
                        "id",
                    ],
                    "sides": {
                        "left": ["user_id", "subject", "title", "content", "image_url"],
                        "right": ["answer", "analysis", "ai_analysis", "error_reason", "source"],
                        "top": ["difficulty", "review_count", "next_review_at", "review_stage"],
                        "bottom": ["is_mastered", "created_at", "updated_at", "deleted_at", "id"],
                    },
                },
                {
                    "name": "question_tags",
                    "x": 1280,
                    "y": 1690,
                    "w": 300,
                    "h": 86,
                    "attributes": ["question_id", "tag_id"],
                    "sides": {
                        "left": ["question_id"],
                        "right": ["tag_id"],
                    },
                },
            ],
            "relationships": [
                {
                    "name": "创建",
                    "x": 760,
                    "y": 520,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "users", "cardinality": "1"},
                        {"entity": "tags", "cardinality": "n", "via": "user_id"},
                    ],
                },
                {
                    "name": "复习",
                    "x": 1370,
                    "y": 560,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "users", "cardinality": "1"},
                        {"entity": "review_records", "cardinality": "n", "via": "user_id"},
                    ],
                },
                {
                    "name": "出题",
                    "x": 1980,
                    "y": 520,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "users", "cardinality": "1"},
                        {"entity": "questions", "cardinality": "n", "via": "user_id"},
                    ],
                },
                {
                    "name": "记录",
                    "x": 1790,
                    "y": 920,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "review_records", "cardinality": "n", "via": "question_id"},
                        {"entity": "questions", "cardinality": "1"},
                    ],
                },
                {
                    "name": "标记",
                    "x": 760,
                    "y": 1560,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "tags", "cardinality": "1"},
                        {"entity": "question_tags", "cardinality": "n", "via": "tag_id"},
                    ],
                },
                {
                    "name": "关联",
                    "x": 1990,
                    "y": 1560,
                    "w": 140,
                    "h": 90,
                    "connections": [
                        {"entity": "question_tags", "cardinality": "n", "via": "question_id"},
                        {"entity": "questions", "cardinality": "1"},
                    ],
                },
            ],
        },
    ]


def build_html(diagrams: list[dict[str, object]], output_dir: Path) -> None:
    cards = []
    for diagram in diagrams:
        slug = diagram["slug"]
        title = diagram["title"]
        subtitle = diagram["subtitle"]
        cards.append(
            f"""
            <section class="card">
              <div class="meta">
                <h2>{escape(str(title))}</h2>
                <p>{escape(str(subtitle))}</p>
              </div>
              <img src="./{escape(str(slug))}.svg" alt="{escape(str(title))}" />
            </section>
            """
        )

    html_content = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Pastel ER Diagrams</title>
  <style>
    :root {{
      --bg: #f4f7fb;
      --card: rgba(255, 255, 255, 0.88);
      --border: #d8e0eb;
      --title: #294056;
      --text: #607085;
      --shadow: 0 18px 42px rgba(77, 98, 126, 0.12);
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      font-family: {FONT_FAMILY};
      background:
        radial-gradient(circle at top left, rgba(235, 225, 244, 0.9), transparent 28%),
        radial-gradient(circle at top right, rgba(224, 237, 249, 0.95), transparent 30%),
        linear-gradient(180deg, #f8fbff 0%, var(--bg) 100%);
      color: var(--title);
      padding: 32px 24px 64px;
    }}
    .wrap {{
      max-width: 1680px;
      margin: 0 auto;
    }}
    h1 {{
      margin: 0 0 10px;
      font-size: 34px;
      font-weight: 700;
    }}
    .lead {{
      margin: 0 0 28px;
      color: var(--text);
      font-size: 18px;
    }}
    .card {{
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 24px;
      box-shadow: var(--shadow);
      overflow: hidden;
      margin-bottom: 28px;
    }}
    .meta {{
      padding: 18px 22px 10px;
    }}
    .meta h2 {{
      margin: 0 0 6px;
      font-size: 24px;
      font-weight: 700;
    }}
    .meta p {{
      margin: 0;
      color: var(--text);
      font-size: 16px;
    }}
    img {{
      display: block;
      width: 100%;
      height: auto;
      background: #fff;
    }}
  </style>
</head>
<body>
  <main class="wrap">
    <h1>低饱和配色 ER 图</h1>
    <p class="lead">实体使用浅粉色，属性使用浅蓝色，关系使用浅紫色，整体参照你提供的最终样式重绘为 4 张独立 ER 图。</p>
    {"".join(cards)}
  </main>
</body>
</html>
"""
    (output_dir / "index.html").write_text(html_content, encoding="utf-8")


def main() -> None:
    output_dir = Path("docs/er-diagrams")
    output_dir.mkdir(parents=True, exist_ok=True)

    diagrams = build_diagrams()
    for diagram in diagrams:
        render_svg(diagram, output_dir / f"{diagram['slug']}.svg")

    build_html(diagrams, output_dir)
    print(f"Generated {len(diagrams)} SVG diagrams in {output_dir}")


if __name__ == "__main__":
    main()
