# Chat API

This API, built using **Spring AI**, provides endpoints to manage chats and their messages.

## Endpoints

- **Create Chat** – Creates a new chat with an initial user prompt and returns its details.
- **Stream AI Responses** – Streams AI-generated responses to the client in real time using Server-Sent Events (SSE).
- **Get Chat History** – Retrieves the complete history of messages for a given chat.
- **Get All Chats** – Retrieves a list of all chats.
- **Get Paginated Messages** – Retrieves messages from a specific chat with pagination support.
