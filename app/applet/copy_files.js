const fs = require('fs');
const path = require('path');

function copyRecursiveSync(src, dest) {
    if (!fs.existsSync(dest)) {
        fs.mkdirSync(dest, { recursive: true });
    }

    const entries = fs.readdirSync(src, { withFileTypes: true });

    for (let entry of entries) {
        const srcPath = path.join(src, entry.name);
        const destPath = path.join(dest, entry.name);

        if (entry.isDirectory()) {
            copyRecursiveSync(srcPath, destPath);
        } else {
            // Only copy if it doesn't already exist in dest, so we don't overwrite our modified versions
            if (!fs.existsSync(destPath)) {
                fs.copyFileSync(srcPath, destPath);
                console.log(`Copied: ${srcPath} -> ${destPath}`);
            } else {
                console.log(`Skipped (already exists): ${destPath}`);
            }
        }
    }
}

copyRecursiveSync('./android_setup', './update_app');
