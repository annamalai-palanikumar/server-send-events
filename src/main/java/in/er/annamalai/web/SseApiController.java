package in.er.annamalai.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import in.er.annamalai.listener.RedisListener;

@RestController
public class SseApiController {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisListener redisListener;

    @GetMapping("/events")
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter();

        // Add the emitter to the list so it can receive broadcast messages
        redisListener.addEmitter(emitter);

        // Cleanup emitter when it finishes or encounters an error
        emitter.onCompletion(() -> redisListener.removeEmitter(emitter));
        emitter.onTimeout(() -> redisListener.removeEmitter(emitter));
        emitter.onError((ex) -> redisListener.removeEmitter(emitter));

        return emitter;
    }

    // Broadcast the message received from the front-end to all clients via Redis
    @PostMapping("/broadcast")
    public String broadcastMessage(@RequestBody BroadcastMessage message) {
        // Publish the message to the Redis channel
        redisTemplate.convertAndSend("sse-channel", message.getMessage());
        return "Message broadcasted!";
    }

    // BroadcastMessage class to bind incoming JSON data
    public static class BroadcastMessage {
        private String message;

        // Getter and setter
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
