package Test;

import ChatApplication.ChatServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatServerTest {

    @Test
    void testMain() throws Exception {
        // Setup

        // Run the test
        ChatServer.main(new String[]{"value"});

        // Verify the results
    }

    @Test
    void testMain_ThrowsException() {
        // Setup

        // Run the test
        assertThrows(Exception.class, () -> {
            ChatServer.main(new String[]{"value"});
        });
    }
}
