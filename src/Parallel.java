import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Parallel {
    public static void main(String[] args) throws IOException, InterruptedException {
        MyList synchronizedList = new MyList();
        MyQueue<Node> synchronizedQueue = new MyQueue<>();

        List<String> files = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            for (int j = 1; j <= 10; j++) {
                files.add("C:\\Facultate_informatica\\an3\\ppd\\tema5\\" + "C" + i + "_P" + j + ".txt");
            }
        }

        int P = 6;
        int p_r = 2;
        int p_w = P - p_r;

        int start, end = 0, cat, rest;
        int N = files.size();
        start = 0;
        cat = N / p_r;
        rest = N % p_r;

        ExecutorService producerPool = Executors.newFixedThreadPool(p_r);
        ExecutorService consumerPool = Executors.newFixedThreadPool(p_w);

        Lock lock = new ReentrantLock();
        Condition notFull = lock.newCondition();
        Condition notEmpty = lock.newCondition();

        List<Future<?>> producerFutures = new ArrayList<>();
        long start_t = System.nanoTime();

        for (int i = 0; i < p_r; i++) {
            end = start + cat;
            if (rest > 0) {
                end++;
                rest--;
            }
            int finalStart = start;
            int finalEnd = end;
            producerFutures.add(producerPool.submit(() -> {
                try {
                    Producer producer = new Producer(finalStart, finalEnd, synchronizedQueue, files, lock, notFull, notEmpty);
                    producer.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
            start = end;
        }

        for (int i = 0; i < p_w; i++) {
            consumerPool.submit(() -> {
                try {
                    Consumer consumer = new Consumer(synchronizedQueue, synchronizedList, lock, notFull, notEmpty);
                    consumer.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Așteaptă finalizarea tuturor producătorilor
        for (Future<?> future : producerFutures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Marchează că producătorii au terminat
        synchronizedQueue.setProducersFinished();

        // Oprește thread pool-urile
        producerPool.shutdown();
        consumerPool.shutdown();
        consumerPool.awaitTermination(1, TimeUnit.MINUTES);

        System.out.println("Am ieșit toți");
        synchronizedList.showList();

        Utils.writeResultParalell(synchronizedList, "ResultParallel.txt");
        long end_t = System.nanoTime();

        assert (Utils.areFilesEqual("Result.txt", "ResultParallel.txt"));

        System.out.println((double) (end_t - start_t) / 1000000);
    }

    public static class Producer {
        private static final Integer MAX_CAPACITY = 100;
        int start;
        int end;
        MyQueue<Node> queue;
        List<String> files;
        Lock lock;
        Condition notFull;
        Condition notEmpty;

        public Producer(int start, int end, MyQueue<Node> queue, List<String> files, Lock lock, Condition notFull, Condition notEmpty) {
            this.start = start;
            this.end = end;
            this.queue = queue;
            this.files = files;
            this.lock = lock;
            this.notFull = notFull;
            this.notEmpty = notEmpty;
        }

        public void readFromFileAddToQueue(String filePath){
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    int firstNumber = Integer.parseInt(parts[0].trim());
                    int secondNumber = Integer.parseInt(parts[1].trim());
                    Node node=new Node(firstNumber,secondNumber);
                    this.queue.enqueue(node);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void run() {
            for (int i = start; i < end; i++) {
                String file = files.get(i);
                readFromFileAddToQueue(file);
            }
        }
    }

    public static class Consumer {
        MyQueue<Node> queue;
        MyList list;
        Lock lock;
        Condition notFull;
        Condition notEmpty;

        public Consumer(MyQueue<Node> queue, MyList list, Lock lock, Condition notFull, Condition notEmpty) {
            this.queue = queue;
            this.list = list;
            this.lock = lock;
            this.notFull = notFull;
            this.notEmpty = notEmpty;
        }

        public void run() {
            try {
                while (true) {


                    Node nodeDequeue = this.queue.dequeue();
                    if(nodeDequeue == null) break;  // am terminat de citit

                    int firstNumber = nodeDequeue.getIdP();
                    int secondNumber = nodeDequeue.getScore();
                    list.addNode(firstNumber, secondNumber);


                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
