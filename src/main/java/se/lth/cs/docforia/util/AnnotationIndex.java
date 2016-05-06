package se.lth.cs.docforia.util;
/*
 * Copyright 2016 Marcus Klang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * AVL tree based range index implementation, supporting overlapping and duplicated ranges
 */
public class AnnotationIndex<T> implements Iterable<T> {
    public class Entry implements Comparable<Entry> {
        private int start;
        private int end;
        private int max;
        private int balance;
        private T item;

        private Entry left;
        private Entry right;
        private Entry parent;

        public Entry(int start, int end, T item) {
            this.start = start;
            this.end = end;
            this.max = this.end;
            this.item = item;
        }

        public int center() {
            return start + (end-start)/2;
        }

        public T get() {
            return item;
        }

        public void set(T item) {
            this.item = item;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        protected boolean isLeaf() {
            return left == null && right == null;
        }

        protected boolean isBranch() {
            return (left == null) ^ (right == null);
        }

        @Override
        public int compareTo(Entry o) {
            int compare = Integer.compare(start, o.start);
            return compare == 0 ? Integer.compare(center(), o.center()) : compare;
        }

        @Override
        public String toString() {
            return "{ B=" + balance(this) + ", Start=" + start + ", End=" + end + ", Max=" + max + ", Item=" + item.toString() + "}";
        }
    }

    protected Entry root = null;
    protected int size = 0;

