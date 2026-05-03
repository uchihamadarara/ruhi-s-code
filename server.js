
const express = require('express');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = 3000;

app.get('/', (req, res) => {
    res.send(`
        <html>
        <head>
            <title>Download Ruhi Files</title>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body { font-family: sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; background: #0F0F1A; color: white; margin: 0; text-align: center; }
                a { padding: 15px 30px; background: #6200EA; color: white; text-decoration: none; border-radius: 8px; font-size: 20px; font-weight: bold; margin-top: 20px; display: inline-block; box-shadow: 0 4px 15px rgba(98, 0, 234, 0.4); }
                a:hover { background: #7C4DFF; }
                p { color: #AABBC0; padding: 0 20px; }
            </style>
        </head>
        <body>
            <h1>Ruhi AI Update</h1>
            <p>Click below to download the fixed zip file directly to your phone. This direct download avoids any file corruption issues.</p>
            <a href="/download">Download ruhi_update.zip</a>
        </body>
        </html>
    `);
});

app.get('/download', (req, res) => {
    const zipPath = '/app/applet/ruhi_update.zip';
    if(fs.existsSync(zipPath)) {
        res.download(zipPath, 'ruhi_update.zip');
    } else {
        res.status(404).send('File not found. Please ask AI to recreate it.');
    }
});

app.listen(PORT, '0.0.0.0', () => {
    console.log('Server running on port ' + PORT);
});
