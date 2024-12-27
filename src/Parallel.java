import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Pair {
    public int id;
    public String country;

    Pair(int id, String country) {
        this.id = id;
        this.country = country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair pair = (Pair) o; // Cast explicit
        return Objects.equals(id, pair.id) && Objects.equals(country, pair.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, country);
    }

}

class MyListBlack {
    List<Pair> list = new ArrayList<>();
    Lock lock = new ReentrantLock();

    public boolean contains(Pair pair) {
        lock.lock();
        try {
            return list.contains(pair);
        } finally {
            lock.unlock();
        }
    }

    public void add(Pair pair) {
        lock.lock();
        try {
            list.add(pair);
        } finally {
            lock.unlock();
        }
    }

    public List<Pair> getList() {
        return list;
    }
}



public class Parallel {
    private static final Map<Integer, ReentrantLock> access = new ConcurrentHashMap<>();
    private static final MyList resultList = new MyList();
    private static final MyListBlack blackList = new MyListBlack();

    public static void printAllThreads() {
        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : threads.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();
            System.out.println("Thread: " + thread.getName() + " (State: " + thread.getState() + ")");
            for (StackTraceElement element : stackTrace) {
                System.out.println("  at " + element);
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        MyQueue<Node> synchronizedQueue = new MyQueue<>();
        for (int i = 1; i < 449; ++i) {
            access.put(i, new ReentrantLock());
        }

        List<String> files = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            for (int j = 1; j <= 10; j++) {
                files.add("C:\\Facultate_informatica\\an3\\ppd\\tema5\\" + "C" + i + "_P" + j + ".txt");
            }
        }

        int P = 6;
        int p_r = 2;
        int p_w = P - p_r;

        int start = 0, end = 0, cat, rest;
        int N = files.size();
        cat = N / p_r;
        rest = N % p_r;

        ExecutorService producerPool = Executors.newFixedThreadPool(p_r);
        ExecutorService consumerPool = Executors.newFixedThreadPool(p_w);

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
                    Producer producer = new Producer(finalStart, finalEnd, synchronizedQueue, files);
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
                    Consumer consumer = new Consumer(synchronizedQueue, resultList);
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

        printAllThreads();
        resultList.sort();
        resultList.showList();

        Utils.writeResultParalell(resultList, "ResultParallel.txt");
        long end_t = System.nanoTime();

        assert (Utils.areFilesEqual("Result.txt", "ResultParallel.txt"));

        System.out.println((double) (end_t - start_t) / 1000000);
    }

    public static class Producer {
        int start;
        int end;
        MyQueue<Node> queue;
        List<String> files;

        public Producer(int start, int end, MyQueue<Node> queue, List<String> files) {
            this.start = start;
            this.end = end;
            this.queue = queue;
            this.files = files;
        }

        public void readFromFileAddToQueue(String filePath) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    int firstNumber = Integer.parseInt(parts[0].trim());
                    int secondNumber = Integer.parseInt(parts[1].trim());
                    Node node = new Node(new Participant(firstNumber, secondNumber, ""),null,null);
                    this.queue.enqueue(node);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
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

        public Consumer(MyQueue<Node> queue, MyList list) {
            this.queue = queue;
            this.list = list;
        }

        public void run() {
            try {
                while (!queue.isEmpty() || !queue.isProducersFinished()) {
                    Participant participant = null;
                    Node node=null;
                    try{
                        node = queue.dequeue();
                        if (node == null) {
                            // Dacă node este null (coada este goală și producătorii au terminat)
                            if(queue.isProducersFinished()) {
                                break;
                            }
                        }else{
                            participant=node.getData();
                        }

                    } catch (InterruptedException e) {

                    }
                    if (participant == null) {
                        queue.isProducersFinished();
                        continue;
                    }
                    access.get(participant.getId()).lock();

                    if (!blackList.contains(new Pair(participant.getId(), participant.getCountry()))) {
                        if (participant.getScore() == -1) {
                            resultList.delete(participant);
                            blackList.add(new Pair(participant.getId(), participant.getCountry()));
                        } else {
                            Node actual = resultList.update(participant);

                            if (actual == null) {
                                resultList.add(participant);
                            }
                        }
                    }

                    access.get(participant.getId()).unlock();

                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
