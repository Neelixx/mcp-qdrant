#!/usr/bin/env python3
"""
Convert Eclipse help HTML files to Markdown format.
Preserves image links and converts internal HTML links to .md format.
"""

import os
import sys
import re
import html2text
from pathlib import Path

def convert_html_to_md(html_file_path, output_dir):
    """Convert a single HTML file to Markdown."""
    html_path = Path(html_file_path)
    
    # Read HTML content
    with open(html_path, 'r', encoding='utf-8', errors='ignore') as f:
        html_content = f.read()
    
    # Create html2text converter
    h = html2text.HTML2Text()
    h.ignore_links = False
    h.ignore_images = False
    h.ignore_tables = True  # Skip navigation tables
    h.body_width = 0  # No wrapping
    h.protect_links = True
    
    # Convert HTML to Markdown
    md_content = h.handle(html_content)
    
    # Post-processing:
    # 1. Convert .html links to .md links
    md_content = re.sub(r'\(.*?\.html\)', lambda m: m.group(0).replace('.html', '.md'), md_content)
    md_content = re.sub(r'\[([^\]]+)\]\(([^)]+)\.html([^)]*)\)', r'[\1](\2.md\3)', md_content)
    
    # 2. Clean up navigation artifacts (horizontal rules from table separators)
    md_content = re.sub(r'\n---\s*\n---\s*\n', '\n\n', md_content)
    
    # 3. Remove empty table remnants
    md_content = re.sub(r'\|\s*\|\s*\|\s*\n', '', md_content)
    md_content = re.sub(r'\n\|[-\s|]+\|\n', '\n', md_content)
    
    # 4. Fix image references - keep them as relative paths
    # html2text should handle this, but let's ensure consistency
    md_content = re.sub(r'!\[([^\]]*)\]\(([^)]+)\)', lambda m: f'![{m.group(1)}]({m.group(2)})', md_content)
    
    # 5. Clean up excessive newlines
    md_content = re.sub(r'\n{4,}', '\n\n\n', md_content)
    
    # 6. Add frontmatter with source info
    frontmatter = f"""---
source: {html_path.name}
original_path: {html_path.relative_to(output_dir)}
---

"""
    md_content = frontmatter + md_content
    
    # Determine output path
    rel_path = html_path.relative_to(output_dir)
    md_file_name = html_path.stem + '.md'
    output_path = output_dir / rel_path.parent / md_file_name
    
    # Create output directory if needed
    output_path.parent.mkdir(parents=True, exist_ok=True)
    
    # Write Markdown file
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(md_content)
    
    return output_path

def main():
    base_dir = Path('/home/frankw/git/github/mcp-qdrant/help-files/eclipse-help')
    
    if not base_dir.exists():
        print(f"Error: Directory not found: {base_dir}")
        sys.exit(1)
    
    # Find all HTML files
    html_files = list(base_dir.rglob('*.html')) + list(base_dir.rglob('*.htm'))
    
    print(f"Found {len(html_files)} HTML files to convert")
    
    converted = 0
    errors = 0
    
    for html_file in html_files:
        try:
            output_path = convert_html_to_md(html_file, base_dir)
            print(f"Converted: {html_file.name} -> {output_path.name}")
            converted += 1
        except Exception as e:
            print(f"Error converting {html_file}: {e}")
            errors += 1
    
    print(f"\nConversion complete: {converted} files converted, {errors} errors")

if __name__ == '__main__':
    main()
