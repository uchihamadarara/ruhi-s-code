import { GoogleGenAI } from "@google/genai";

async function main() {
  const ai = new GoogleGenAI({ apiKey: "dummy" });
  try {
    const session = await ai.live.connect({ 
      model: "gemini-3.1-flash-live-preview",
      config: {
        speechConfig: { voiceConfig: { prebuiltVoiceConfig: { voiceName: "Aoede" } } }
      }
    });
    console.log("Success with Aoede");
    session.close();
  } catch (e) {
    console.error("Error with Aoede:", e);
  }
}
main();
