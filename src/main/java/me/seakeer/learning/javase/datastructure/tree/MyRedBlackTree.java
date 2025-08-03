package me.seakeer.learning.javase.datastructure.tree;

import java.util.*;

/**
 * MyRedBlackTree;
 *
 * @author Seakeer;
 * @date 2024/12/4;
 */
public class MyRedBlackTree<E> {

    private Node<E> root;

    private final Comparator<? super E> comparator;

    private int size = 0;

    private static final boolean RED = false;
    private static final boolean BLACK = true;

    static final class Node<E> {
        E data;
        Node<E> left;
        Node<E> right;
        Node<E> parent;
        boolean color = BLACK;


        Node(E data, Node<E> parent) {
            this.data = data;
            this.parent = parent;
        }
    }


    public MyRedBlackTree() {
        this.comparator = null;
    }

    public MyRedBlackTree(Comparator<? super E> cmp) {
        this.comparator = cmp;
    }


    public boolean insert(E data) {
        Node<E> t = root;
        if (t == null) {
            root = new Node<>(data, null);
            size = 1;
            return true;
        }
        int cmp;
        Node<E> parent;
        // split comparator and comparable paths
        Comparator<? super E> cpr = comparator;
        if (cpr != null) {
            do {
                parent = t;
                cmp = cpr.compare(data, t.data);
                if (cmp < 0)
                    t = t.left;
                else
                    t = t.right;
            } while (t != null);
        } else {
            if (data == null)
                throw new NullPointerException();
            @SuppressWarnings("unchecked")
            Comparable<? super E> k = (Comparable<? super E>) data;
            do {
                parent = t;
                cmp = k.compareTo(t.data);
                if (cmp < 0)
                    t = t.left;
                else
                    t = t.right;
            } while (t != null);
        }
        Node<E> e = new Node<>(data, parent);
        if (cmp < 0)
            parent.left = e;
        else
            parent.right = e;
        fixAfterInsertion(e);
        size++;
        return true;
    }

    public boolean delete(E data) {
        Node<E> node = find(data);
        if (null == node) {
            return false;
        }
        deleteNode(node);
        return true;
    }

    public int size() {
        return size;
    }

    public boolean contains(E data) {
        return find(data) != null;
    }

    public boolean isEmpty() {
        return size <= 0;
    }

