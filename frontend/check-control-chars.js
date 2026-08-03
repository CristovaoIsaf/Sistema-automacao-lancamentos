const fs = require('fs');
const path = require('path');
const root = process.cwd();
function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === 'node_modules' || entry.name === 'dist' || entry.name === '.git') continue;
      walk(full);
    } else if (['.ts', '.tsx', '.js', '.jsx', '.css', '.json', '.mjs'].includes(path.extname(entry.name))) {
      const data = fs.readFileSync(full);
      for (let i = 0; i < data.length; i++) {
        const code = data[i];
        if (code < 32 && ![9, 10, 13].includes(code)) {
          console.log(path.relative(root, full), 'control-char', code, 'at', i);
          process.exit(0);
        }
      }
    }
  }
}
walk(root);
console.log('no-control-chars');
