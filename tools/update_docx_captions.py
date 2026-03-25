import argparse
import copy
import re
import zipfile
import xml.etree.ElementTree as ET
from collections import Counter
from dataclasses import dataclass, field


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


@dataclass
class Block:
    idx: int
    elem: ET.Element
    kind: str
    text: str = ""
    style_id: str = ""
    style_name: str = ""
    drawings: int = 0
    table_texts: list[str] = field(default_factory=list)
    table_rows: int = 0
    chapter: str = ""
    h2: str = ""
    h3: str = ""


def para_text(elem: ET.Element) -> str:
    return "".join((t.text or "") for t in elem.findall(".//w:t", NS)).strip()


def para_style_id(elem: ET.Element) -> str:
    style = elem.find("w:pPr/w:pStyle", NS)
    return style.get(qn("val")) if style is not None else ""


def table_texts(elem: ET.Element) -> list[str]:
    texts: list[str] = []
    for p in elem.findall(".//w:p", NS):
        text = para_text(p)
        if text:
            texts.append(text)
    return texts


def is_heading(style_name: str) -> bool:
    return style_name.startswith("heading ")


def is_caption_style(style_name: str) -> bool:
    return style_name == "caption"


def is_url(text: str) -> bool:
    lowered = text.lower()
    return lowered.startswith("http://") or lowered.startswith("https://")


def clean_label_text(text: str) -> str:
    txt = text.strip()
    txt = re.sub(r"^\s*[图表]\s*\d+\s*[:：、.\-]?\s*", "", txt)
    txt = re.sub(r"^\s*[图表]\s*[:：、.\-]?\s*", "", txt)
    txt = re.sub(r"^\s*[(（]?\d+[)）]?[.、]?\s*", "", txt)
    txt = re.sub(r"^\s*[一二三四五六七八九十]+[.、]\s*", "", txt)
    return txt.strip()


def looks_like_short_label(text: str) -> bool:
    txt = text.strip()
    if not txt or is_url(txt):
        return False
    if len(txt) > 60:
        return False
    if re.search(r"[。！？；]$", txt):
        return False
    return True


def make_caption_paragraph(text: str) -> ET.Element:
    p = ET.Element(qn("p"))
    p_pr = ET.SubElement(p, qn("pPr"))
    p_style = ET.SubElement(p_pr, qn("pStyle"))
    p_style.set(qn("val"), "14")
    run = ET.SubElement(p, qn("r"))
    run_text = ET.SubElement(run, qn("t"))
    run_text.text = text
    return p


def build_blocks(body: ET.Element, style_map: dict[str, str]) -> list[Block]:
    blocks: list[Block] = []
    for idx, child in enumerate(list(body)):
        tag = child.tag.split("}")[-1]
        if tag == "p":
            sid = para_style_id(child)
            blocks.append(
                Block(
                    idx=idx,
                    elem=child,
                    kind="p",
                    text=para_text(child),
                    style_id=sid,
                    style_name=style_map.get(sid, ""),
                    drawings=len(child.findall(".//w:drawing", NS)),
                )
            )
        elif tag == "tbl":
            blocks.append(
                Block(
                    idx=idx,
                    elem=child,
                    kind="tbl",
                    drawings=len(child.findall(".//w:drawing", NS)),
                    table_texts=table_texts(child),
                    table_rows=len(child.findall("w:tr", NS)),
                )
            )
        else:
            blocks.append(Block(idx=idx, elem=child, kind=tag))
    chapter = "前置部分"
    h2 = ""
    h3 = ""
    for block in blocks:
        if block.kind == "p" and block.text:
            if block.style_name == "heading 1":
                chapter = block.text
                h2 = ""
                h3 = ""
            elif block.style_name == "heading 2":
                h2 = block.text
                h3 = ""
            elif block.style_name == "heading 3":
                h3 = block.text
        block.chapter = chapter
        block.h2 = h2
        block.h3 = h3
    return blocks


def previous_non_empty_paragraph(blocks: list[Block], idx: int, limit: int = 8) -> Block | None:
    steps = 0
    for j in range(idx - 1, -1, -1):
        block = blocks[j]
        if block.kind != "p" or not block.text or block.drawings:
            continue
        steps += 1
        if steps > limit:
            break
        return block
    return None


def previous_meaningful_block(blocks: list[Block], idx: int, limit: int = 8) -> Block | None:
    steps = 0
    for j in range(idx - 1, -1, -1):
        block = blocks[j]
        if block.kind.startswith("bookmark"):
            continue
        if block.kind == "p" and not block.text and not block.drawings:
            continue
        steps += 1
        if steps > limit:
            break
        return block
    return None


