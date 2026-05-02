import { GoogleGenAI } from "@google/genai";
import { getSystemInstruction } from "../utils/systemPrompt";

let chatSession: any = null;

export function resetRuhiSession() {
  chatSession = null;
}

export async function getRuhiResponse(prompt: string, history: { sender: "user" | "ruhi", text: string }[] = [], accent: string = "default"): Promise<string> {
  try {
    const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
    
    if (!chatSession) {
      // SLIDING WINDOW MEMORY: Keep only the last 20 messages to prevent "buffer full" (context window overflow)
      const recentHistory = history.slice(-20);
      
      let formattedHistory: any[] = [];
      let currentRole = "";
      let currentText = "";

      for (const msg of recentHistory) {
        const role = msg.sender === "user" ? "user" : "model";
        if (role === currentRole) {
          currentText += "\n" + msg.text;
        } else {
          if (currentRole !== "") {
            formattedHistory.push({ role: currentRole, parts: [{ text: currentText }] });
          }
          currentRole = role;
          currentText = msg.text;
        }
      }
      if (currentRole !== "") {
        formattedHistory.push({ role: currentRole, parts: [{ text: currentText }] });
      }

      if (formattedHistory.length > 0 && formattedHistory[0].role !== "user") {
        formattedHistory.shift();
      }

      chatSession = ai.chats.create({
        model: "gemini-3.1-flash-lite-preview",
        config: {
          systemInstruction: getSystemInstruction(accent),
        },
        history: formattedHistory,
      });
    }

    const response = await chatSession.sendMessage({ message: prompt });
    return response.text || "Ugh, fine. I have nothing to say.";
  } catch (error) {
    console.error("Gemini Error:", error);
    return "Uff, mera dimaag kharab ho gaya hai. Try again later, Dinesh.";
  }
}

export async function getRuhiAudio(text: string): Promise<string | null> {
  try {
    const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
    const response = await ai.models.generateContent({
      model: "gemini-2.5-flash-preview-tts",
      contents: [{ parts: [{ text }] }],
      config: {
        responseModalities: ["AUDIO"],
        speechConfig: {
          voiceConfig: {
            prebuiltVoiceConfig: { voiceName: "Kore" },
          },
        },
      },
    });
    
    if (!response.candidates?.[0]?.content?.parts?.[0]?.inlineData?.data) {
      throw new Error("Invalid or empty audio response from Gemini TTS API.");
    }
    
    return response.candidates[0].content.parts[0].inlineData.data;
  } catch (error) {
    if (error instanceof Error) {
      console.error(`TTS Error in getRuhiAudio: ${error.message}\nStack: ${error.stack}`);
    } else {
      console.error("Unknown TTS Error in getRuhiAudio:", error);
    }
    return "ammm, sorry boss muje kuch sunai nahi diya.";
  }
}
