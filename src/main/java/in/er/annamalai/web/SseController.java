package in.er.annamalai.web;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@EnableScheduling  // Enable scheduling of tasks
public class SseController {

    // Use a thread-safe list to store SseEmitter instances
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping("/events")
    public SseEmitter streamEvents() {
        // Create a new emitter for each client
        SseEmitter emitter = new SseEmitter();

        // Add the emitter to the list of active clients
        emitters.add(emitter);

        // Handle client disconnect
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((ex) -> emitters.remove(emitter));

        // Return the emitter to the client
        return emitter;
    }

    @Scheduled(fixedRate = 5000)  // Broadcast every 5 seconds
    public void sendBroadcastMessage() {
        broadcast("Broadcast Message at " + System.currentTimeMillis());
    }

    // This method simulates a broadcast event
    public void broadcast(String message) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("message").data(message));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }
}
