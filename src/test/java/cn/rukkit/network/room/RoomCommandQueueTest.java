package cn.rukkit.network.room;

import cn.rukkit.network.command.GameCommand;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomCommandQueueTest {
    @Test
    void drainPreservesFifoOrder() {
        RoomCommandQueue queue = new RoomCommandQueue();
        GameCommand first = command(1);
        GameCommand second = command(2);

        queue.addLast(first);
        queue.addLast(second);

        List<GameCommand> batch = queue.drain();

        assertEquals(2, batch.size());
        assertSame(first, batch.get(0));
        assertSame(second, batch.get(1));
        assertTrue(queue.isEmpty());
    }

    @Test
    void prependRestoresFailedBatchBeforeNewCommands() {
        RoomCommandQueue queue = new RoomCommandQueue();
        GameCommand first = command(1);
        GameCommand second = command(2);
        GameCommand later = command(3);

        queue.addLast(first);
        queue.addLast(second);
        List<GameCommand> batch = queue.drain();
        queue.addLast(later);
        queue.prepend(batch);

        List<GameCommand> restored = queue.drain();
        assertEquals(List.of(first, second, later), restored);
    }

    @Test
    void concurrentProducersDoNotLoseCommands() throws Exception {
        RoomCommandQueue queue = new RoomCommandQueue();
        int producerCount = 4;
        int commandsPerProducer = 250;
        ExecutorService executor = Executors.newFixedThreadPool(producerCount);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int producer = 0; producer < producerCount; producer++) {
                int producerId = producer;
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < commandsPerProducer; i++) {
                        queue.addLast(command(producerId * commandsPerProducer + i));
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(producerCount * commandsPerProducer, queue.size());
        assertEquals(producerCount * commandsPerProducer, queue.drain().size());
    }

    private static GameCommand command(int marker) {
        GameCommand command = new GameCommand();
        command.arr = new byte[] {(byte) marker};
        return command;
    }
}
