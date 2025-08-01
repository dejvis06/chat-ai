package com.ai.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class MockChatController {

    @GetMapping(value = "/mock/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMockResponse(@RequestParam String message) {
        SseEmitter emitter = new SseEmitter(0L); // 0L = no timeout

        new Thread(() -> {
            try {
                //  Mock "AI response" text
                String[] tokens = {
                        "Ahoy! ",
                        "Ye asked: ",
                        message,
                        ". ",
                        "Here be me answer 🏴‍☠️"
                };

                //  Send tokens one by one (simulating AI streaming)
                for (String token : tokens) {
                    emitter.send(token);
                }

                emitter.send(SseEmitter.event()
                        .name("end")
                        .data("done"));
                emitter.complete(); //  Mark SSE as done
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}