    public List<E> bfs() {
        List<E> result = new ArrayList<>(this.size);
        if (root == null) {
            return result;
        }
        Queue<Node<E>> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Node<E> current = queue.poll();
            result.add(current.data);
            if (current.left != null) {
                queue.add(current.left);
            }
            if (current.right != null) {
                queue.add(current.right);
            }
        }
        return result;
    }

    public void bfsOut() {
        if (root == null) {
            System.out.println("RBT: EMPTY");
            return;
        }

        Queue<Node<E>> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            Node<E> current = queue.poll();
            System.out.println("RBT.NODE: " + current.data + " " + (current.color == RED ? "RED" : "BLACK"));

            if (current.left != null) {
                queue.add(current.left);
            }
            if (current.right != null) {
                queue.add(current.right);
            }
        }
    }


    private Node<E> find(E data) {
        if (comparator != null) {
            return findNodeUsingComparator(data);
        }
        if (data == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Comparable<? super E> k = (Comparable<? super E>) data;
        Node<E> p = root;
        while (p != null) {
            int cmp = k.compareTo(p.data);
            if (cmp < 0)
                p = p.left;
            else if (cmp > 0)
                p = p.right;
            else
                return p;
        }
        return null;
    }

    final Node<E> findNodeUsingComparator(E data) {
        Comparator<? super E> cpr = comparator;
        if (cpr != null) {
            Node<E> p = root;
            while (p != null) {
                int cmp = cpr.compare(data, p.data);
                if (cmp < 0)
                    p = p.left;
                else if (cmp > 0)
                    p = p.right;
                else
                    return p;
            }
        }
        return null;
    }

    /**
     * Returns the first Entry in the TreeMap (according to the TreeMap's
     * data-sort function).  Returns null if the TreeMap is empty.
     */
    final Node<E> getMin() {
        Node<E> p = root;
        if (p != null)
            while (p.left != null)
                p = p.left;
        return p;
    }

    /**
     * Returns the last Entry in the TreeMap (according to the TreeMap's
     * data-sort function).  Returns null if the TreeMap is empty.
     */
    final Node<E> getMax() {
        Node<E> p = root;
        if (p != null)
            while (p.right != null)
                p = p.right;
        return p;
    }

    /**
     * Returns the successor of the specified Entry, or null if no such.
     */
    static <E> Node<E> successor(Node<E> t) {
        if (t == null)
            return null;
        else if (t.right != null) {
            Node<E> p = t.right;
            while (p.left != null)
                p = p.left;
            return p;
        } else {
            Node<E> p = t.parent;
            Node<E> ch = t;
            while (p != null && ch == p.right) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * Returns the predecessor of the specified Entry, or null if no such.
     */
    static <E> Node<E> predecessor(Node<E> t) {
        if (t == null)
            return null;
        else if (t.left != null) {
            Node<E> p = t.left;
            while (p.right != null)
                p = p.right;
            return p;
        } else {
            Node<E> p = t.parent;
            Node<E> ch = t;
            while (p != null && ch == p.left) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * Balancing operations.
     * <p>
     * Implementations of rebalancings during insertion and deletion are
     * slightly different than the CLR version.  Rather than using dummy
     * nilnodes, we use a set of accessors that deal properly with null.  They
     * are used to avoid messiness surrounding nullness checks in the main
     * algorithms.
     */

    private static <E> boolean colorOf(Node<E> p) {
        return (p == null ? BLACK : p.color);
    }

    private static <E> Node<E> parentOf(Node<E> p) {
        return (p == null ? null : p.parent);
    }

    private static <E> void setColor(Node<E> p, boolean c) {
        if (p != null)
            p.color = c;
    }

    private static <E> Node<E> leftOf(Node<E> p) {
        return (p == null) ? null : p.left;
    }

    private static <E> Node<E> rightOf(Node<E> p) {
        return (p == null) ? null : p.right;
    }

    /**
     * From CLR
     */
    private void rotateLeft(Node<E> p) {
        if (p != null) {
            Node<E> r = p.right;
            p.right = r.left;
            if (r.left != null)
                r.left.parent = p;
            r.parent = p.parent;
            if (p.parent == null)
                root = r;
            else if (p.parent.left == p)
                p.parent.left = r;
            else
                p.parent.right = r;
            r.left = p;
            p.parent = r;
        }
    }

    /**
     * From CLR
     */
    private void rotateRight(Node<E> p) {
        if (p != null) {
            Node<E> l = p.left;
            p.left = l.right;
            if (l.right != null) l.right.parent = p;
            l.parent = p.parent;
            if (p.parent == null)
                root = l;
            else if (p.parent.right == p)
                p.parent.right = l;
            else p.parent.left = l;
            l.right = p;
            p.parent = l;
        }
    }

    /**
     * From CLR
     */
    private void fixAfterInsertion(Node<E> x) {
        x.color = RED;
        while (x != null && x != root && x.parent.color == RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Node<E> y = rightOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateRight(parentOf(parentOf(x)));
                }
            } else {
                Node<E> y = leftOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = BLACK;
    }

    /**
     * Delete node p, and then rebalance the tree.
     */
    private void deleteNode(Node<E> p) {
        size--;

        // If strictly internal, copy successor's element to p and then make p
        // point to successor.
        if (p.left != null && p.right != null) {
            Node<E> s = successor(p);
            p.data = s.data;
            p = s;
        } // p has 2 children

        // Start fixup at replacement node, if it exists.
        Node<E> replacement = (p.left != null ? p.left : p.right);

        if (replacement != null) {
            // Link replacement to parent
            replacement.parent = p.parent;
            if (p.parent == null)
                root = replacement;
            else if (p == p.parent.left)
                p.parent.left = replacement;
            else
                p.parent.right = replacement;

            // Null out links so they are OK to use by fixAfterDeletion.
            p.left = p.right = p.parent = null;

            // Fix replacement
            if (p.color == BLACK)
                fixAfterDeletion(replacement);
        } else if (p.parent == null) { // return if we are the only node.
            root = null;
        } else { //  No children. Use self as phantom replacement and unlink.
            if (p.color == BLACK)
                fixAfterDeletion(p);

            if (p.parent != null) {
                if (p == p.parent.left)
                    p.parent.left = null;
                else if (p == p.parent.right)
                    p.parent.right = null;
                p.parent = null;
            }
        }
    }

    /**
     * From CLR
     */
    private void fixAfterDeletion(Node<E> x) {
        while (x != root && colorOf(x) == BLACK) {
            if (x == leftOf(parentOf(x))) {
                Node<E> sib = rightOf(parentOf(x));

                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateLeft(parentOf(x));
                    sib = rightOf(parentOf(x));
                }

                if (colorOf(leftOf(sib)) == BLACK &&
                        colorOf(rightOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(rightOf(sib)) == BLACK) {
                        setColor(leftOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateRight(sib);
                        sib = rightOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(rightOf(sib), BLACK);
                    rotateLeft(parentOf(x));
                    x = root;
                }
            } else { // symmetric
                Node<E> sib = leftOf(parentOf(x));

                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateRight(parentOf(x));
                    sib = leftOf(parentOf(x));
                }

                if (colorOf(rightOf(sib)) == BLACK &&
                        colorOf(leftOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(leftOf(sib)) == BLACK) {
                        setColor(rightOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateLeft(sib);
                        sib = leftOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(leftOf(sib), BLACK);
                    rotateRight(parentOf(x));
                    x = root;
                }
            }
        }

        setColor(x, BLACK);
    }

    final int compare(Object k1, Object k2) {
        return comparator == null ? ((Comparable<? super E>) k1).compareTo((E) k2)
                : comparator.compare((E) k1, (E) k2);
    }

    public static void main(String[] args) {
        MyRedBlackTree<Integer> tree = new MyRedBlackTree<>(Integer::compareTo);
        for (int i = 1; i < 10; i++) {
            tree.insert(i * 10);
        }
//        tree.insert(10);
//        tree.insert(120);
//        tree.insert(140);
//        tree.delete(70);
        tree.bfsOut();
    }
}