def find_recent_short_label(blocks: list[Block], idx: int, limit: int = 8) -> str | None:
    steps = 0
    for j in range(idx - 1, -1, -1):
        block = blocks[j]
        if block.kind != "p" or not block.text or block.drawings:
            continue
        steps += 1
        if steps > limit:
            break
        if is_caption_style(block.style_name) or is_heading(block.style_name):
            continue
        if looks_like_short_label(block.text):
            return clean_label_text(block.text)
    return None


def existing_caption_text(block: Block) -> str:
    return clean_label_text(block.text)


def derive_image_base(blocks: list[Block], idx: int) -> str:
    block = blocks[idx]
    base = ""
    if block.h3:
        base = clean_label_text(block.h3)
    elif block.h2:
        base = clean_label_text(block.h2)
    elif block.chapter and block.chapter != "前置部分":
        base = clean_label_text(block.chapter)

    recent = find_recent_short_label(blocks, idx)
    if recent and (not base or base in {"数据采集"}):
        return recent
    if base:
        return base
    if recent:
        return recent
    return "图片"


def derive_table_base(blocks: list[Block], idx: int) -> str:
    block = blocks[idx]
    if block.drawings:
        short_texts: list[str] = []
        for text in block.table_texts:
            cleaned = clean_label_text(text)
            if cleaned and len(cleaned) <= 20 and cleaned not in short_texts:
                short_texts.append(cleaned)
            if len(short_texts) >= 2:
                break
        if block.h3 == "产品原型图":
            if short_texts:
                return f"产品原型图：{'、'.join(short_texts)}"
            return "产品原型图"
        if block.h2 == "界面需求":
            if short_texts:
                return f"界面原型：{'、'.join(short_texts)}"
            return "界面原型"
        if block.h2 == "用户界面设计":
            return "用户界面设计原型"
    if block.chapter == "开发计划" and block.h2 == "主要功能描述" and block.h3:
        return f"{clean_label_text(block.h3)}功能说明"
    if block.chapter == "需求分析" and block.h2 == "功能需求":
        if block.h3:
            return f"{clean_label_text(block.h3)}用例说明"
        return "功能需求总览"
    if block.h3:
        return clean_label_text(block.h3)
    if block.h2:
        return clean_label_text(block.h2)
    prev = previous_non_empty_paragraph(blocks, idx)
    if prev and looks_like_short_label(prev.text):
        return clean_label_text(prev.text)
    return "表格"


def should_reuse_as_table_caption(block: Block) -> bool:
    if block.kind != "p" or not block.text or block.drawings:
        return False
    if is_heading(block.style_name):
        return False
    if is_caption_style(block.style_name):
        return True
    txt = block.text.strip()
    if is_url(txt):
        return False
    if len(txt) > 80:
        return False
    if "表" in txt:
        return True
    if "（" in txt or "(" in txt:
        return True
    return False


def collect_plan(blocks: list[Block]) -> tuple[dict[int, str], dict[int, str], dict[int, str], dict[int, str], Counter]:
    image_existing: dict[int, str] = {}
    image_insert_after: dict[int, str] = {}
    table_reuse_prev: dict[int, str] = {}
    table_insert_before: dict[int, str] = {}
    usage = Counter()
    image_caption_indices: set[int] = set()

    raw_image_descs: list[tuple[str, int, bool]] = []
    for i, block in enumerate(blocks):
        if (
            block.kind != "p"
            or block.drawings == 0
            or block.chapter in {"详细设计", "前置部分"}
        ):
            continue
        next_block = blocks[i + 1] if i + 1 < len(blocks) else None
        if next_block and next_block.kind == "p" and next_block.text and (
            is_caption_style(next_block.style_name) or next_block.text.startswith("图")
        ):
            raw_desc = existing_caption_text(next_block) or derive_image_base(blocks, i)
            raw_image_descs.append((raw_desc, next_block.idx, True))
            image_caption_indices.add(next_block.idx)
        else:
            raw_desc = derive_image_base(blocks, i)
            raw_image_descs.append((raw_desc, block.idx, False))

    image_totals = Counter(desc for desc, _, _ in raw_image_descs)
    image_seen = Counter()
    figure_no = 1
    for raw_desc, target_idx, reuse_existing in raw_image_descs:
        image_seen[raw_desc] += 1
        desc = raw_desc
        if image_totals[raw_desc] > 1 and not reuse_existing:
            desc = f"{raw_desc}（{image_seen[raw_desc]}）"
        caption = f"图{figure_no} {desc}"
        figure_no += 1
        if reuse_existing:
            image_existing[target_idx] = caption
        else:
            image_insert_after[target_idx] = caption
        usage["images"] += 1

    raw_table_descs: list[tuple[str, int, bool]] = []
    skipped_tables = 0
    for i, block in enumerate(blocks):
        if block.kind != "tbl":
            continue
        if block.chapter == "前置部分" and block.drawings > 0:
            skipped_tables += 1
            continue
        prev_block = previous_meaningful_block(blocks, i)
        if prev_block and prev_block.idx not in image_caption_indices and should_reuse_as_table_caption(prev_block):
            raw_desc = existing_caption_text(prev_block) or derive_table_base(blocks, i)
            raw_table_descs.append((raw_desc, prev_block.idx, True))
        else:
            raw_desc = derive_table_base(blocks, i)
            raw_table_descs.append((raw_desc, block.idx, False))

    table_totals = Counter(desc for desc, _, _ in raw_table_descs)
    table_seen = Counter()
    table_no = 1
    for raw_desc, target_idx, reuse_existing in raw_table_descs:
        table_seen[raw_desc] += 1
        desc = raw_desc
        if table_totals[raw_desc] > 1 and not reuse_existing:
            desc = f"{raw_desc}（{table_seen[raw_desc]}）"
        caption = f"表{table_no} {desc}"
        table_no += 1
        if reuse_existing:
            table_reuse_prev[target_idx] = caption
        else:
            table_insert_before[target_idx] = caption
        usage["tables"] += 1
    usage["skipped_cover_tables"] = skipped_tables
    return image_existing, image_insert_after, table_reuse_prev, table_insert_before, usage


