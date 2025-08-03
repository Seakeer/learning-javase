package me.seakeer.learning.javase.multithread;

import java.util.*;
import java.util.concurrent.*;

/**
 * SafeDataStructureExample;
 * 多线程安全的数据结构示例
 *
 * @author Seakeer;
 * @date 2024/9/9;
 */
public class SafeDataStructureExample {


    public static void main(String[] args) {

        StringBuffer stringBuffer = new StringBuffer();

        Vector<Integer> vector = new Vector<>();
        Stack<Integer> stack = new Stack<>();

        CopyOnWriteArrayList<Integer> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        CopyOnWriteArraySet<Integer> copyOnWriteArraySet = new CopyOnWriteArraySet<>();


        ConcurrentSkipListSet<Integer> concurrentSkipListSet = new ConcurrentSkipListSet<>();
        ConcurrentLinkedQueue<Integer> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedDeque<Integer> concurrentLinkedDeque = new ConcurrentLinkedDeque<>();

        ArrayBlockingQueue<Integer> arrayBlockingQueue = new ArrayBlockingQueue<>(10);
        PriorityBlockingQueue<Integer> priorityBlockingQueue = new PriorityBlockingQueue<>();
        LinkedBlockingQueue<Integer> linkedBlockingQueue = new LinkedBlockingQueue<>();
        LinkedBlockingDeque<Integer> linkedBlockingDeque = new LinkedBlockingDeque<>();

        SynchronousQueue<Integer> synchronousQueue = new SynchronousQueue<>();
        LinkedTransferQueue<Integer> linkedTransferQueue = new LinkedTransferQueue<>();
        DelayQueue<?> delayQueue = new DelayQueue<>();

        Hashtable<Integer, Integer> hashtable = new Hashtable<>();
        ConcurrentHashMap<Integer, Integer> concurrentHashMap = new ConcurrentHashMap<>();
        ConcurrentSkipListMap<Integer, Integer> concurrentSkipListMap = new ConcurrentSkipListMap<>();


        Collections.synchronizedCollection(new LinkedList<>());
        Collections.synchronizedList(new ArrayList<>());
        Collections.synchronizedSet(new HashSet<>());
        Collections.synchronizedNavigableSet(new TreeSet<>());
        Collections.synchronizedSortedSet(new TreeSet<>());
        Collections.synchronizedMap(new HashMap<>());
        Collections.synchronizedNavigableMap(new TreeMap<>());
        Collections.synchronizedSortedMap(new TreeMap<>());
    }
}
