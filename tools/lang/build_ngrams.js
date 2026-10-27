// Builds app/src/main/assets/kb/ngram_<lang>.txt: which words follow which, from a plain-text corpus.
//
//   node --max-old-space-size=6144 tools/lang/build_ngrams.js <lang> <corpus.txt|.gz> [maxLines]
//
// The corpus is one sentence per line (the OPUS OpenSubtitles "mono" files work, see
// https://opus.nlpl.eu/OpenSubtitles.php). Only words in app/src/main/assets/kb/words_<lang>.txt are
// counted; any other word breaks the chain. The sentence start counts as the word "^".
//
// Output lines (tab separated), read by NGramModel:
//   2  <a>      <total>  <b>:<count>,<b>:<count>,...     what follows a
//   3  <a> <b>  <total>  <c>:<count>,...                 what follows a then b
const fs = require('fs');
const zlib = require('zlib');
const readline = require('readline');
const path = require('path');

const [lang, corpus, maxArg] = process.argv.slice(2);
if (!lang || !corpus) {
  console.error('usage: node build_ngrams.js <lang> <corpus> [maxLines]');
  process.exit(1);
}
const MAX_LINES = Number(maxArg || 3000000);
const KB = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets', 'kb');
const words = fs.readFileSync(path.join(KB, `words_${lang}.txt`), 'utf8').split('\n').filter(Boolean);
const id = new Map(words.map((w, i) => [w, i]));
const START = words.length;          // the virtual word "^"
const V = words.length + 1;
const TRI_CTX = 3000;                // trigram contexts only over the most common words

const bi = new Map();                // a * V + b -> count
const tri = new Map();               // ((a * V) + b) * V + c -> count
const biTotal = new Float64Array(V);

function bump(m, k) {
  m.set(k, (m.get(k) || 0) + 1);
}

let lines = 0;
let tokens = 0;

function feed(line) {
  const toks = line.toLowerCase().replace(/[’]/g, "'").match(/[\p{L}][\p{L}'-]*/gu);
  if (!toks) return;
  let p2 = -1;
  let p1 = START;
  for (const t of toks) {
    const c = id.has(t) ? id.get(t) : -1;
    tokens++;
    if (c < 0) {            // unknown word: the chain breaks here
      p2 = -1;
      p1 = -1;
      continue;
    }
    if (p1 >= 0) {
      bump(bi, p1 * V + c);
      biTotal[p1]++;
      if (p2 >= 0 && (p2 === START || p2 < TRI_CTX) && (p1 === START || p1 < TRI_CTX)) {
        bump(tri, (p2 * V + p1) * V + c);
      }
    }
    p2 = p1;
    p1 = c;
  }
}

function finish() {
  const name = (i) => (i === START ? '^' : words[i]);
  const out = [];

  // bigrams: for each of the most common first words, its likeliest followers
  const byA = new Map();
  for (const [k, n] of bi) {
    if (n < 2) continue;
    const a = Math.floor(k / V);
    const b = k % V;
    if (b === START) continue;
    if (!byA.has(a)) byA.set(a, []);
    byA.get(a).push([b, n]);
  }
  const aList = [...byA.keys()].filter((a) => a === START || a < 10000).sort((x, y) => x - y);
  for (const a of aList) {
    if (a !== START && biTotal[a] < 20) continue;
    const top = byA.get(a).sort((x, y) => y[1] - x[1]).slice(0, 6);
    out.push(`2\t${name(a)}\t${biTotal[a]}\t${top.map(([b, n]) => `${name(b)}:${n}`).join(',')}`);
  }

  // trigrams: contexts seen often enough, with their top followers
  const ctx = new Map();
  for (const [k, n] of tri) {
    const c = k % V;
    const ab = (k - c) / V;
    let e = ctx.get(ab);
    if (!e) {
      e = { total: 0, next: [] };
      ctx.set(ab, e);
    }
    e.total += n;
    if (n >= 3) e.next.push([c, n]);
  }
  const rows = [...ctx.entries()]
    .filter(([, e]) => e.total >= 10 && e.next.length)
    .sort((x, y) => y[1].total - x[1].total)
    .slice(0, 12000);
  for (const [ab, e] of rows) {
    const b = ab % V;
    const a = (ab - b) / V;
    const top = e.next.sort((x, y) => y[1] - x[1]).slice(0, 3);
    out.push(`3\t${name(a)} ${name(b)}\t${e.total}\t${top.map(([c, n]) => `${name(c)}:${n}`).join(',')}`);
  }
  const file = path.join(KB, `ngram_${lang}.txt`);
  fs.writeFileSync(file, out.join('\n') + '\n');
  console.log(`${lang}: ${lines} lines, ${tokens} tokens, ${aList.length} bigram rows, ${rows.length} trigram rows -> ${file}`);
}

let input = fs.createReadStream(corpus);
if (corpus.endsWith('.gz')) {
  const gz = zlib.createGunzip();
  gz.on('error', () => {});            // a truncated download ends with an error: keep what was read
  input = input.pipe(gz);
}
const rl = readline.createInterface({ input, crlfDelay: Infinity });
let done = false;
const end = () => {
  if (done) return;
  done = true;
  finish();
};
rl.on('line', (line) => {
  if (done) return;
  lines++;
  feed(line);
  if (lines >= MAX_LINES) {
    rl.close();
    input.destroy();
    end();
  }
});
rl.on('close', end);
