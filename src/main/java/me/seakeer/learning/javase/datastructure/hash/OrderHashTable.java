package me.seakeer.learning.javase.datastructure.hash;

/**
 * OrderHashTable;
 * 哈希表实例：存储订单信息
 *
 * @author Seakeer;
 * @date 2024/11/20;
 */
public class OrderHashTable {

    /**
     * 订单类
     */
    public static class Order {

        /**
         * 订单ID，使用orderId作为关键字
         */
        private Long orderId;

        private String number;
        private Long amount;

        // 拉链法解决哈希冲突
        private Order next;

        public Order(Long orderId, String number, Long amount) {
            this.orderId = orderId;
            this.number = number;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return "Order{" +
                    "orderId=" + orderId +
                    ", number='" + number + '\'' +
                    ", amount=" + amount +
                    '}';
        }
    }

    // 哈希表内部用来存数据的数组
    private Order[] orders;

    // 负载因子
    private final float loadFactor;

    // 已存储的元素数量
    private int size;

    public OrderHashTable(int capacity, float loadFactor) {
        this.orders = new Order[capacity];
        this.loadFactor = loadFactor;
        this.size = 0;
    }

    public void add(Order order) {
        // 当存储的元素数量/容量大于负载因子，则进行扩容
        if (size > orders.length * loadFactor) {
            resize();
        }
        doAdd(order, orders);
        size++;
    }

    private void doAdd(Order order, Order[] orders) {
        int index = hash(order.orderId);
        Order idxOrder = orders[index];
        if (idxOrder == null) {
            // 不存在哈希冲突则直接放到索引位置
            orders[index] = order;
        } else {
            // 存在哈希冲突，则在索引处构建链表
            while (idxOrder.next != null) {
                idxOrder = idxOrder.next;
            }
            idxOrder.next = order;
        }
    }

    private void resize() {
        // 容量翻倍
        int newCapacity = orders.length * 2;
        Order[] newOrders = new Order[newCapacity];
        // 迁移数据到新的数组
        for (Order value : orders) {
            Order order = value;
            while (order != null) {
                doAdd(order, newOrders);
                order = order.next;
            }
        }
        orders = newOrders;
    }

    private int hash(Long orderId) {
        // 采用除留余数法构造哈希函数
        return (int) (orderId % orders.length);
    }

    public void remove(Long orderId) {
        int index = hash(orderId);
        Order idxOrder = orders[index];
        if (idxOrder == null) {
            return;
        }
        if (idxOrder.orderId.equals(orderId)) {
            orders[index] = idxOrder.next;
            size--;
            return;
        }
        while (idxOrder.next != null) {
            if (idxOrder.next.orderId.equals(orderId)) {
                idxOrder.next = idxOrder.next.next;
                size--;
                return;
            }
            idxOrder = idxOrder.next;
        }
    }

    public Order get(Long orderId) {
        int index = hash(orderId);
        Order idxOrder = orders[index];
        while (idxOrder != null) {
            if (idxOrder.orderId.equals(orderId)) {
                return idxOrder;
            }
            idxOrder = idxOrder.next;
        }
        return null;
    }

    public static void main(String[] args) {
        OrderHashTable hashTable = new OrderHashTable(100, 0.75f);
        hashTable.add(new Order(1L, "1", 1L));
        hashTable.add(new Order(101L, "101", 101L));
        hashTable.add(new Order(201L, "201", 201L));
        System.out.println(hashTable.get(201L));
        hashTable.remove(1L);
        hashTable.remove(201L);
    }

}
