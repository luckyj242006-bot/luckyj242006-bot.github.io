// Downloads the app fonts from Google Fonts into www/fonts, so the finished app never contacts Google.
// Run it from the project folder: node scripts/vendor-fonts.mjs
// Add --strict to make the build fail if the download does not work. The release workflows do this.
import fs from 'node:fs';
import path from 'node:path';

const strict = process.argv.includes('--strict');
const indexPath = path.join('www', 'index.html');
const fontsDir = path.join('www', 'fonts');
const KEEP = new Set(['latin', 'latin-ext']);
const UA =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36';

function fail(message) {
  console.error('[vendor-fonts] ' + message);
  if (strict) process.exit(1);
  console.error('[vendor-fonts] Continuing without bundled fonts. The app will load them from Google Fonts instead.');
  process.exit(0);
}

let html = fs.readFileSync(indexPath, 'utf8');
const link = html.match(/<link rel="stylesheet" href="(https:\/\/fonts\.googleapis\.com\/css2[^"]+)">/);
if (!link) {
  console.log('[vendor-fonts] index.html has no Google Fonts link. Nothing to do.');
  process.exit(0);
}

try {
  const cssRes = await fetch(link[1], { headers: { 'User-Agent': UA } });
  if (!cssRes.ok) fail('Google Fonts answered ' + cssRes.status);
  const css = await cssRes.text();

  const blocks = [...css.matchAll(/\/\*\s*([\w-]+)\s*\*\/\s*(@font-face\s*\{[^}]*\})/g)].filter((m) => KEEP.has(m[1]));
  if (!blocks.length) fail('No latin font blocks found in the Google Fonts answer');

  fs.mkdirSync(fontsDir, { recursive: true });
  const out = [];
  for (const [, subset, block] of blocks) {
    const url = block.match(/url\((https:[^)]+)\)/);
    if (!url) fail('A font block had no file address');
    const file = subset + '-' + path.basename(new URL(url[1]).pathname);
    const res = await fetch(url[1], { headers: { 'User-Agent': UA } });
    if (!res.ok) fail('Font file answered ' + res.status);
    fs.writeFileSync(path.join(fontsDir, file), Buffer.from(await res.arrayBuffer()));
    out.push('/* ' + subset + ' */\n' + block.replace(url[1], 'fonts/' + file));
  }
  fs.writeFileSync(path.join('www', 'fonts.css'), out.join('\n') + '\n');

  html = html
    .replace(/<link rel="preconnect" href="https:\/\/fonts\.(googleapis|gstatic)\.com"[^>]*>\n?/g, '')
    .replace(link[0], '<link rel="stylesheet" href="fonts.css">');
  fs.writeFileSync(indexPath, html);
  console.log('[vendor-fonts] Saved ' + out.length + ' font files and fonts.css');
} catch (err) {
  fail('Download failed: ' + (err && err.message ? err.message : err));
}
