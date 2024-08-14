package org.example;

import com.azure.messaging.servicebus.*;

import java.util.concurrent.TimeUnit;


public class AzureServiceBusTopic {

    static String connectionString = "";
    static String topicName = "";
    static String subName = "";

    public static void main(String[] args) {
        AzureServiceBusTopic.sendMessage();
        try {
            AzureServiceBusTopic.receiveMessages();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void sendMessage()
    {
        // create a Service Bus Sender client for the topic
        ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .topicName(topicName)
                .buildClient();

        // send one message to the topic
        senderClient.sendMessage(new ServiceBusMessage("Hello, World!"));
        System.out.println("Sent a single message to the topic: " + topicName);
    }

    // handles received messages
    static void receiveMessages() throws InterruptedException
    {
        // Create an instance of the processor through the ServiceBusClientBuilder
        ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .topicName(topicName)
                .subscriptionName(subName)
                .processMessage(AzureServiceBusTopic::processMessage)
                .processError(AzureServiceBusTopic::processError)
                .buildProcessorClient();

        System.out.println("Starting the processor");
        processorClient.start();

        TimeUnit.SECONDS.sleep(10);
        System.out.println("Stopping and closing the processor");
        processorClient.close();
    }

    private static void processError(ServiceBusErrorContext serviceBusErrorContext) {
        System.out.println(serviceBusErrorContext);
    }

    private static void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        System.out.printf("Processing message. Session: %s, Sequence #: %s. Contents: %s%n", message.getMessageId(),
                message.getSequenceNumber(), message.getBody());
    }


}
