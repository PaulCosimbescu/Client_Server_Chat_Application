package Test;

import ChatApplication.ChatClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatClientTest {

    private ChatClient chatClientUnderTest;

    @BeforeEach
    void setUp() {
        chatClientUnderTest = new ChatClient();
    }

    @Test
    void testRun() throws Exception {
        // Setup

        // Run the test
        chatClientUnderTest.run();

        // Verify the results
    }

    @Test
    void testRun_ThrowsIOException() {
        // Setup

        // Run the test
        assertThrows(IOException.class, () -> {
            chatClientUnderTest.run();
        });
    }

    @Test
    void testSetPort() {

    }

    @Test
    void testMain() throws Exception {
        // Setup

        // Run the test
        ChatClient.main(new String[]{"value"});

        // Verify the results
    }

    @Test
    void testMain_ThrowsException() {
        // Setup

        // Run the test
        assertThrows(Exception.class, () -> {
            ChatClient.main(new String[]{"value"});
        });
    }
}
