package org.example;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import com.azure.storage.queue.models.QueueProperties;
import com.azure.storage.queue.models.SendMessageResult;

import java.time.Duration;

public class AzureStorageQueue {

    public static void main(String[] args) {
        // Your connection string
        String connectionString = "";
        // The name of the queue you want to connect to
        String queueName = "";

        // Create a QueueClient
        QueueClient queueClient = new QueueClientBuilder()
                .connectionString(connectionString)
                .queueName(queueName)
                .buildClient();

        // Create the queue if it doesn't exist
        queueClient.create();

//         Send a message to the queue
        String message = "message is there";
        String message2 = "expiring message";
        SendMessageResult result = queueClient.sendMessageWithResponse(message2, Duration.ZERO, Duration.ofMinutes(1), null, null).getValue();
        System.out.println(result.getMessageId());
        queueClient.updateMessage(result.getMessageId(),
                result.getPopReceipt(),
                "Third message has been updated",
                Duration.ofSeconds(1));
        queueClient.sendMessage(message2);
        System.out.println("Sent message: " + message + message2);

//         Receive messages from the queue
        for (QueueMessageItem messageItem : queueClient.receiveMessages(5)) {
            System.out.println("Received message: " + messageItem.getBody().toString());

            // Delete the message after processing
        //   queueClient.deleteMessage(messageItem.getMessageId(), messageItem.getPopReceipt());
        }

        QueueProperties properties = queueClient.getProperties();
        long messageCount = properties.getApproximateMessagesCount();

        System.out.printf("Queue length: %d%n", messageCount);
    }
}
