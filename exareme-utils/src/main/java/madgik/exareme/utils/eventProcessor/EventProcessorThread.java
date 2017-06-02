/**
 * Copyright MaDgIK Group 2010 - 2015.
 */
package madgik.exareme.utils.eventProcessor;


import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventProcessorThread extends Thread {

    private static Logger log = LoggerFactory.getLogger(EventProcessorThread.class);
    private EventQueue eventQueue = null;
    private ExecutorService executor = null;
    private EventProcessor processor = null;

    public EventProcessorThread(EventQueue eventQueue, ExecutorService executor,
        EventProcessor processor) {
        this.eventQueue = eventQueue;
        this.executor = executor;
        this.processor = processor;
    }

    @Override public void run() {
        while (true) {
            try {
                ActiveEvent next = eventQueue.getNext();
                EventHandlerRunnable job = new EventHandlerRunnable(next, processor);
                executor.submit(job);
            } catch (Exception e) {
                log.error("Cannot run event", e);
            }
        }
    }
}