    protected int balance(Entry node) {
        if(node == null)
            return 0;

        return node.balance;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    protected int maxend(Entry node) {
        if(node == null)
            return 0;
        else
            return node.max;
    }

    protected Entry rotateLeft(Entry node) {
        Entry x = node;
        Entry y = node.right;


        x.right = y.left;
        y.left = x;

        if(x.right != null) {
            x.right.parent = x;
        }

        if(x.parent == null) {
            root = y;
        } else {
            if (x.parent.left == x)
                x.parent.left = y;
            else
                x.parent.right = y;
        }

        y.parent = x.parent;
        x.parent = y;

        int xbalance = x.balance;
        int ybalance = y.balance;

        x.balance = xbalance - (ybalance <= 0 ? ybalance : 0) + 1;
        y.balance = ybalance + (x.balance > 0 ? x.balance : 0) + 1;

        node.max = Math.max(node.end, Math.max(maxend(node.left), maxend(node.right)));
        y.max = Math.max(y.end, Math.max(maxend(y.left), maxend(y.right)));

        return y;
    }

    protected Entry rotateRight(Entry node) {
        Entry x = node;
        Entry y = node.left;

        x.left = y.right;
        y.right = x;

        if(x.left != null) {
            x.left.parent = x;
        }

        if(x.parent == null) {
            root = y;
        } else {
            if (x.parent.left == x)
                x.parent.left = y;
            else
                x.parent.right = y;
        }

        y.parent = x.parent;
        x.parent = y;

        int xbalance = x.balance;
        int ybalance = y.balance;

        x.balance = xbalance - (ybalance > 0 ? ybalance : 0) - 1;
        y.balance = ybalance + (x.balance <= 0 ? x.balance : 0) - 1;

        node.max = Math.max(node.end, Math.max(maxend(node.left), maxend(node.right)));
        y.max = Math.max(y.end, Math.max(maxend(y.left), maxend(y.right)));

        return y;
    }

    protected void insertRebalance(Entry child) {
        Entry parent = child.parent;
        while(parent != null) {
            if(child == parent.left) {
                if (parent.balance == 1) {
                    parent.balance = 2;
                    if (child.balance ==  -1) { // Left Right Case
                        rotateLeft(child); // Reduce to Left Left Case
                    }
                    // Left Left Case
                    rotateRight(parent);
                    break;
                }
                if (parent.balance == -1) {
                    parent.balance = 0;
                    break;
                }
                parent.balance = 1;
            } else {
                if (parent.balance == -1) {
                    parent.balance = -2;
                    if (child.balance == 1) { // Right Left Case
                        child = rotateRight(child); // Reduce to Right Right Case
                    }
                    // Right Right Case
                    rotateLeft(parent);
                    break;
                }
                if (parent.balance == 1) {
                    parent.balance = 0;
                    break;
                }
                parent.balance = -1;
            }

            child = parent;
            parent = child.parent;
        }
    }

    protected void removeRebalance(Entry parent, int dir) {
        while(parent != null) {
            if (dir == 1) {
                if (parent.balance == 1) {
                    parent.balance = 2;
                    Entry sibling = parent.left;
                    int b = sibling == null ? 0 : sibling.balance;
                    if (b == -1) { // Left Right Case
                        rotateLeft(sibling);
                    }
                    // Left Left Case
                    parent = rotateRight(parent);
                    if (b == 0)
                        break;
                }
                else if (parent.balance == 0) {
                    parent.balance = 1;
                    break;
                }
                else {
                    parent.balance = 0;
                }
            } else {
                if (parent.balance == -1) {
                    parent.balance = -2;
                    Entry sibling = parent.right;
                    int b = sibling == null ? 0 : sibling.balance;
                    if (b == 1) { // Right Left Case
                        rotateRight(sibling);// Reduce to Right Right Case
                    }
                    // Right Right Case
                    parent = rotateLeft(parent);
                    if (b == 0)
                        break;
                }
                else if (parent.balance == 0) {
                    parent.balance = -1;
                    break;
                }
                else {
                    parent.balance = 0;
                }
            }

            Entry child = parent;
            parent = child.parent;
            if(parent != null) {
                if(parent.left == child)
                    dir = -1;
                else
                    dir = 1;
            }
        }
    }

    protected void add(Entry key) {
        Entry node = root;
        if(node == null) {
            root = key;
            this.size += 1;
            return;
        }

        Entry prev = node;
        boolean left = true;
        while(node != null) {
            prev = node;
            node.max = Math.max(node.max, key.max);
            if(left = (key.compareTo(node) <= 0)) {
                //Left subtree
                node = node.left;
            } else {
                //right subtree
                node = node.right;
            }
        }

        if(left)
            prev.left = key;
        else
            prev.right = key;

        key.parent = prev;
        insertRebalance(key);
        this.size += 1;
    }

    /**
     * Add entry to index
     * @param start start position
     * @param end end position
     * @param item data
     * @return indexed entry reference
     */
    public Entry add(int start, int end, T item) {
        Entry entry = new Entry(start, end, item);
        add(entry);
        return entry;
    }

    /**
     * Move a range and make the needed changes in the index
     * @param entry entry to be updated
     * @param start new start position
     * @param end new end position
     */
    public void update(Entry entry, int start, int end) {
        remove(entry);
        add(entry);
    }

    private Entry firstOverlap(int start, int end) {
        Entry currentNode = root;
        Entry prevNode = root;
        while(currentNode != null) {
            prevNode = currentNode;
            if(currentNode.start >= start || currentNode.max > start) {
                currentNode = currentNode.left;
            } else {
                currentNode = currentNode.right;
            }
        }

        return prevNode;
    }

    /**
     * Find the covered ranges by given range
     * @param start start position (open)
     * @param end end position (closed)
     */
    public Iterator<T> cover(int start, int end) {
        return new Iterator<T>() {
            final Iterator<Entry> iter = coverEntries(start, end);
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                return iter.next().item;
            }
        };
    }

    private Entry backtrack(Entry startNode, int start) {
        Entry closest = startNode;
        Entry currentNode = startNode;
        while(currentNode != null && currentNode.start < start) {
            if(currentNode.start >= closest.start)
                closest = currentNode;

            currentNode = currentNode.parent;
        }

        return closest;
    }

    private Entry find(Entry startNode, int start) {
        Entry currentNode = startNode;
        Entry last = currentNode;
        while(currentNode != null) {
            last = currentNode;
            if(currentNode.start >= start) {
                currentNode = currentNode.left;
            } else {
                currentNode = currentNode.right;
            }
        }

        return last;
    }

    /**
     * Next in-order node
     * @param current current node (not null!)
     * @return next entry or null if current was last in order.
     */
    public Entry next(Entry current) {
        if(current == null)
            throw new NullPointerException("current");

        Entry nextNode = current;
        Entry currentNode = nextNode;
        if (currentNode.right != null) {
            currentNode = currentNode.right;
            while (currentNode != null) {
                nextNode = currentNode;
                currentNode = currentNode.left;
            }
            current = nextNode;
            return current;
        } else if (currentNode.parent != null) {
            if (currentNode.parent.left == currentNode) {
                nextNode = currentNode.parent;
                current = nextNode;
                return current;
            } else {
                Entry lastNode = currentNode;
                while (currentNode != null && currentNode.left != lastNode) {
                    lastNode = currentNode;
                    currentNode = currentNode.parent;
                }

                current = currentNode;
                return current;
            }
        } else {
            return null;
        }
    }

