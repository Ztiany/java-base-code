package chapter6.queue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockingQueueTest {

    public static void main(String[] args) {
//		testArrayBlockingQueue();
//		testLinkedBlockingQueue();
//		testPriorityBlockingQueue();
//		testDelayQueue();
//		testLinkedTransferQueue();
        testLinkedBlockingDeque();

    }

    private static void testLinkedBlockingDeque() {
        LinkedBlockingDeque<String> strings = new LinkedBlockingDeque<String>(10);
    }

    private static void testLinkedTransferQueue() {
        final LinkedTransferQueue<String> strings = new LinkedTransferQueue<String>();
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(5000);
                    String poll = strings.poll();
                    System.err.println(poll);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            ;
        }.start();
        try {
            strings.transfer("b");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static void testDelayQueue() {

        DelayQueue<DelayedTask> delayedTasks = new DelayQueue<DelayedTask>();

        delayedTasks.add(new DelayedTask("task 1", System.nanoTime() + TimeUnit.SECONDS.toNanos(10)));
        delayedTasks.add(new DelayedTask("task 2", System.nanoTime() + TimeUnit.SECONDS.toNanos(5)));
        delayedTasks.add(new DelayedTask("task 3", System.nanoTime() + TimeUnit.SECONDS.toNanos(3)));
        delayedTasks.add(new DelayedTask("task 4", System.nanoTime() + TimeUnit.SECONDS.toNanos(7)));
        delayedTasks.add(new DelayedTask("task 5", System.nanoTime() + TimeUnit.SECONDS.toNanos(6)));
        delayedTasks.add(new DelayedTask("task 6", System.nanoTime() + TimeUnit.SECONDS.toNanos(2)));

        while (!delayedTasks.isEmpty()) {
            DelayedTask poll = delayedTasks.poll();
            if (poll != null) {
                poll.start();
            }
        }
    }

    private static void testPriorityBlockingQueue() {
        // ??????????????????????????????????????????????????????????????????????????????10
        PriorityBlockingQueue<String> priorityBlockingQueue = new PriorityBlockingQueue<String>(
                10);
    }

    private static void testLinkedBlockingQueue() {
        // ???????????????????????????
        LinkedBlockingQueue<String> linkedBlockingQueue = new LinkedBlockingQueue<String>();
    }

    private static void testArrayBlockingQueue() {
        // capacity ????????????
        ArrayBlockingQueue<String> arrayBlockingQueue = new ArrayBlockingQueue<String>(
                10);
        // fair ????????????????????????????????????
        ArrayBlockingQueue<String> fairArrayBlockingQueue = new ArrayBlockingQueue<String>(
                10, true);
    }

}
