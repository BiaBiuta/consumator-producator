import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyList {
    private Node head; // Sentinel start node
    private final Node tail; // Sentinel end node
    private final Map<Integer, Node> frauds;

    public MyList(){
        head = new Node();
        tail = new Node();
        head.setNext(tail); // Initially, head points directly to tail
        frauds = new HashMap<>();
    }

    public void addNode(int idP, int score) {
        Node newNode = new Node(idP, score);
        System.out.println(Thread.currentThread().getName() + " trying to lock head");
        Node foundN = find(newNode.getIdP());

        if(foundN != null ){
            newNode.setScore(newNode.getScore() + foundN.getScore());
            deleteNode(newNode.getIdP());
        }

        head.lock();
        System.out.println(Thread.currentThread().getName() + " locked head");

        Node prev = head;
        Node curr = head.getNext();

        try {
            curr.lock(); // Lock the first real node
            System.out.println(Thread.currentThread().getName() + " locked " + curr);

            if (newNode.getScore() == -1) {
                frauds.put(idP, newNode);
            }
            if (!isFraud(newNode.getIdP())) {
                while (curr != tail && curr.getScore() > newNode.getScore()) {
                    prev.unlock();
                    prev = curr;
                    curr = curr.getNext();
                    curr.lock();
                }

                if (curr != tail && newNode.getScore() == curr.getScore()) {
                    while (curr != tail && curr.getIdP() < newNode.getIdP() && curr.getScore() == newNode.getScore()) {
                        prev.unlock();
                        prev = curr;
                        curr = curr.getNext();
                        curr.lock();
                    }
                }

                newNode.setNext(curr);
                prev.setNext(newNode);
            }

        } finally {
            prev.unlock();
            curr.unlock();
            if(newNode.getScore()==-1){
                deleteNode(newNode.getIdP());
            }
        }
    }





    public void deleteNode(int idP) {
        head.lock();
        Node prev = head;
        Node curr = head.getNext();
        curr.lock();

        try {
            while (curr != tail && curr.getIdP() != idP) {
                prev.unlock();
                prev = curr;
                curr = curr.getNext();
                curr.lock();
            }

            if (curr != tail) {
                prev.setNext(curr.getNext());
            }
        } finally {

            curr.unlock();
            prev.unlock();
        }
    }

    public Node find(int idP) {
        head.lock();
        Node prev = head;
        Node curr = head.getNext();
        curr.lock();

        try {
            while (curr != tail && curr.getIdP() != idP) {
                prev.unlock();
                prev = curr;
                curr = curr.getNext();
                curr.lock();
            }

            return curr != tail ? curr : null;
        } finally {

            curr.unlock();
            prev.unlock();
        }
    }

    public boolean isFraud(int idP) {
        return frauds.containsKey(idP);
    }

    public List<Node> getElements(){
        List<Node> all = new ArrayList<>();
        Node p = this.head;
        while(p!=null){
            all.add(p);
            p = p.getNext();
        }
        return all;
    }
    public void removeHead() {
        if (head != null) {
            head = this.head.getNext(); // Capul devine următorul nod
        }
    }

    public void showList() {
        Node p= this.head.getNext();
        while(p!=this.tail){
            System.out.println("("+p.getIdP()+" , "+p.getScore()+")");
            p=p.getNext();
        }
    }
}
