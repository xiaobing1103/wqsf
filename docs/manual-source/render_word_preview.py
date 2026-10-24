"""Windows无捆绑LibreOffice时，用Word只读导出的PDF接入技能渲染器。

不修改技能文件；仅替换本次进程的PDF转换函数，其PNG渲染仍由render_docx.py完成。
"""
import argparse
import importlib.util
import os
import shutil
from pathlib import Path

p=argparse.ArgumentParser()
p.add_argument('docx')
p.add_argument('pdf')
p.add_argument('output')
a=p.parse_args()
dependencies=Path('C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies')
os.environ['PATH']=str(dependencies/'native/poppler/Library/bin')+os.pathsep+os.environ.get('PATH','')
renderer=Path('C:/Users/Administrator/.codex/plugins/cache/openai-primary-runtime/documents/26.1011.11113/skills/documents/render_docx.py')
spec=importlib.util.spec_from_file_location('document_renderer',renderer)
mod=importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
pdf=Path(a.pdf).resolve()
if not pdf.is_file():raise FileNotFoundError(pdf)
def word_pdf(input_path, profile, output, stem, verbose=False):
    dst=Path(output)/(stem+'.pdf')
    shutil.copy2(pdf,dst)
    return str(dst),'PDF由Windows Word只读导出，未使用系统LibreOffice'
mod.convert_to_pdf=word_pdf
pages=mod.rasterize(a.docx,a.output,150,False,False)
print('渲染页数',len(pages))
