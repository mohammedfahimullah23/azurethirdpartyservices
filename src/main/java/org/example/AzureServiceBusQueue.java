package org.example;

import com.azure.messaging.servicebus.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AzureServiceBusQueue {
    static String connectionString = "";
    static String queueName = "";

    public static void main(String[] args) throws InterruptedException {
        // send one message to the queue
//        sendMessage();
        sendMessageBatch();
//        receiveMessages();
    }

    static void sendMessage() throws InterruptedException {
        // create a Service Bus Sender client for the queue
        ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();


    }

    static void sendMessageBatch() {
        // create a Service Bus Sender client for the queue
        ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();

        // Creates an ServiceBusMessageBatch where the ServiceBus.
        ServiceBusMessageBatch messageBatch = senderClient.createMessageBatch();

        // create a list of messages
        List<ServiceBusMessage> listOfMessages = createMessages();

        // We try to add as many messages as a batch can fit based on the maximum size and send to Service Bus when
        // the batch can hold no more messages. Create a new batch for next set of messages and repeat until all
        // messages are sent.
        for (ServiceBusMessage message : listOfMessages) {
            if (messageBatch.tryAddMessage(message)) {
                continue;
            }

            // The batch is full, so we create a new batch and send the batch.
            senderClient.sendMessages(messageBatch);
            System.out.println("Sent a batch of messages to the queue: " + queueName);

            // create a new batch
            messageBatch = senderClient.createMessageBatch();

            // Add that message that we couldn't before.
            if (!messageBatch.tryAddMessage(message)) {
                System.err.printf("Message is too large for an empty batch. Skipping. Max size: %s.", messageBatch.getMaxSizeInBytes());
            }
        }

        if (messageBatch.getCount() > 0) {
            senderClient.sendMessages(messageBatch);
            System.out.println("Sent a batch of messages to the queue: " + queueName);
        }

        //close the client
        senderClient.close();
    }

    static List<ServiceBusMessage> createMessages() {
        // create a list of messages and return it to the caller
        ServiceBusMessage message1 = new ServiceBusMessage("122 message");
        message1.setTimeToLive(Duration.ofSeconds(10));
        message1.getApplicationProperties().put("Priority", "High");
        message1.getApplicationProperties().put("Category", "Greeting");
        message1.setMessageId("1");

        ServiceBusMessage message2 = new ServiceBusMessage("222 message");
        message2.getApplicationProperties().put("Priority", "Medium");
        message2.getApplicationProperties().put("Category", "Update");
        message2.setTimeToLive(Duration.ofSeconds(10));
        message2.setMessageId("2");

        ServiceBusMessage message3 = new ServiceBusMessage("222 message");
        message3.getApplicationProperties().put("Priority", "Low");
        message3.getApplicationProperties().put("Category", "Alert");
        message3.setMessageId("3");

        return Arrays.asList(message1, message2, message3);
    }

    // handles received messages
    static void receiveMessages() throws InterruptedException {
        // Create an instance of the processor through the ServiceBusClientBuilder
//        This processor client automatically takes care of the
        ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName(queueName)
                .processMessage(System.out::println)
                .processError(AzureServiceBusQueue::processError)
                .buildProcessorClient();

        System.out.println("Starting the processor");
        processorClient.start();

        TimeUnit.SECONDS.sleep(10);
        System.out.println("Stopping and closing the processor");
        processorClient.close();
    }

    private static void processError(ServiceBusErrorContext context) {
        System.out.printf("Error when receiving messages from namespace: '%s'. Entity: '%s'%n",
                context.getFullyQualifiedNamespace(), context.getEntityPath());

        if (!(context.getException() instanceof ServiceBusException)) {
            System.out.printf("Non-ServiceBusException occurred: %s%n", context.getException());
            return;
        }

        ServiceBusException exception = (ServiceBusException) context.getException();
        ServiceBusFailureReason reason = exception.getReason();

        if (reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED
                || reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND
                || reason == ServiceBusFailureReason.UNAUTHORIZED) {
            System.out.printf("An unrecoverable error occurred. Stopping processing with reason %s: %s%n",
                    reason, exception.getMessage());
        } else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
            System.out.printf("Message lock lost for message: %s%n", context.getException());
        } else if (reason == ServiceBusFailureReason.SERVICE_BUSY) {
            try {
                // Choosing an arbitrary amount of time to wait until trying again.
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                System.err.println("Unable to sleep for period of time");
            }
        } else {
            System.out.printf("Error source %s, reason %s, message: %s%n", context.getErrorSource(),
                    reason, context.getException());
        }
    }


}
