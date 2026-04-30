import { GoogleGenAI } from "@google/genai";

async function main() {
  const ai = new GoogleGenAI({ apiKey: "dummy" });
  try {
    const session = await ai.live.connect({ 
      model: "gemini-3.1-flash-live-preview",
      config: {
        speechConfig: { voiceConfig: { prebuiltVoiceConfig: { voiceName: "Puck" } } }
      },
      callbacks: {
        onmessage: (message) => {
          console.log("Got message from Live API!", JSON.stringify(message).substring(0, 200));
        },
        onerror: (err) => {
          console.error("Live API Error =>", err);
        },
        onclose: () => {
          console.log("Live API Closed =(");
        }
      }
    });

    console.log("Session connected. Sending audio chunk.");
    
    // send dummy audio
    const base64Data = Buffer.alloc(4096).toString("base64");
    session.sendRealtimeInput({
      audio: { data: base64Data, mimeType: 'audio/pcm;rate=16000' }
    });
    
    session.sendRealtimeInput({
      text: "Hello!"
    });
    
    // Use session.receive() since session is an async generator?
    // Let's just hook to onmessage

    
    setTimeout(() => { session.close(); }, 5000);
  } catch (e) {
    console.error("Connection failed:", e);
  }
}
main();
