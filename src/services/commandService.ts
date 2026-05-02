import { getFeatures } from '../components/SettingsModal';
export function processCommand(command: string): {
  action: string;
  url?: string;
  isBrowserAction: boolean;
  webhookUrl?: string;
  isOfflineFallback?: boolean;
} {
  const lowerCmd = command.toLowerCase().trim();

  const features = getFeatures();

  // Call feature: "Call [name/number]"
  const callMatch = lowerCmd.match(/^(?:call|dial|phone)\s+(.+)$|(.+?)\s+(?:ko\s+)?(?:call|phone)\s*(?:karo|lagao|kar|laga)/i);
  if (callMatch && features.enableCalling) {
    let contact = (callMatch[1] || callMatch[2]).trim();
    if (typeof localStorage !== 'undefined') {
      try {
        const saved = localStorage.getItem('ruhi_contacts');
        if (saved) {
          const contacts = JSON.parse(saved);
          const found = contacts.find((c: any) => c.name.toLowerCase() === contact.toLowerCase());
          if (found) {
            contact = found.phone;
          }
        }
      } catch (e) {}
    }
    // Use tel: protocol which works natively on mobile devices to open the dialer
    return {
      action: `Calling ${contact}, boss.`,
      url: `tel:${encodeURIComponent(contact)}`,
      isBrowserAction: true,
    };
  }

  // General Browsing / App Opening: "Open [app]" or "[app] open karo"
  const openMatch = lowerCmd.match(/^(?:open|start|launch)\s+(.+)$|(.+?)\s+(?:open|chalu|shuru)\s*(?:karo|kar|karo\s*na)?$/);
  if (
    openMatch && features.enableAppOpening &&
    !lowerCmd.includes("youtube") &&
    !lowerCmd.includes("spotify")
  ) {
    let appName = (openMatch[1] || openMatch[2]).trim().toLowerCase();
    
    // Special case for WhatsApp
    if (appName.includes("whatsapp")) {
      return {
        action: `Opening WhatsApp, boss.`,
        url: `whatsapp://`,
        isBrowserAction: true,
      };
    }

    let website = appName.replace(/\s+/g, "");
    if (!website.includes(".")) {
      website += ".com";
    }
    return {
      action: `Opening ${appName} for you, boss.`,
      url: `https://www.${website}`,
      isBrowserAction: true,
    };
  }

  // Media Search: "Play [song/video] on YouTube"
  const ytMatch = lowerCmd.match(/^play\s+(.+?)\s+on\s+youtube$/);
  if (ytMatch && features.enableMedia) {
    const query = encodeURIComponent(ytMatch[1].trim());
    return {
      action: `Playing ${ytMatch[1]} on YouTube, boss.`,
      url: `https://www.youtube.com/results?search_query=${query}`,
      isBrowserAction: true,
    };
  }

  // Media Search: "Search [query] on Spotify"
  const spotifyMatch = lowerCmd.match(/^search\s+(.+?)\s+on\s+spotify$/);
  if (spotifyMatch && features.enableMedia) {
    const query = encodeURIComponent(spotifyMatch[1].trim());
    return {
      action: `Searching ${spotifyMatch[1]} on Spotify.`,
      url: `https://open.spotify.com/search/${query}`,
      isBrowserAction: true,
    };
  }

  // WhatsApp direct message
  const waMatch = lowerCmd.match(/(?:send\s+a\s+whats?app?\s+message\s+to|whats?app?\s+p[ea]r)\s+(.+?)\s+(?:saying|ko\s+bolo\s+k[io])\s+(.+)$/i);
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
          if (found) phoneNumber = found.phone;
        }
      } catch (e) {}
    }
    const message = encodeURIComponent(messageRaw);
    return {
      action: `Opening WhatsApp to send your message to ${contactRaw}, boss.`,
      url: `whatsapp://send?phone=${encodeURIComponent(phoneNumber)}&text=${message}`,
      isBrowserAction: true,
    };
  }

  if (typeof navigator !== "undefined" && !navigator.onLine && features.enableOfflineMode) {
    if (lowerCmd.includes("hello") || lowerCmd.includes("hi ruhi")) return { action: "Hello Boss. I am currently offline, but listening.", isBrowserAction: true, isOfflineFallback: true };
    if (lowerCmd.includes("how are you") || lowerCmd.includes("kya haal hai")) return { action: "I am fine Boss, but my internet connection seems down.", isBrowserAction: true, isOfflineFallback: true };
    if (lowerCmd.includes("time")) { const timeStr = new Date().toLocaleTimeString(); return { action: "The time is " + timeStr + ", Boss.", isBrowserAction: true, isOfflineFallback: true }; }
    return { action: "Boss, I am currently offline and cannot process complex requests.", isBrowserAction: true, isOfflineFallback: true };
  }

  return { action: "", isBrowserAction: false };
}
