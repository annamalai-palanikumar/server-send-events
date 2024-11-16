package in.er.annamalai.listener;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class RedisListener implements MessageListener {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        String eventMessage = new String(message.getBody());
        // Broadcast the message to all connected clients via SSE
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("message").data(eventMessage));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }

    // Add a new emitter to the list
    public void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
    }

    // Remove an emitter from the list
    public void removeEmitter(SseEmitter emitter) {
        emitters.remove(emitter);
    }
}
