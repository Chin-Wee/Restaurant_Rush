#!/usr/bin/env python3
"""Compare the unchanged final UML with compiled Stage 1 declarations using only stdlib and javap."""
from pathlib import Path
import argparse
import hashlib
import html
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DIAGRAM = ROOT / "docs/uml/RestaurantRush_ClassDiagram.drawio"
OUTPUT = ROOT / "docs/Stage1-Api-Differences.md"
LATER = {"RegularCustomer", "VIPCustomer", "CriticCustomer", "Dish", "ComboMeal", "Host",
         "BillCalculator", "HappyHour", "CustomerFactory", "CustomerType", "HostDecision", "HostTaskType"}
LATER_MEMBERS = {"customerFactory", "happyHour", "host", "isHappyHourActive", "happyHourRemainingTurns",
                 "getHappyHourRemainingTurns", "recover", "applyCustomerDiscount"}

def text(value):
    return html.unescape(re.sub(r"<[^>]+>", " ", value or "")).strip()

def short(value):
    return re.sub(r"(?:[a-z_]\w*\.)+([A-Z]\w*)", r"\1", value)

def erased(value):
    while "<" in value:
        value = re.sub(r"<[^<>]*>", "", value)
    return re.sub(r"\s+", "", short(value))

def split_types(value):
    return [] if not value else re.split(r",\s*(?![^<]*>)", value)

def code(value):
    return "<code>" + html.escape(value) + "</code>"

def uml():
    cells = ET.parse(DIAGRAM).findall(".//mxCell")
    classes = {}
    for cell in cells:
        raw = text(cell.get("value"))
        if "swimlane" not in cell.get("style", "") or not raw:
            continue
        name = raw.splitlines()[-1].strip()
        kind = "enum" if "enumeration" in raw else "abstract class" if "abstract" in raw else "class"
        members = []
        for child in cells:
            if child.get("parent") != cell.get("id"):
                continue
            line = re.sub(r"\s+", " ", text(child.get("value")))
            if not line:
                continue
            method = re.match(r"([+#~-])\s*(\w+)\((.*?)\):\s*(\w+)", line)
            field = re.match(r"([+#~-])\s*(\w+):\s*(\w+)", line)
            if method:
                vis, member, params, ret = method.groups()
                types = tuple(erased(p.split(":", 1)[1].strip()) for p in split_types(params))
                members.append((("method", member, types, ret, vis), line))
            elif field:
                vis, member, typ = field.groups()
                members.append((("field", member, (), typ, vis), line))
            elif re.fullmatch(r"[A-Z][A-Z_0-9]*", line):
                members.append((("constant", line, (), name, "+"), line))
        classes[name] = (kind, members)
    return classes

def java():
    paths = sorted((ROOT / "src/main/java").rglob("*.java"))
    qualified = [".".join(path.relative_to(ROOT / "src/main/java").with_suffix("").parts) for path in paths]
    output = subprocess.run(["javap", "-private", "-classpath", str(ROOT / "target/classes"), *qualified],
                            check=True, text=True, capture_output=True).stdout
    sources = {path.stem: path for path in paths}
    declarations = {}
    classes = {}
    name = None
    for raw in output.splitlines():
        line = raw.strip()
        header = re.match(r".*\b(?:class|interface)\s+([\w.]+).* \{", line)
        if header:
            fq = header.group(1)
            name = fq.split(".")[-1]
            source = sources[name].read_text()
            kind = "enum" if re.search(r"\benum\s+" + name + r"\b", source) else "record" if re.search(
                r"\brecord\s+" + name + r"\b", source) else "abstract class" if re.search(
                r"\babstract\s+class\s+" + name + r"\b", source) else "class"
            declarations[name] = short(line[:-2])
            classes[name] = (kind, [])
            continue
        if name is None or not line.endswith(";") or "$" in line or line == "static {};":
            continue
        kind, members = classes[name]
        vis = "+" if line.startswith("public ") else "-" if line.startswith("private ") else "#" if line.startswith("protected ") else "~"
        declaration = line[:-1]
        if "(" in declaration:
            pre, rest = declaration.split("(", 1)
            params = rest.split(")", 1)[0]
            member = pre.split()[-1]
            if "." in member:
                member, ret = "<init>", name
            else:
                ret = erased(pre.split()[-2])
            if kind == "record" and member in {"equals", "hashCode", "toString"}:
                continue
            if kind == "enum" and member in {"values", "valueOf", "<init>"}:
                continue
            key = ("method", member, tuple(erased(p) for p in split_types(params)), ret, vis)
        else:
            words = declaration.split()
            if len(words) < 2:
                continue
            member, typ = words[-1], erased(words[-2])
            key = ("constant" if kind == "enum" and "public static final" in declaration else "field",
                   member, (), typ, vis)
        members.append((key, short(declaration)))
    return classes, sources, declarations

