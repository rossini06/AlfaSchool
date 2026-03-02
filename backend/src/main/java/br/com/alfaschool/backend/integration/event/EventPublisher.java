package br.com.alfaschool.backend.integration.event;

public interface EventPublisher {
    void publish(String eventType, Object payload);
}