    /**
     * Previous in-order node
     * @param current current node (not null!)
     * @return previous entry or null if current was first in order.
     */
    public Entry prev(Entry current) {
        if(current == null)
            throw new NullPointerException("current");

        Entry nextNode = current;
        Entry currentNode = nextNode;
        if (currentNode.left != null) {
            currentNode = currentNode.left;
            while (currentNode != null) {
                nextNode = currentNode;
                currentNode = currentNode.right;
            }
            current = nextNode;
            return current;
        } else if (currentNode.parent != null) {
            if (currentNode.parent.right == currentNode) {
                nextNode = currentNode.parent;
                current = nextNode;
                return current;
            } else {
                Entry lastNode = currentNode;
                while (currentNode != null && currentNode.right != lastNode) {
                    lastNode = currentNode;
                    currentNode = currentNode.parent;
                }

                current = currentNode;
                return current;
            }
        } else {
            return current;
        }
    }

    /** Internal tree navigator */
    private class Navigator implements AnnotationNavigator<T> {
        public Navigator() {
            this.current = null;
        }

        public Navigator(Entry current) {
            this.current = current;
        }

        Entry min;
        Entry max;
        Entry current;
        boolean lastNode = false;

        private Entry getMin() {
            if(min == null) {
                Entry current = root;
                Entry prev = root;
                while(current != null) {
                    prev = current;
                    current = current.left;
                }

                this.min = prev;
            }

            return this.min;
        }

        private Entry getMax() {
            if(max == null) {
                Entry current = root;
                Entry prev = root;
                while(current != null) {
                    prev = current;
                    current = current.right;
                }
                this.max = prev;
            }

            return this.max;
        }

        @Override
        public T current() {
            return this.current != null ? this.current.item : null;
        }

        @Override
        public boolean next() {
            if(current == null && !lastNode) {
                current = getMin();
                return true;
            } else if(lastNode)
                return false;
            else
            {
                this.current = AnnotationIndex.this.next(current);
                if(current == null)
                    lastNode = true;

                return current != null;
            }
        }

        @Override
        public boolean nextFloor(int start) {
            if(current == null && !lastNode) {
                Entry closest = find(root, start);

                this.current = closest;
                if(closest != null && closest.start > start) {
                    if(!prev()) {
                        this.current = closest;
                        return true;
                    }
                    else
                        return true;
                } else {
                    return current != null;
                }
            }
            else if(current == null)
                return false;
            else if(current.start >= start) {
                return next();
            } else {
                //Find largest parent, that is < start
                Entry startNode = current;
                Entry closest = backtrack(current, start);
                if (closest == current) {
                    if (closest.right == null)
                        return next();
                    else
                        closest = closest.right;
                }

                Entry nextClosest = find(closest, start);
                this.current = nextClosest;
                return nextClosest.start <= start || (prev() && (current != startNode || next()));
            }
        }

        @Override
        public boolean hasReachedEnd() {
            return lastNode;
        }

        @Override
        public boolean prev() {
            if(current == null && lastNode) {
                current = getMax();
                lastNode = false;
                return true;
            } else if(current == null) {
                return false;
            } else {
                this.current = AnnotationIndex.this.prev(current);
                if(current == null)
                    lastNode = false;

                return current != null;
            }
        }

        @Override
        public void reset() {
            current = null;
            lastNode = false;
        }

        @Override
        public boolean next(int start) {
            if(current == null && !lastNode) {
                Entry closest = find(root, start);

                this.current = closest;
                if(closest != null && closest.start < start) {
                    return next();
                } else {
                    return current != null;
                }
            }
            else if(current == null)
                return false;
            else if(current.start >= start) {
                return next();
            } else {
                //Find largest parent, that is < start
                Entry startNode = current;
                Entry closest = backtrack(current, start);
                if (closest == current) {
                    if (closest.right == null)
                        return next();
                    else
                        closest = closest.right;
                }

                this.current = find(closest, start);
                return this.current.start >= start || next();
            }
        }

