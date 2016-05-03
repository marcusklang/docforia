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
public class AnnotationIndex<T> implements Iterable<AnnotationIndex<T>.Entry> {
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

    protected int balance(Entry node) {
        if(node == null)
            return 0;

        return node.balance;
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

        y.balance += 1;
        x.balance += 1;

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

        x.balance -= 1;
        y.balance -= 1;

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

        node.max = Math.max(node.end, Math.max(maxend(node.left), maxend(node.right)));
        y.max = Math.max(y.end, Math.max(maxend(y.left), maxend(y.right)));

        return y;
    }

    protected void insertRebalance(Entry child) {
        Entry parent = child.parent;
        while(parent != null) {
            if(child == parent.left) {
                if (parent.balance == 1) {
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

    protected void removeRebalance(Entry child) {
        Entry parent = child != null ? child.parent : null;
        while(parent != null) {
            if (parent.right == child) {
                if (parent.balance == 1) {
                    Entry sibling = parent.left;
                    int b = sibling == null ? 0 : sibling.balance;
                    if (b == -1) { // Left Right Case
                        rotateLeft(sibling);
                    }
                    // Left Left Case
                    rotateRight(parent);
                    if (b == 0)
                        break;
                }
                if (parent.balance == 0) {
                    parent.balance = 1;
                    break;
                }
                parent.balance = 0;
            } else {
                if (parent.balance == -1) {
                    Entry sibling = parent.right;
                    int b = sibling == null ? 0 : sibling.balance;
                    if (b == 1) { // Right Left Case
                        rotateRight(sibling);// Reduce to Right Right Case
                    }
                    // Right Right Case
                    rotateLeft(parent);
                    if (b == 0)
                        break;
                }
                if (parent.balance == 0) {
                    parent.balance = -1;
                    break;
                }
                parent.balance = 0;
            }
            child = parent;
            parent = child.parent;
        }
    }

    protected void add(Entry key) {
        Entry node = root;
        if(node == null) {
            root = key;
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

    /**
     * Find the covered ranges by given range
     * @param start start position (open)
     * @param end end position (closed)
     */
    public Iterator<Entry> cover(int start, int end) {
        final ArrayDeque<Entry> stack = new ArrayDeque<>();
        if(root == null)
            return Collections.emptyIterator();

        Entry startNode = root;
        while(startNode != null) {
            stack.push(startNode);
            if(startNode.start >= start) {
                startNode = startNode.left;
            } else {
                startNode = startNode.right;
            }
        }

        return new Iterator<Entry>() {
            protected Entry entry;

            protected boolean moveForward() {
                if(entry != null)
                    return true;
                else {
                    while (!stack.isEmpty()) {
                        Entry node = stack.pop();
                        if (node.right != null) {
                            Entry current = node.right;
                            while (current != null) {
                                stack.push(current);
                                current = current.left;
                            }
                        }

                        if (node.start >= start && node.end <= end) {
                            entry = node;
                            return true;
                        } else if (node.start >= end) {
                            entry = null;
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
     * Find all overlaps for the given area
     * @param start start (closed)
     * @param end end range (open)
     */
    public Iterator<Entry> overlap(int start, int end) {
        final ArrayDeque<Entry> stack = new ArrayDeque<>();
        if(root == null)
            return Collections.emptyIterator();

        Entry startNode = root;
        while(startNode != null) {
            stack.push(startNode);
            if(startNode.start >= start || startNode.max > start) {
                startNode = startNode.left;
            } else {
                startNode = startNode.right;
            }
        }

        return new Iterator<Entry>() {
            protected Entry entry;

            protected boolean moveForward() {
                if(entry != null)
                    return true;
                else {
                    while (!stack.isEmpty()) {
                        Entry node = stack.pop();
                        if (node.right != null) {
                            Entry current = node.right;
                            while (current != null) {
                                stack.push(current);
                                current = current.left;
                            }
                        }

                        if (node.end > start && node.start < end) {
                            entry = node;
                            return true;
                        } else if (node.start >= end) {
                            entry = null;
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

    /**
     * Remove entry from index
     */
    public void remove(Entry entry) {
        if(entry.parent == null && root != entry)
            throw new IllegalStateException("Node is already removed!");

        if(entry.isLeaf()) {
            if(entry.parent == null) {
                root = null;
            } else {
                if(entry.parent.left == entry) {
                    entry.parent.left = null;
                    entry.parent.max = Math.max(entry.parent.end, maxend(entry.parent.right));
                } else {
                    entry.parent.right = null;
                    entry.parent.max = Math.max(entry.parent.end, maxend(entry.parent.left));
                }

                removeRebalance(entry.parent);
                entry.parent = null;
            }
        }
        else if(entry.isBranch()) {
            if(entry.parent != null) {
                if(entry.parent.left == entry) {
                    entry.parent.left = entry.left != null ? entry.left : entry.right;
                    entry.parent.left.parent = entry.parent;
                } else {
                    entry.parent.right = entry.left != null ? entry.left : entry.right;
                    entry.parent.right.parent = entry.parent;
                }

                entry.parent.max = Math.max(entry.parent.end, Math.max(maxend(entry.parent.left), maxend(entry.parent.right)));

                removeRebalance(entry.parent);
                entry.parent = null;
                entry.left = null;
                entry.right = null;
            } else {
                root = entry.left != null ? entry.left : entry.right;
                root.parent = null;
                entry.left = null;
                entry.right = null;
            }
        } else {
            Entry succ = successor(entry);
            replaceNode(entry, succ);

            removeRebalance(succ.parent);
        }
    }

    /**
     * <b>DEBUG UTILITY</b> Computes the actual height of tree.
     */
    private int height(int depth, Entry entry, Reference2IntOpenHashMap<Entry> map) {
        if(entry.isLeaf()) {
            map.put(entry, 0);
            return 0;
        }
        else if (entry.isBranch()) {
            int retval;
            if(entry.left != null) {
                map.put(entry, retval=height(depth+1, entry.left, map)+1);
            } else {
                map.put(entry, retval=height(depth+1, entry.right, map)+1);
            }

            return retval;
        } else {
            int height = Math.max(height(depth+1, entry.left, map)+1,
                                  height(depth+1, entry.right, map)+1);
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
        height(0, root, heightMap);

        ArrayDeque<Entry> stack = new ArrayDeque<>();

        Entry start = root;
        while(start != null) {
            stack.push(start);
            start = start.left;
        }

        while(!stack.isEmpty()) {
            Entry node = stack.pop();

            if(Math.abs(heightMap.getInt(root.left)-heightMap.getInt(root.right)) > 1)
                throw new AssertionError("Balance at: "
                                                 + node.toString()
                                                 + " = "
                                                 + (heightMap.getInt(root.left) - heightMap.getInt(root.right)));

            if(node.right != null) {
                Entry current = node.right;
                while(current != null) {
                    stack.push(current);
                    current = current.left;
                }
            }
        }

        return true;
    }

    private void replaceNode(Entry node, Entry succ) {
        Entry parent = node.parent;
        Entry left = node.left;
        Entry right = node.right;

        //Entry succLeft = succ.left;
        Entry succRight = succ.right;
        Entry succParent = succ.parent;

        succ.parent = parent;
        if(parent == null) {
            this.root = succ;
        } else {
            if(parent.left == node) {
                parent.left = succ;
            } else {
                parent.right = succ;
            }
        }

        if (succParent.left == succ) {
            succParent.left = succ.right;
            if(succ.right != null)
                succ.right.parent = succParent;
        } else {
            succParent.right = succ.right;
            if(succ.right != null)
                succ.right.parent = succParent;
        }

        succ.left = left;
        succ.left.parent = succ;

        if(right != succ) {
            succ.right = right;
            succ.right.parent = succ;
        }

        succ.max = Math.max(succ.end, Math.max(maxend(succ.left), maxend(succ.right)));
        if(succ.parent != null) {
            succ.parent.max = Math.max(succ.parent.end, Math.max(maxend(succ.parent.left), maxend(succ.parent.right)));
        }
    }

    /**
     * Clean the tree, basically set root to null, will not mark node parents as null for performance reasons.
     */
    public void clear() {
        root = null;
    }

    /**
     * Forward iterator of all entries in this index
     */
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
}
