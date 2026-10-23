// Writes a small set of documents for trying out content search by hand:
//
//   node tools/search/make_test_corpus.js <out-dir>
//   adb push <out-dir> /sdcard/Documents/search-test
//
// Files: markdown, text, Vietnamese and Chinese text, code, docx, xlsx, pptx, odt, epub, a PDF with a
// text layer, and a PDF that is only a picture (a scan: it must be skipped). A password-protected PDF
// cannot be made here; use any you have. Every file contains the word of its own kind (see FILES).
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const out = process.argv[2];
if (!out) {
  console.error('usage: node make_test_corpus.js <out-dir>');
  process.exit(1);
}
fs.mkdirSync(out, { recursive: true });

// ---- a minimal zip writer (stored, no compression)
function zip(file, entries) {
  const locals = [];
  const centrals = [];
  let offset = 0;
  for (const [name, content] of entries) {
    const data = Buffer.from(content, 'utf8');
    const nameBuf = Buffer.from(name, 'utf8');
    const crc = zlib.crc32(data);
    const local = Buffer.alloc(30);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(20, 4);
    local.writeUInt16LE(0x0800, 6);
    local.writeUInt16LE(0, 8);
    local.writeUInt32LE(crc, 14);
    local.writeUInt32LE(data.length, 18);
    local.writeUInt32LE(data.length, 22);
    local.writeUInt16LE(nameBuf.length, 26);
    locals.push(local, nameBuf, data);

    const central = Buffer.alloc(46);
    central.writeUInt32LE(0x02014b50, 0);
    central.writeUInt16LE(20, 4);
    central.writeUInt16LE(20, 6);
    central.writeUInt16LE(0x0800, 8);
    central.writeUInt32LE(crc, 16);
    central.writeUInt32LE(data.length, 20);
    central.writeUInt32LE(data.length, 24);
    central.writeUInt16LE(nameBuf.length, 28);
    central.writeUInt32LE(offset, 42);
    centrals.push(central, nameBuf);
    offset += local.length + nameBuf.length + data.length;
  }
  const centralBuf = Buffer.concat(centrals);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(entries.length, 8);
  end.writeUInt16LE(entries.length, 10);
  end.writeUInt32LE(centralBuf.length, 12);
  end.writeUInt32LE(offset, 16);
  fs.writeFileSync(path.join(out, file), Buffer.concat([...locals, centralBuf, end]));
}