        @Override
        public int start() {
            return current.start;
        }

        @Override
        public int end() {
            return current.end;
        }
    }

    private AnnotationNavigator<T> emptyNavigator = new AnnotationNavigator<T>() {
        @Override
        public T current() {
            return null;
        }

        @Override
        public boolean next() {
            return false;
        }

        @Override
        public boolean nextFloor(int start) {
            return false;
        }

        @Override
        public boolean hasReachedEnd() {
            return true;
        }

        @Override
        public boolean prev() {
            return false;
        }

        @Override
        public void reset() {

        }

        @Override
        public boolean next(int start) {
            return false;
        }

        @Override
        public int start() {
            throw new NoSuchElementException();
        }

        @Override
        public int end() {
            throw new NoSuchElementException();
        }
    };

    /**
     * A navigator from minimum position or a empty navigator if the size of this index == 0
     */
    public AnnotationNavigator<T> navigator() {
        if(root == null)
            return emptyNavigator;
        else
            return new Navigator();
    }

    /**
     * Get a navigator from a specific entry
     * @param current the current entry in the navigator
     */
    public AnnotationNavigator<T> navigator(Entry current) {
         return new Navigator(current);
    }

    /**
     * Find the covered ranges by given range
     * @param start start position (open)
     * @param end end position (closed)
     */
    public Iterator<Entry> coverEntries(int start, int end) {
        final ArrayDeque<Entry> stack = new ArrayDeque<>();
        if(root == null)
            return Collections.emptyIterator();

        Entry currentNode = root;
        Entry prevNode = root;
        while(currentNode != null) {
            prevNode = currentNode;
            if(currentNode.start >= start) {
                currentNode = currentNode.left;
            } else {
                currentNode = currentNode.right;
            }
        }

        Entry startNode = prevNode;

        return new Iterator<Entry>() {
            protected Entry nextNode = startNode;
            protected Entry entry;

            protected boolean moveForward() {
                if (entry != null)
                    return true;
                else if (nextNode == null)
                    return false;
                else {
                    while (nextNode != null) {
                        Entry node = nextNode;
                        Entry currentNode = nextNode;

                        if (currentNode.right != null) {
                            currentNode = currentNode.right;
                            while (currentNode != null) {
                                nextNode = currentNode;
                                currentNode = currentNode.left;
                            }
                        } else if (currentNode.parent != null) {
                            if (currentNode.parent.left == currentNode) {
                                nextNode = currentNode.parent;
                            } else {
                                Entry lastNode = currentNode;
                                while (currentNode != null && currentNode.left != lastNode) {
                                    lastNode = currentNode;
                                    currentNode = currentNode.parent;
                                }

                                nextNode = currentNode;
                            }
                        } else {
                            nextNode = null;
                        }

                        if (node.start >= start && node.end <= end) {
                            entry = node;
                            return true;
                        } else if (node.start >= end) {
                            entry = null;
                            nextNode = null;
                            return false;
                        }
                    }

                    entry = null;
                    return false;
                }
            }

            @Override
            public boolean hasNext() {
                return moveForward();
            }

            @Override
            public Entry next() {
                if(!moveForward())
                    throw new NoSuchElementException();

                Entry retval = entry;
                entry = null;
                return retval;
            }
        };
    }

    /**
     * Find all overlaps for the given interval
     * @param start start (closed)
     * @param end end (open)
     */
    public Iterator<T> overlap(final int start, final int end) {
        return new Iterator<T>() {
            final Iterator<Entry> iter = overlapEntries(start, end);
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                return iter.next().item;
            }
        };
    }