def generate():
    expected = uml()
    actual, sources, declarations = java()
    lines = ["# Stage 1 API differences from the supplied final UML", "",
             "Generated by python3 scripts/audit_stage1_api.py after compiling. --check detects a stale inventory; it does **not** assert zero differences.",
             "", "The supplied diagram remains unchanged. It describes Stage 3; later-stage exclusions are marked separately.",
             "Record-generated equals/hashCode/toString, synthetic methods, and enum compiler helpers are omitted. Constructors and explicit accessors are included.",
             "Generic element types are erased for signature matching because the UML declares raw List; the actual generic declaration remains visible. Associations and behavior are audited in [Stage1-Parity-Audit.md](Stage1-Parity-Audit.md).",
             "", f"Diagram SHA-256: {code(hashlib.sha256(DIAGRAM.read_bytes()).hexdigest())}.",
             "", f"Scope: **{len(actual)} production Java types**, **{len(expected)} named UML types**.", ""]
    counts = {"MATCH": 0, "API DIFFERENCE": 0, "STAGE 2/3": 0, "CODE ADDITION": 0}
    for name in sorted(set(expected) | set(actual)):
        ukind, umembers = expected.get(name, ("not shown", []))
        ckind, cmembers = actual.get(name, ("absent", []))
        lines += [f"## {name}", "", f"UML: **{ukind}**. Stage 1: **{ckind}**."]
        if name in declarations:
            lines += ["Compiled declaration: " + code(declarations[name]) + "."]
        if name in sources:
            lines += [f"Source: [{name}.java](../{sources[name].relative_to(ROOT)})."]
        if ukind != ckind and name in {"Customer", "MenuItem"}:
            lines += ["Declaration difference: a concrete Stage 1 type replaces the final abstraction; a Java record is a concrete class."]
        lines += ["", "| Status | UML declaration | Stage 1 declaration / reason |", "| --- | --- | --- |"]
        used = set()
        for key, display in umembers:
            matches = [(i, d) for i, (k, d) in enumerate(cmembers) if k == key]
            if matches:
                i, declaration = matches[0]
                used.add(i)
                status = "MATCH"
            elif name in LATER or key[1] in LATER_MEMBERS:
                status, declaration = "STAGE 2/3", "Excluded from this runnable stage."
            else:
                status = "API DIFFERENCE"
                similar = [d for k, d in cmembers if k[0] == key[0] and k[1] == key[1]]
                declaration = "; ".join(similar) if similar else "Not declared with this name/signature; see code additions below."
            counts[status] += 1
            lines.append(f"| {status} | {code(display)} | {code(declaration)} |")
        for i, (key, display) in enumerate(cmembers):
            if i not in used:
                counts["CODE ADDITION"] += 1
                lines.append(f"| CODE ADDITION | — | {code(display)} |")
        lines += [""]
    lines += ["## Inventory totals", "", "| Status | Declarations |", "| --- | --- |"]
    lines += [f"| {key} | {value} |" for key, value in counts.items()]
    lines += ["", "A literal API difference is not automatically a Stage 1 gameplay defect. See the audit for disposition.", ""]
    return "\n".join(lines)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    result = generate()
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text() != result:
            raise SystemExit("API inventory is stale. Compile, then run python3 scripts/audit_stage1_api.py")
        print("API inventory matches the compiled Stage 1 declarations and supplied UML.")
    else:
        OUTPUT.write_text(result)
        print(f"Wrote {OUTPUT.relative_to(ROOT)}")
