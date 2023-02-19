import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

class LRUCache {
    /**
     LRU: 最近未使用， 头部是最新节点，
     1.添加元素，修改元素，修改完都移动到头部。
     2. 删除完都移动到尾部

     通过双端队列维护关系。
     通过hashmap迅速定位节点
     */
    int capacity;
    Deque<Node> queue;
    Map<Integer, Node> map;
    public LRUCache(int capacity) {
        this.queue = new LinkedList<>();
        this.map = new HashMap<>(capacity);
        this.capacity = capacity;
    }

    public int get(int key) {
        if (map.containsKey(key)) {
            Node node = map.get(key);
            // 调整节点位置
            queue.remove(node);
            queue.addFirst(node);
            return node.val;

        } else {
            return -1;
        }
    }

    public void put(int key, int value) {
        if (map.containsKey(key)) {
            Node node = map.get(key);
            // 调整节点位置
            queue.remove(node);
            queue.addFirst(node);
            return;
        }
        if (queue.size() == capacity) {
            queue.removeLast();
        }
        Node node = new Node(key, value);
        map.put(key, node);
        queue.addFirst(node);
    }
    class Node {
        int key;
        int val;
        Node(int key, int val) {
            this.key = key;
            this.val  =val;
        }
    }

    public static void main(String[] args) {
        LRUCache cache = new LRUCache(2);
        // [[2], [1, 1], [2, 2], [1], [3, 3], [2], [4, 4], [1], [3], [4]]
        cache.put(1,1);
        cache.put(2,2);
        int val = cache.get(1);
        cache.put(3,3);
        val = cache.get(2);
        cache.put(4,4);

    }
}



/**
 * Your LRUCache object will be instantiated and called as such:
 * LRUCache obj = new LRUCache(capacity);
 * int param_1 = obj.get(key);
 * obj.put(key,value);
 */