    /**
     * Find all overlaps for the interval
     * @param start start (closed)
     * @param end end (open)
     */
    public Iterator<Entry> overlapEntries(final int start, final int end) {
        if(root == null)
            return Collections.emptyIterator();

        Entry startNode = firstOverlap(start, end);

        return new Iterator<Entry>() {
            protected Entry nextNode = startNode;
            protected Entry entry;

            protected boolean moveForward() {
                if (entry != null)
                    return true;
                else if (nextNode == null)
                    return false;
                else {
                    while (nextNode != null) {
                        Entry node = nextNode;
                        Entry currentNode = nextNode;

                        if (currentNode.right != null) {
                            currentNode = currentNode.right;
                            while (currentNode != null) {
                                nextNode = currentNode;
                                currentNode = currentNode.left;
                            }
                        } else if (currentNode.parent != null) {
                            if (currentNode.parent.left == currentNode) {
                                nextNode = currentNode.parent;
                            } else {
                                Entry lastNode = currentNode;
                                while (currentNode != null && currentNode.left != lastNode) {
                                    lastNode = currentNode;
                                    currentNode = currentNode.parent;
                                }

                                nextNode = currentNode;
                            }
                        } else {
                            nextNode = null;
                        }

                        if (node.end > start && node.start < end) {
                            entry = node;
                            return true;
                        } else if (node.start >= end) {
                            entry = null;
                            nextNode = null;
                            return false;
                        }
                    }

                    entry = null;
                    return false;
                }
            }

            @Override
            public boolean hasNext() {
                return moveForward();
            }

            @Override
            public Entry next() {
                if(!moveForward())
                    throw new NoSuchElementException();

                Entry retval = entry;
                entry = null;
                return retval;
            }
        };
    }

    /**
     * Remove entry by providing the key, item is needed as duplicated intervals are supported
     * @param start start range
     * @param end end range
     * @param item the value
     */
    public Entry remove(int start, int end, T item) {
        Entry tempNode = new Entry(start, end, item);

        //find entry
        Entry entry = root;
        while(entry != null) {
            int comparision = entry.compareTo(tempNode);
            if(comparision == 0 && entry.item.equals(tempNode.item)) {
                remove(entry);
                return entry;
            } else if(comparision <= 0){
                entry = entry.left;
            } else {
                entry = entry.right;
            }
        }

        return null;
    }

    protected Entry successor(Entry entry) {
        Entry node = entry.right;
        Entry prev = node;
        while(node != null) {
            prev = node;
            node = node.left;
        }

        return prev;
    }

    protected Entry predecessor(Entry entry) {
        Entry node = entry.left;
        Entry prev = node;
        while(node != null) {
            prev = node;
            node = node.right;
        }

        return prev;
    }

    private void removeLeaf(Entry entry) {
        if(entry.parent == null) {
            root = null;
        } else {
            int dir = 0;
            if(entry.parent.left == entry) {
                entry.parent.left = null;
                dir = -1;
            } else {
                entry.parent.right = null;
                dir = 1;
            }
            entry.parent.max = Math.max(entry.parent.end, maxend(entry.parent.right));

            removeRebalance(entry.parent, dir);
            entry.parent = null;
        }
        size -= 1;
    }

    private void removeBranch(Entry entry) {
        if(entry.parent != null) {
            Entry y;
            if(entry.right == null)
                y = entry.left;
            else
                y = entry.right;

            int dir = 0;
            if(entry.parent.left == entry) {
                y.parent = entry.parent;
                entry.parent.left = y;
                dir = -1;
            } else {
                y.parent = entry.parent;
                entry.parent.right = y;
                dir = 1;
            }

            entry.parent.max = Math.max(entry.parent.end, Math.max(maxend(entry.parent.left), maxend(entry.parent.right)));

            removeRebalance(y.parent,dir);
            entry.parent = null;
            entry.left = null;
            entry.right = null;
        } else {
            root = entry.left != null ? entry.left : entry.right;
            root.parent = null;
            entry.left = null;
            entry.right = null;
        }
        size -= 1;
    }

    /**
     * Remove entry from index
     */
    public void remove(Entry entry) {
        if(entry.parent == null && root != entry)
            throw new IllegalStateException("Node is already removed!");

        if(entry.isLeaf()) {
            removeLeaf(entry);
        }
        else if(entry.isBranch()) {
            removeBranch(entry);
        } else {
            Entry succ = successor(entry);
            Entry removeMe = swapNode(entry, succ);
            if(removeMe.isLeaf())
                removeLeaf(removeMe);
            else
                removeBranch(removeMe);
            //removeRebalance(succ);
        }
    }

    /**
     * <b>DEBUG UTILITY</b> Computes the actual height of tree.
     */
    private int height(Entry entry, Reference2IntOpenHashMap<Entry> map) {
        if(entry.isLeaf()) {
            map.put(entry, 1);
            return 1;
        }
        else if (entry.isBranch()) {
            int retval;
            if(entry.left != null) {
                map.put(entry, retval=height(entry.left, map)+1);
            } else {
                map.put(entry, retval=height(entry.right, map)+1);
            }

            return retval;
        } else {
            int height = Math.max(height(entry.left, map)+1,
                                  height(entry.right, map)+1);
            map.put(entry, height);
            return height;
        }
    }

