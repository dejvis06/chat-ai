# 🌱 Spring AI – Build Generative AI apps with Spring

Spring AI makes it easy to integrate AI models into your applications. Among all of its features, this project focuses on a simple flow:

1️⃣ **User sends a message** 📝  
2️⃣ **App passes the message to the Chat Client** 📡  
3️⃣ **Chat Client forwards it to the chosen Chat Model** 🧠  
4️⃣ **Chat Model generates a reply** — delivered all at once or streamed in real time ⚡

🧠 **Chat Model** — Supports major AI model providers like OpenAI, Google, and Ollama.  
📡 **Chat Client** — Connects to a Chat Model, returning either a synchronous or streaming reply.

### API

- 💬 **Create Chat** – Creates a new chat with an initial user prompt and returns its details.
- ⚡ **Stream AI Responses** – Streams AI-generated responses to the client in real time using Server-Sent Events (SSE).
- 📜 **Get Chat History** – Retrieves the complete history of messages for a given chat.
- 📋 **Get All Chats** – Retrieves a list of all chats.
- 📄 **Get Paginated Messages** – Retrieves messages from a specific chat with pagination support.  

