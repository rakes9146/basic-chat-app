package com.message.message_service.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MessageReadEvent Tests")
class MessageReadEventTest {

    private MessageReadEvent messageReadEvent;
    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private Instant readAt;

    @BeforeEach
    void setUp() {
        messageId = 1L;
        senderId = 100L;
        receiverId = 200L;
        readAt = Instant.now();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("No-argument constructor should create empty instance")
        void testNoArgsConstructor() {
            messageReadEvent = new MessageReadEvent();
            assertNotNull(messageReadEvent);
            assertNull(messageReadEvent.getMessageId());
            assertNull(messageReadEvent.getSenderId());
            assertNull(messageReadEvent.getReceiverId());
            assertFalse(messageReadEvent.isRead());
            assertNull(messageReadEvent.getReadAt());
        }

        @Test
        @DisplayName("All-argument constructor should set all fields")
        void testAllArgsConstructor() {
            messageReadEvent = new MessageReadEvent(messageId, senderId, receiverId, true, readAt);
            
            assertEquals(messageId, messageReadEvent.getMessageId());
            assertEquals(senderId, messageReadEvent.getSenderId());
            assertEquals(receiverId, messageReadEvent.getReceiverId());
            assertTrue(messageReadEvent.isRead());
            assertEquals(readAt, messageReadEvent.getReadAt());
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        @BeforeEach
        void setUp() {
            messageReadEvent = new MessageReadEvent();
        }

        @Test
        @DisplayName("setMessageId and getMessageId should work correctly")
        void testMessageIdGetterSetter() {
            messageReadEvent.setMessageId(messageId);
            assertEquals(messageId, messageReadEvent.getMessageId());
        }

        @Test
        @DisplayName("setSenderId and getSenderId should work correctly")
        void testSenderIdGetterSetter() {
            messageReadEvent.setSenderId(senderId);
            assertEquals(senderId, messageReadEvent.getSenderId());
        }

        @Test
        @DisplayName("setReceiverId and getReceiverId should work correctly")
        void testReceiverIdGetterSetter() {
            messageReadEvent.setReceiverId(receiverId);
            assertEquals(receiverId, messageReadEvent.getReceiverId());
        }

        @Test
        @DisplayName("setRead and isRead should work correctly")
        void testReadGetterSetter() {
            messageReadEvent.setRead(true);
            assertTrue(messageReadEvent.isRead());
            
            messageReadEvent.setRead(false);
            assertFalse(messageReadEvent.isRead());
        }

        @Test
        @DisplayName("setReadAt and getReadAt should work correctly")
        void testReadAtGetterSetter() {
            messageReadEvent.setReadAt(readAt);
            assertEquals(readAt, messageReadEvent.getReadAt());
        }
    }

    @Nested
    @DisplayName("Field Initialization Tests")
    class FieldInitializationTests {

        @Test
        @DisplayName("All fields should initialize correctly with constructor")
        void testCompleteInitialization() {
            messageReadEvent = new MessageReadEvent(messageId, senderId, receiverId, true, readAt);
            
            assertAll("Verify all fields are initialized",
                    () -> assertEquals(messageId, messageReadEvent.getMessageId()),
                    () -> assertEquals(senderId, messageReadEvent.getSenderId()),
                    () -> assertEquals(receiverId, messageReadEvent.getReceiverId()),
                    () -> assertTrue(messageReadEvent.isRead()),
                    () -> assertEquals(readAt, messageReadEvent.getReadAt())
            );
        }

        @Test
        @DisplayName("Fields should be independent and not affect each other")
        void testFieldIndependence() {
            messageReadEvent = new MessageReadEvent();
            messageReadEvent.setMessageId(1L);
            messageReadEvent.setSenderId(2L);
            messageReadEvent.setReceiverId(3L);
            
            assertEquals(1L, messageReadEvent.getMessageId());
            assertEquals(2L, messageReadEvent.getSenderId());
            assertEquals(3L, messageReadEvent.getReceiverId());
            assertNotEquals(messageReadEvent.getMessageId(), messageReadEvent.getSenderId());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null values in setters")
        void testNullValues() {
            messageReadEvent = new MessageReadEvent();
            messageReadEvent.setMessageId(null);
            messageReadEvent.setSenderId(null);
            messageReadEvent.setReceiverId(null);
            messageReadEvent.setReadAt(null);
            
            assertNull(messageReadEvent.getMessageId());
            assertNull(messageReadEvent.getSenderId());
            assertNull(messageReadEvent.getReceiverId());
            assertNull(messageReadEvent.getReadAt());
        }

        @Test
        @DisplayName("Should handle large ID values")
        void testLargeIdValues() {
            Long largeId = Long.MAX_VALUE;
            messageReadEvent = new MessageReadEvent();
            messageReadEvent.setMessageId(largeId);
            messageReadEvent.setSenderId(largeId);
            messageReadEvent.setReceiverId(largeId);
            
            assertEquals(largeId, messageReadEvent.getMessageId());
            assertEquals(largeId, messageReadEvent.getSenderId());
            assertEquals(largeId, messageReadEvent.getReceiverId());
        }

        @Test
        @DisplayName("Should handle zero and negative ID values")
        void testZeroAndNegativeIds() {
            messageReadEvent = new MessageReadEvent();
            messageReadEvent.setMessageId(0L);
            messageReadEvent.setSenderId(-1L);
            messageReadEvent.setReceiverId(-100L);
            
            assertEquals(0L, messageReadEvent.getMessageId());
            assertEquals(-1L, messageReadEvent.getSenderId());
            assertEquals(-100L, messageReadEvent.getReceiverId());
        }

        @Test
        @DisplayName("Should handle instant in past and future")
        void testInstantValues() {
            Instant pastInstant = Instant.parse("2020-01-01T00:00:00Z");
            Instant futureInstant = Instant.parse("2050-12-31T23:59:59Z");
            
            messageReadEvent = new MessageReadEvent();
            messageReadEvent.setReadAt(pastInstant);
            assertEquals(pastInstant, messageReadEvent.getReadAt());
            
            messageReadEvent.setReadAt(futureInstant);
            assertEquals(futureInstant, messageReadEvent.getReadAt());
        }
    }

    @Nested
    @DisplayName("Object Equality and State Tests")
    class ObjectStateTests {

        @Test
        @DisplayName("Two instances with same values should be created independently")
        void testIndependentInstances() {
            MessageReadEvent event1 = new MessageReadEvent(messageId, senderId, receiverId, true, readAt);
            MessageReadEvent event2 = new MessageReadEvent(messageId, senderId, receiverId, true, readAt);
            
            assertNotSame(event1, event2);
            assertEquals(event1.getMessageId(), event2.getMessageId());
            assertEquals(event1.getSenderId(), event2.getSenderId());
            assertEquals(event1.getReceiverId(), event2.getReceiverId());
            assertEquals(event1.isRead(), event2.isRead());
            assertEquals(event1.getReadAt(), event2.getReadAt());
        }

        @Test
        @DisplayName("Modifying one instance should not affect another")
        void testInstanceIsolation() {
            MessageReadEvent event1 = new MessageReadEvent(messageId, senderId, receiverId, true, readAt);
            MessageReadEvent event2 = new MessageReadEvent(messageId, senderId, receiverId, true, readAt);
            
            event1.setMessageId(999L);
            event1.setRead(false);
            
            assertEquals(999L, event1.getMessageId());
            assertFalse(event1.isRead());
            assertEquals(messageId, event2.getMessageId());
            assertTrue(event2.isRead());
        }
    }

    @Nested
    @DisplayName("Boolean Field Tests")
    class BooleanFieldTests {

        @BeforeEach
        void setUp() {
            messageReadEvent = new MessageReadEvent();
        }

        @Test
        @DisplayName("isRead should return false by default")
        void testReadDefaultValue() {
            assertFalse(messageReadEvent.isRead());
        }

        @Test
        @DisplayName("setRead should toggle boolean value")
        void testReadToggle() {
            assertFalse(messageReadEvent.isRead());
            messageReadEvent.setRead(true);
            assertTrue(messageReadEvent.isRead());
            messageReadEvent.setRead(false);
            assertFalse(messageReadEvent.isRead());
        }

        @Test
        @DisplayName("Multiple setRead calls should work correctly")
        void testMultipleReadUpdates() {
            for (int i = 0; i < 5; i++) {
                messageReadEvent.setRead(true);
                assertTrue(messageReadEvent.isRead());
                messageReadEvent.setRead(false);
                assertFalse(messageReadEvent.isRead());
            }
        }
    }

    @Nested
    @DisplayName("Real-world Scenario Tests")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("Simulate complete message read event workflow")
        void testCompleteMessageReadWorkflow() {
            // Simulate a receiver reading a message
            MessageReadEvent event = new MessageReadEvent();
            event.setMessageId(1L);
            event.setSenderId(100L);
            event.setReceiverId(200L);
            event.setRead(true);
            event.setReadAt(Instant.now());
            
            assertAll("Complete workflow",
                    () -> assertEquals(1L, event.getMessageId()),
                    () -> assertEquals(100L, event.getSenderId()),
                    () -> assertEquals(200L, event.getReceiverId()),
                    () -> assertTrue(event.isRead()),
                    () -> assertNotNull(event.getReadAt())
            );
        }

        @Test
        @DisplayName("Simulate multiple message read events")
        void testMultipleMessageReadEvents() {
            MessageReadEvent event1 = new MessageReadEvent(1L, 100L, 200L, true, Instant.now());
            MessageReadEvent event2 = new MessageReadEvent(2L, 100L, 200L, true, Instant.now());
            MessageReadEvent event3 = new MessageReadEvent(3L, 100L, 200L, true, Instant.now());
            
            assertAll("Multiple events",
                    () -> assertEquals(1L, event1.getMessageId()),
                    () -> assertEquals(2L, event2.getMessageId()),
                    () -> assertEquals(3L, event3.getMessageId()),
                    () -> assertEquals(100L, event1.getSenderId()),
                    () -> assertEquals(100L, event2.getSenderId()),
                    () -> assertEquals(100L, event3.getSenderId())
            );
        }
    }
}
