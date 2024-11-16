package in.er.annamalai.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

import in.er.annamalai.listener.RedisListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import redis.embedded.RedisServer;

@Configuration
@EnableRedisRepositories
public class RedisConfiguration {

    private RedisServer redisServer;

    private RedisListener redisListener;

    public RedisConfiguration(RedisProperties redisProperties, RedisListener redisListener) throws IOException {
        this.redisServer = new RedisServer(redisProperties.getRedisPort());
        this.redisListener = redisListener;
    }

    @PostConstruct
    public void postConstruct() throws IOException {
        try {
            redisServer.start();
        } catch(Exception exception) {
            boolean isProcessKilled = false;
            for(int port: redisServer.ports()) {
                String pid = getPidByPort(port);
                if (pid != null) {
                    killProcess(pid);
                    isProcessKilled = true;
                }
            }
            if(isProcessKilled) {
                redisServer.start();
            }
        }
    }

    @PreDestroy
    public void preDestroy() throws IOException {
        redisServer.stop();
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        return new LettuceConnectionFactory(
          redisProperties.getRedisHost(), 
          redisProperties.getRedisPort());
    }

    @Bean
    public RedisTemplate<?, ?> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<byte[], byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(LettuceConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(new MessageListenerAdapter(redisListener), new ChannelTopic("sse-channel"));
        return container;
    }

    // Method to get the PID of the process running on a specific port (Linux/macOS)
    private static String getPidByPort(int port) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        String pid = null;

        if (os.contains("win")) {
            // Windows command to find PID
            String command = "netstat -ano | findstr :" + port;
            Process process = Runtime.getRuntime().exec(command);
            pid = extractPidFromWindows(process);
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            // Unix-based (Linux/macOS) command to find PID
            String command = "lsof -i :" + port;
            Process process = Runtime.getRuntime().exec(command);
            pid = extractPidFromUnix(process);
        }

        return pid;
    }

    // Extract PID from Windows command output
    private static String extractPidFromWindows(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            // Extract PID from the output
            String[] tokens = line.split("\\s+");
            if (tokens.length >= 5) {
                return tokens[tokens.length - 1]; // PID is the last column
            }
        }
        return null;
    }

    // Extract PID from Unix-based command output (Linux/macOS)
    private static String extractPidFromUnix(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("COMMAND") || line.trim().isEmpty()) {
                continue; // Skip header line or empty lines
            }
            // The second column in the output is the PID
            String[] tokens = line.split("\\s+");
            if (tokens.length > 1) {
                return tokens[1]; // PID is in the second column
            }
        }
        return null;
    }

    // Method to kill the process using the PID (Linux/macOS/Windows)
    private static void killProcess(String pid) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // For Windows
            String command = "taskkill /PID " + pid + " /F";
            Runtime.getRuntime().exec(command);
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            // For Linux/macOS
            String command = "kill -9 " + pid;
            Runtime.getRuntime().exec(command);
        }
    }
}
