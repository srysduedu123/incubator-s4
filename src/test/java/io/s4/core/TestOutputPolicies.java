package io.s4.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestOutputPolicies extends TestCase {

    private static final Logger logger = LoggerFactory
            .getLogger(TestOutputPolicies.class);

    static int[] values = { 111, 222, 333, 444, 555 };
    static Map<String, Long> results = new HashMap<String, Long>();
    static {
        results.put("111", 37l);
        results.put("222", 39l);
        results.put("333", 36l);
        results.put("444", 47l);
        results.put("555", 41l);
    }

    public void testTimeInterval() {

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.TRACE);

        MyApp myApp = new MyApp();
        myApp.init();
        myApp.start();

    }

    public class MyApp extends App {

        private GenerateTestEventPE generateTestEventPE;
        private CounterPE counterPE;

        /*
         * Build the application graph using POJOs. Don't like it? Write a nice
         * tool.
         * 
         * @see io.s4.App#init()
         */
        @SuppressWarnings("unchecked")
        @Override
        protected void init() {

            counterPE = new CounterPE(this);

            // counterPE.setOutputIntervalInEvents(1);
            counterPE.setOutputInterval(20, TimeUnit.MILLISECONDS, false);

            Stream<TestEvent> testStream = new Stream<TestEvent>(this,
                    "Test Stream", new TestKeyFinder(), counterPE);

            generateTestEventPE = new GenerateTestEventPE(this, testStream);

        }

        @Override
        protected void start() {

            for (int i = 0; i < 200; i++) {
                generateTestEventPE.processOutputEvent(null);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            logger.info("Check results.");
            Collection<ProcessingElement> pes = counterPE.getInstances();

            for (ProcessingElement pe : pes) {
                counterPE = (CounterPE) pe;
                logger.info("Final count for {} is {}.", pe.id,
                        counterPE.getCount());
                Assert.assertEquals(results.get(pe.id).longValue(),
                        counterPE.getCount());
            }

            logger.info("Start to close resources...");
            removeAll();

        }

        @Override
        protected void close() {
            System.out.println("Bye.");

        }
    }

    public class GenerateTestEventPE extends SingletonPE {

        final private Stream<TestEvent>[] targetStreams;
        final private Random generator = new Random(100);
        private int count = 0;

        public GenerateTestEventPE(App app, Stream<TestEvent>... targetStreams) {
            super(app);
            this.targetStreams = targetStreams;
        }

        /* 
         * 
         */
        @Override
        protected void processInputEvent(Event event) {

        }

        @Override
        public void processOutputEvent(Event event) {

            int indexValue = generator.nextInt(values.length);

            int value = values[indexValue];
            String key = String.valueOf(value);

            TestEvent testEvent = new TestEvent(key, count++);

            for (int i = 0; i < targetStreams.length; i++) {
                targetStreams[i].put(testEvent);
            }
        }

        @Override
        protected void onRemove() {
        }
    }

    public class CounterPE extends ProcessingElement {

        public CounterPE(App app) {
            super(app);
        }

        private long counter = 0;

        @Override
        protected void processInputEvent(Event event) {

            counter += 1;
        }

        @Override
        public void processOutputEvent(Event event) {
            logger.info(String.format("PE ID: %4s  - Count: %4d", id, counter));
        }

        @Override
        protected void onCreate() {
        }

        @Override
        protected void onRemove() {

        }

        public long getCount() {
            return counter;
        }
    }

    public class TestEvent extends Event {
        final private String key;
        final private long value;

        TestEvent(String key, long count) {
            this.key = key;
            this.value = count;
        }

        /**
         * @return the key
         */
        public String getKey() {
            return key;
        }

        /**
         * @return the count
         */
        public long getCount() {
            return value;
        }
    }

    public class TestKeyFinder implements KeyFinder<TestEvent> {

        public List<String> get(TestEvent event) {

            List<String> results = new ArrayList<String>();

            /* Retrieve the user ID and add it to the list. */
            results.add(event.getKey());

            return results;
        }

    }
}