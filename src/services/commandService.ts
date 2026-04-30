export function processCommand(command: string): {
  action: string;
  url?: string;
  isBrowserAction: boolean;
  webhookUrl?: string;
} {
  const lowerCmd = command.toLowerCase().trim();

  // General Browsing / App Opening: "Open [app]" or "[app] open karo"
  const openMatch = lowerCmd.match(/^(?:open|start|launch)\s+(.+)$|(.+?)\s+(?:open|chalu|shuru)\s*(?:karo|kar|karo\s*na)?$/);
  if (
    openMatch &&
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
  if (ytMatch) {
    const query = encodeURIComponent(ytMatch[1].trim());
    return {
      action: `Playing ${ytMatch[1]} on YouTube, boss.`,
      url: `https://www.youtube.com/results?search_query=${query}`,
      isBrowserAction: true,
    };
  }

  // Media Search: "Search [query] on Spotify"
  const spotifyMatch = lowerCmd.match(/^search\s+(.+?)\s+on\s+spotify$/);
  if (spotifyMatch) {
    const query = encodeURIComponent(spotifyMatch[1].trim());
    return {
      action: `Searching ${spotifyMatch[1]} on Spotify.`,
      url: `https://open.spotify.com/search/${query}`,
      isBrowserAction: true,
    };
  }

  // WhatsApp via MacroDroid: "whatsapp per [contact] ko bolo ki [message]"
  const waMatch = lowerCmd.match(
    /(?:send\s+a\s+whats?app?\s+message\s+to|whats?app?\s+p[ea]r)\s+(.+?)\s+(?:saying|ko\s+bolo\s+k[io])\s+(.+)$/i
  );
  if (waMatch) {
    const contactRaw = waMatch[1].trim();
    const messageRaw = waMatch[2].trim();
    
    const contact = encodeURIComponent(contactRaw);
    const message = encodeURIComponent(messageRaw);
    
    // Using MacroDroid Webhook silently
    const deviceId = (import.meta as any).env.VITE_MACRODROID_DEVICE_ID || "0cdf99e6-ca05-4d8b-839a-07c426336cc9";
    
    if (deviceId) {
      return {
        action: `Sending your WhatsApp message to ${contactRaw} via MacroDroid, boss.`,
        webhookUrl: `https://trigger.macrodroid.com/${deviceId}/ruhi_whatsapp?contact=${contact}&message=${message}`,
        isBrowserAction: true,
      };
    }
  }

  return { action: "", isBrowserAction: false };
}

