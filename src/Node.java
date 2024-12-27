import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Node {
    private int idP;
    private int score;
    private Node next;
    private final Lock nodeLock;

    public Node(int idP, int score){
        this.idP = idP;
        this.score = score;
        next = null;
        this.nodeLock = new ReentrantLock();
    }

    // Sentinel node constructor
    public Node() {
        this(-1, -1); // Sentinel nodes will have idP and score set to -1
    }

    public int getIdP() {
        return idP;
    }

    public void setIdP(int idP) {
        this.idP = idP;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Node getNext() {
        return next;
    }

    public void setNext(Node next) {
        this.next = next;
    }

    public Lock getNodeLock() {
        return nodeLock;
    }

    public void lock() {
        nodeLock.lock();
    }

    public void unlock() {
        nodeLock.unlock();
    }
}
