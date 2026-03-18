from __future__ import annotations

import argparse
import html
import re
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.platypus import Paragraph, Preformatted, SimpleDocTemplate, Spacer


HEADING_RE = re.compile(r"^(#{1,6})\s+(.*)$")
BOLD_RE = re.compile(r"\*\*(.+?)\*\*")
INLINE_CODE_RE = re.compile(r"`([^`]+)`")
LINK_RE = re.compile(r"\[([^\]]+)\]\(([^)]+)\)")
ORDERED_RE = re.compile(r"^(\d+)\.\s+(.*)$")


def register_fonts() -> None:
    pdfmetrics.registerFont(UnicodeCIDFont("STSong-Light"))


def build_styles():
    styles = getSampleStyleSheet()
    base = {
        "fontName": "STSong-Light",
        "textColor": colors.HexColor("#1F2937"),
        "wordWrap": "CJK",
    }
    return {
        "title": ParagraphStyle(
            "TitleZh",
            parent=styles["Title"],
            fontSize=22,
            leading=28,
            spaceAfter=10,
            **base,
        ),
        "h1": ParagraphStyle(
            "Heading1Zh",
            parent=styles["Heading1"],
            fontSize=18,
            leading=24,
            spaceBefore=10,
            spaceAfter=6,
            **base,
        ),
        "h2": ParagraphStyle(
            "Heading2Zh",
            parent=styles["Heading2"],
            fontSize=15,
            leading=21,
            spaceBefore=8,
            spaceAfter=4,
            **base,
        ),
        "h3": ParagraphStyle(
            "Heading3Zh",
            parent=styles["Heading3"],
            fontSize=13,
            leading=18,
            spaceBefore=6,
            spaceAfter=3,
            **base,
        ),
        "body": ParagraphStyle(
            "BodyZh",
            parent=styles["BodyText"],
            fontSize=10.5,
            leading=16,
            spaceAfter=3,
            **base,
        ),
        "bullet": ParagraphStyle(
            "BulletZh",
            parent=styles["BodyText"],
            fontSize=10.5,
            leading=16,
            leftIndent=12,
            firstLineIndent=0,
            spaceAfter=2,
            **base,
        ),
        "code": ParagraphStyle(
            "CodeZh",
            parent=styles["Code"],
            fontName="STSong-Light",
            fontSize=8.5,
            leading=11,
            leftIndent=8,
            rightIndent=8,
            textColor=colors.HexColor("#111827"),
            backColor=colors.HexColor("#F3F4F6"),
            borderPadding=6,
            wordWrap="CJK",
        ),
    }


def format_inline(text: str) -> str:
    escaped = html.escape(text.strip())
    escaped = LINK_RE.sub(r"\1 (\2)", escaped)
    escaped = BOLD_RE.sub(r"<b>\1</b>", escaped)
    escaped = INLINE_CODE_RE.sub(r'<font backColor="#F3F4F6">\1</font>', escaped)
    return escaped.replace("  ", "&nbsp;&nbsp;")


def build_story(markdown: str):
    styles = build_styles()
    story = []
    in_code = False
    code_lines: list[str] = []

    for raw_line in markdown.splitlines():
        line = raw_line.rstrip()
        stripped = line.strip()

        if stripped.startswith("```"):
            if in_code:
                story.append(Preformatted("\n".join(code_lines), styles["code"]))
                story.append(Spacer(1, 3))
                code_lines = []
                in_code = False
            else:
                in_code = True
            continue

        if in_code:
            code_lines.append(line)
            continue

        if not stripped:
            story.append(Spacer(1, 4))
            continue

        heading = HEADING_RE.match(stripped)
        if heading:
            level = min(len(heading.group(1)), 3)
            style_key = "title" if level == 1 and not story else f"h{level}"
            story.append(Paragraph(format_inline(heading.group(2)), styles[style_key]))
            continue

        ordered = ORDERED_RE.match(stripped)
        if ordered:
            story.append(Paragraph(f"{ordered.group(1)}. {format_inline(ordered.group(2))}", styles["body"]))
            continue

        if stripped.startswith("- "):
            story.append(Paragraph(format_inline(stripped[2:]), styles["bullet"], bulletText="•"))
            continue

        story.append(Paragraph(format_inline(stripped), styles["body"]))

    if code_lines:
        story.append(Preformatted("\n".join(code_lines), styles["code"]))

    return story


def export_pdf(source: Path, target: Path) -> None:
    register_fonts()
    target.parent.mkdir(parents=True, exist_ok=True)
    document = SimpleDocTemplate(
        str(target),
        pagesize=A4,
        leftMargin=16 * mm,
        rightMargin=16 * mm,
        topMargin=14 * mm,
        bottomMargin=14 * mm,
        title="课程平台 API 接口文档",
        author="Codex",
    )
    story = build_story(source.read_text(encoding="utf-8"))
    document.build(story)


def main() -> None:
    project_root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description="Export docs/API_SPEC.md to PDF.")
    parser.add_argument(
        "--source",
        default=str(project_root / "docs" / "API_SPEC.md"),
        help="Markdown source file path.",
    )
    parser.add_argument(
        "--output",
        default=str(project_root / "docs" / "API_SPEC.pdf"),
        help="PDF output file path.",
    )
    args = parser.parse_args()

    export_pdf(Path(args.source), Path(args.output))


if __name__ == "__main__":
    main()