def write_output(
    body: ET.Element,
    blocks: list[Block],
    image_existing: dict[int, str],
    image_insert_after: dict[int, str],
    table_reuse_prev: dict[int, str],
    table_insert_before: dict[int, str],
) -> None:
    new_children: list[ET.Element] = []
    replaced_para_indices = set(image_existing) | set(table_reuse_prev)

    for block in blocks:
        if block.kind == "tbl" and block.idx in table_insert_before:
            new_children.append(make_caption_paragraph(table_insert_before[block.idx]))

        if block.idx in image_existing:
            new_children.append(make_caption_paragraph(image_existing[block.idx]))
        elif block.idx in table_reuse_prev:
            new_children.append(make_caption_paragraph(table_reuse_prev[block.idx]))
        else:
            new_children.append(copy.deepcopy(block.elem))

        if block.kind == "p" and block.idx in image_insert_after:
            new_children.append(make_caption_paragraph(image_insert_after[block.idx]))

    body[:] = new_children


def load_style_map(docx_path: str) -> tuple[ET.ElementTree, ET.Element, dict[str, str], dict[str, bytes]]:
    with zipfile.ZipFile(docx_path, "r") as zin:
        raw_files = {name: zin.read(name) for name in zin.namelist()}
    document_tree = ET.ElementTree(ET.fromstring(raw_files["word/document.xml"]))
    body = document_tree.getroot().find("w:body", NS)
    if body is None:
        raise RuntimeError("word/document.xml 中未找到文档正文。")
    styles_root = ET.fromstring(raw_files["word/styles.xml"])
    style_map: dict[str, str] = {}
    for style in styles_root.findall("w:style", NS):
        sid = style.get(qn("styleId"), "")
        name = style.find("w:name", NS)
        style_map[sid] = name.get(qn("val")) if name is not None else ""
    return document_tree, body, style_map, raw_files


def save_docx(output_path: str, document_tree: ET.ElementTree, raw_files: dict[str, bytes]) -> None:
    raw_files["word/document.xml"] = ET.tostring(
        document_tree.getroot(),
        encoding="utf-8",
        xml_declaration=True,
    )
    with zipfile.ZipFile(output_path, "w", compression=zipfile.ZIP_DEFLATED) as zout:
        for name, data in raw_files.items():
            zout.writestr(name, data)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    document_tree, body, style_map, raw_files = load_style_map(args.input)
    blocks = build_blocks(body, style_map)
    image_existing, image_insert_after, table_reuse_prev, table_insert_before, usage = collect_plan(blocks)
    write_output(
        body,
        blocks,
        image_existing,
        image_insert_after,
        table_reuse_prev,
        table_insert_before,
    )
    save_docx(args.output, document_tree, raw_files)

    print(
        f"updated_images={usage['images']} "
        f"updated_tables={usage['tables']} "
        f"skipped_cover_tables={usage['skipped_cover_tables']}"
    )


if __name__ == "__main__":
    main()
