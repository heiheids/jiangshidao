'use strict';
// ---------------------------------------------------------------------------
// 备份/打包工具：在每次改动前备份工程源码，并按需生成发布 zip。
// 用法：
//   node tools/ops/backup.js "本次要做的操作说明"    # 备份 + 记录日志
//   node tools/ops/backup.js --pack v0.1.0            # 生成发布 zip
// ---------------------------------------------------------------------------

const fs = require('fs');
const path = require('path');
const { writeZip } = require('../lib/zip');

const ROOT = path.resolve(__dirname, '..', '..');
const BACKUP_DIR = path.join(ROOT, '备份');
const LOG_FILE = path.join(ROOT, '某某工程备份.txt');
const PACKAGE_LOG = path.join(BACKUP_DIR, '打包记录.txt');

const SKIP_DIRS = new Set(['备份', 'node_modules', '.git', 'build', '.gradle', 'run', 'out']);

function stamp() {
  const d = new Date();
  const p = (n, w = 2) => String(n).padStart(w, '0');
  return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}-${p(d.getHours())}${p(d.getMinutes())}${p(d.getSeconds())}`;
}

function walk(dir, base = ROOT, acc = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.isDirectory()) {
      if (SKIP_DIRS.has(e.name)) continue;
      walk(path.join(dir, e.name), base, acc);
    } else if (e.isFile()) {
      acc.push(path.relative(base, path.join(dir, e.name)).replace(/\\/g, '/'));
    }
  }
  return acc;
}

function ensureLog() {
  if (!fs.existsSync(LOG_FILE)) {
    fs.writeFileSync(LOG_FILE, [
      '# 某某工程备份 操作日志',
      '# 每次修改源码前先备份；本文件只追加，不覆盖历史。',
      '# 格式：时间 | 类型 | 说明 | 产物',
      '',
    ].join('\r\n'), 'utf8');
  }
}

function nowLocal() {
  const d = new Date();
  const p = (n, w = 2) => String(n).padStart(w, '0');
  const off = -d.getTimezoneOffset();
  const sign = off >= 0 ? '+' : '-';
  const oh = p(Math.floor(Math.abs(off) / 60));
  const om = p(Math.abs(off) % 60);
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} `
    + `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())} (UTC${sign}${oh}:${om})`;
}

function appendLog(line) {
  ensureLog();
  fs.appendFileSync(LOG_FILE, `${line}\r\n`, 'utf8');
}

function backup(note) {
  fs.mkdirSync(BACKUP_DIR, { recursive: true });
  const files = walk(ROOT);
  const entries = files.map((rel) => ({ name: rel, data: fs.readFileSync(path.join(ROOT, rel)) }));
  const name = `源码备份_${stamp()}.zip`;
  fs.writeFileSync(path.join(BACKUP_DIR, name), writeZip(entries));
  appendLog(`${nowLocal()} | 源码备份 | ${note} | 备份/${name}（${files.length} 个文件）`);
  console.log(`已备份 ${files.length} 个文件 -> 备份/${name}`);
  console.log(`备份原因：${note}`);
  return name;
}

function pack(version) {
  const files = walk(ROOT);
  const entries = files.map((rel) => ({ name: rel, data: fs.readFileSync(path.join(ROOT, rel)) }));
  const name = `僵尸怪物模组_${version}_完整包.zip`;
  const out = path.join(BACKUP_DIR, name);
  fs.mkdirSync(BACKUP_DIR, { recursive: true });
  fs.writeFileSync(out, writeZip(entries));
  const line = `${nowLocal()} | 发布打包 | 版本 ${version} | 备份/${name}（${files.length} 个文件，${(fs.statSync(out).size / 1024).toFixed(0)} KB）`;
  appendLog(line);
  fs.appendFileSync(PACKAGE_LOG, `${line}\r\n`, 'utf8');
  console.log(`已打包 -> 备份/${name}`);
  return out;
}

if (require.main === module) {
  const args = process.argv.slice(2);
  if (args[0] === '--pack') {
    pack(args[1] || 'v0.1.0');
  } else {
    backup(args.join(' ') || '未说明的操作');
  }
}

module.exports = { backup, pack, walk };
