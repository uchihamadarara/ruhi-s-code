const fs = require('fs');
const path = require('path');

const srcDir = '/app/applet/update_app';
const destDir = '/app/applet/final';

if (!fs.existsSync(destDir)) {
    fs.mkdirSync(destDir);
}

const files = [
    'AndroidManifest.xml',
    'MainActivity.kt',
    'GeminiLiveClient.kt'
];

files.forEach(file => {
    const srcPath = path.join(srcDir, file);
    const destPath = path.join(destDir, file);
    if (fs.existsSync(srcPath)) {
        fs.copyFileSync(srcPath, destPath);
        console.log(`Copied ${file} to final/ folder.`);
    }
});
