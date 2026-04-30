import { GoogleGenAI } from "@google/genai";

async function main() {
  const ai = new GoogleGenAI({ apiKey: "dummy" });
  const session = await ai.live.connect({ model: "gemini-3.1-flash-live-preview" });
  console.log(Object.getOwnPropertyNames(Object.getPrototypeOf(session)));
  session.close();
}
main();