// ---- a minimal PDF writer
function pdf(file, pages, withText) {
  const objs = [];
  const add = (body) => objs.push(body) && objs.length;
  add('<< /Type /Catalog /Pages 2 0 R >>');                        // 1
  const kids = pages.map((_, i) => `${4 + i * 2} 0 R`).join(' ');
  add(`<< /Type /Pages /Kids [${kids}] /Count ${pages.length} >>`); // 2
  add('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>');    // 3
  pages.forEach((text, i) => {
    const pageNo = 4 + i * 2;
    const stream = withText
      ? `BT /F1 12 Tf 72 720 Td (${text.replace(/[()\\]/g, '\\$&')}) Tj ET`
      : 'q 400 0 0 400 100 200 cm /Im1 Do Q';
    add(`<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents ${pageNo + 1} 0 R `
      + `/Resources << ${withText ? '/Font << /F1 3 0 R >>' : '/XObject << /Im1 ' + (4 + pages.length * 2) + ' 0 R >>'} >> >>`);
    add(`<< /Length ${stream.length} >>\nstream\n${stream}\nendstream`);
  });
  if (!withText) {
    add('<< /Type /XObject /Subtype /Image /Width 1 /Height 1 /ColorSpace /DeviceGray /BitsPerComponent 8 /Length 1 >>\nstream\n\u0080\nendstream');
  }
  let body = '%PDF-1.4\n';
  const offsets = [];
  objs.forEach((o, i) => {
    offsets.push(Buffer.byteLength(body, 'latin1'));
    body += `${i + 1} 0 obj\n${o}\nendobj\n`;
  });
  const xref = Buffer.byteLength(body, 'latin1');
  body += `xref\n0 ${objs.length + 1}\n0000000000 65535 f \n`;
  for (const off of offsets) body += String(off).padStart(10, '0') + ' 00000 n \n';
  body += `trailer\n<< /Size ${objs.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  fs.writeFileSync(path.join(out, file), Buffer.from(body, 'latin1'));
}

const w = (name, text) => fs.writeFileSync(path.join(out, name), text, 'utf8');

w('notes.md', '# Quarterly plan\nThe zebrafinch project ships in June.\n\n## Risks\nBudget is tight and staffing is thin.\n');
w('report.txt', 'Annual report\nThe harmonica revenue grew by twelve percent over the previous year.\n');
w('vietnamese.txt', 'Ông Nguyễn Văn Tùng đã đến Đà Nẵng để gặp bạn bè và thưởng thức bánh xèo.\n');
w('chinese.txt', '这是一个关于文件夹和搜索的测试。读取中文文档应该可以找到子字符串。\n');
w('script.py', '# helper\ndef pangolin_count(items):\n    return len(items)\n');
w('page.html', '<html><head><title>Garden page</title></head><body><h1>Garden</h1><p>The quokka lives here.</p></body></html>');

zip('budget.docx', [['word/document.xml',
  '<?xml version="1.0"?><w:document><w:body>'
  + '<w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Budget Report</w:t></w:r></w:p>'
  + '<w:p><w:r><w:t>The wombat initiative needs more funding next year.</w:t></w:r></w:p>'
  + '</w:body></w:document>']]);

zip('sales.xlsx', [
  ['xl/workbook.xml', '<workbook><sheets><sheet name="Sales" sheetId="1" r:id="rId1"/></sheets></workbook>'],
  ['xl/_rels/workbook.xml.rels', '<Relationships><Relationship Id="rId1" Target="worksheets/sheet1.xml"/></Relationships>'],
  ['xl/sharedStrings.xml', '<sst><si><t>Capybara</t></si><si><t>Wheelbarrows</t></si></sst>'],
  ['xl/worksheets/sheet1.xml', '<worksheet><sheetData><row><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c></row></sheetData></worksheet>'],
]);

zip('deck.pptx', [
  ['ppt/slides/slide1.xml', '<p:sld><a:p><a:r><a:t>Launch plan</a:t></a:r></a:p><a:p><a:r><a:t>Ship the axolotl release</a:t></a:r></a:p></p:sld>'],
  ['ppt/slides/slide2.xml', '<p:sld><a:p><a:r><a:t>Second slide about marmots</a:t></a:r></a:p></p:sld>'],
]);

zip('letter.odt', [['content.xml',
  '<office:document-content><office:body><office:text><text:h text:outline-level="1">Dear friend</text:h>'
  + '<text:p>The ocelot sends greetings from the north.</text:p></office:text></office:body></office:document-content>']]);

zip('book.epub', [
  ['META-INF/container.xml', '<container><rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles></container>'],
  ['OEBPS/content.opf', '<package><metadata><dc:title>Field Guide</dc:title></metadata><manifest><item id="c1" href="ch1.xhtml"/></manifest><spine><itemref idref="c1"/></spine></package>'],
  ['OEBPS/ch1.xhtml', '<html><body><h1>Chapter One</h1><p>A narwhal surfaced beside the boat.</p></body></html>'],
]);

const para = 'The platypus is a semi-aquatic egg-laying mammal and this page has plenty of ordinary words in it.';
pdf('text.pdf', [para + ' First page.', para + ' Second page mentions the aardvark.', para + ' Third page.'], true);
pdf('scan.pdf', ['', ''], false);

console.log('wrote test corpus to ' + out);
console.log('words to search for: zebrafinch harmonica "Nguyen" quokka wombat capybara axolotl ocelot narwhal aardvark platypus 文件夹 pangolin');
