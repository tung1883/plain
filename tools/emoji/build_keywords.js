// Builds app/src/main/assets/kb/emoji_keywords.tsv for the keyboard's emoji search.
//
// Source:  emojibase-data 17.0.0, en/compact.json (label + tags), MIT licence,
//          https://github.com/milesj/emojibase  (data derived from Unicode CLDR annotations).
// Usage:   npm install emojibase-data@17.0.0 && node build_keywords.js <path to node_modules/emojibase-data>
// Output:  one emoji per line:  <emoji>\t<name>|<keyword>|<keyword>...   (English, base emoji only:
//          no skin-tone variants, no regional indicators or other components).
// Only the generated TSV is committed; the emojibase package is not part of the app.
const fs = require('fs');
const path = require('path');

const dir = process.argv[2] || 'node_modules/emojibase-data';
const data = JSON.parse(fs.readFileSync(path.join(dir, 'en', 'compact.json'), 'utf8'));
const out = [];
for (const e of data) {
  if (e.group === 2) continue;                 // components: skin tones, hair styles, regional indicators
  const words = [e.label, ...(e.tags || [])]
    .map((w) => w.toLowerCase().replace(/[|\t\n]/g, ' ').trim())
    .filter((w, i, all) => w && all.indexOf(w) === i);
  out.push(e.unicode + '\t' + words.join('|'));
}
const dest = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets', 'kb', 'emoji_keywords.tsv');
fs.writeFileSync(dest, out.join('\n') + '\n');
console.log(out.length + ' emoji, ' + fs.statSync(dest).size + ' bytes -> ' + dest);