    /**
     * <b>DEBUG UTILITY</b> Verifies that the tree is balanced.
     */
    boolean verifyBalance() {
        if(root == null)
            return true;

        Reference2IntOpenHashMap<Entry> heightMap = new Reference2IntOpenHashMap<>();
        height(root, heightMap);

        for (Entry entry : entries()) {
            int balance = heightMap.getInt(entry.left)-heightMap.getInt(entry.right);
            if(entry.balance != balance)
                throw new AssertionError("Balance incorrect at " + entry.toString() + ", expected = " + balance);

            if(Math.abs(balance) > 1)
                throw new AssertionError("Balance at: "
                                                 + entry.toString()
                                                 + " = "
                                                 + (heightMap.getInt(entry.left)-heightMap.getInt(entry.right)));
        }

        return true;
    }

    private Entry swapNode(Entry node, Entry succ) {
        Entry parent = node.parent;
        Entry left = node.left;
        Entry right = node.right;

        Entry succParent = succ.parent;
        Entry succRight = succ.right; //succ.left == null!
        int succBalance = succ.balance;
        succ.balance = node.balance;
        node.balance = succBalance;

        if(succParent == node) {
            succ.left = left;
            succ.left.parent = succ;
            succ.right = node;

            node.parent = succ;
            node.left = null;
            node.right = succRight;
            if(succRight != null)
                succRight.parent = node;

            succ.max = Math.max(succ.end, Math.max(maxend(succ.left), maxend(succ.right)));
        } else {
            if(succParent.left == succ) {
                succParent.left = node;
            } else {
                succParent.right = node;
            }

            succ.left = left;
            succ.left.parent = succ;
            succ.right = right;
            succ.right.parent = succ;

            node.parent = succParent;
            node.left = null;
            node.right = succRight;
            if(succRight != null)
                succRight.parent = node;

            succParent.max = Math.max(succParent.end, Math.max(maxend(succParent.left), maxend(succParent.right)));
            succ.max = Math.max(succ.end, Math.max(maxend(succ.left), maxend(succ.right)));
        }

        if(parent == null) { //node is a root node!
            root = succ;
            succ.parent = null;
        } else {
            succ.parent = parent;
            if(parent.left == node) {
                parent.left = succ;
            } else {
                parent.right = succ;
            }
        }

        return node;
    }

    /**
     * Clean the tree, basically set root to null, will not mark node parents as null for performance reasons.
     */
    public void clear() {
        root = null;
    }

    public int size() {
        return size;
    }

    public Iterable<Entry> entries() {
        return new Iterable<Entry>() {
            @Override
            public Iterator<Entry> iterator() {
                final ArrayDeque<Entry> stack = new ArrayDeque<>();
                if(root == null)
                    return Collections.emptyIterator();

                Entry start = root;
                while(start != null) {
                    stack.push(start);
                    start = start.left;
                }

                return new Iterator<Entry>() {
                    @Override
                    public boolean hasNext() {
                        return !stack.isEmpty();
                    }

                    @Override
                    public Entry next() {
                        if(stack.isEmpty())
                            throw new NoSuchElementException();

                        Entry node = stack.pop();
                        if(node.right != null) {
                            Entry current = node.right;
                            while(current != null) {
                                stack.push(current);
                                current = current.left;
                            }
                        }

                        return node;
                    }
                };
            }
        };
    }

    /**
     * Forward iterator of all entries in this index
     */
    public Iterator<T> iterator() {
        final ArrayDeque<Entry> stack = new ArrayDeque<>();
        if(root == null)
            return Collections.emptyIterator();

        Entry start = root;
        while(start != null) {
            stack.push(start);
            start = start.left;
        }

        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return !stack.isEmpty();
            }

            @Override
            public T next() {
                if(stack.isEmpty())
                    throw new NoSuchElementException();

                Entry node = stack.pop();
                if(node.right != null) {
                    Entry current = node.right;
                    while(current != null) {
                        stack.push(current);
                        current = current.left;
                    }
                }

                return node.item;
            }
        };
    }
}
