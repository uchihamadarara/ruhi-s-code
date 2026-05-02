const fs = require('fs');
let C = fs.readFileSync('src/services/commandService.ts', 'utf8');

const replacement = `// WhatsApp direct message
  const waMatch = lowerCmd.match(
    /(?:send\\s+a\\s+whats?app?\\s+message\\s+to|whats?app?\\s+p[ea]r)\\s+(.+?)\\s+(?:saying|ko\\s+bolo\\s+k[io])\\s+(.+)$/i
  );
  if (waMatch && features.enableWhatsApp) {
    const contactRaw = waMatch[1].trim();
    const messageRaw = waMatch[2].trim();
    
    let phoneNumber = contactRaw;
    if (typeof localStorage !== 'undefined') {
      try {
        const saved = localStorage.getItem('ruhi_contacts');
        if (saved) {
          const contacts = JSON.parse(saved);
          const found = contacts.find((c) => c.name.toLowerCase() === contactRaw.toLowerCase());
          if (found) {
            phoneNumber = found.phone;
          }
        }
      } catch (e) {}
    }
    
    const message = encodeURIComponent(messageRaw);
    
    return {
      action: \`Opening WhatsApp to send your message to \${contactRaw}, boss.\`,
      url: \`whatsapp://send?phone=\${encodeURIComponent(phoneNumber)}&text=\${message}\`,
      isBrowserAction: true,
    };
  }`;

C = C.replace(/\/\/ WhatsApp via MacroDroid:[\s\S]*?if\s*\(deviceId\)\s*\{[\s\S]*?return\s*\{[\s\S]*?\};[\s\S]*?\}[\s\S]*?\}/, replacement);

fs.writeFileSync('src/services/commandService.ts', C);
