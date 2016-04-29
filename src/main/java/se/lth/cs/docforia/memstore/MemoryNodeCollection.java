package se.lth.cs.docforia.memstore;
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

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import se.lth.cs.docforia.DocumentNodeNavigator;
import se.lth.cs.docforia.LayerRef;
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.util.DocumentIterable;
import se.lth.cs.docforia.util.DocumentIterableBase;

import java.util.*;

/**
 * Memory Node Collection
 */
public class MemoryNodeCollection extends DocumentIterableBase<NodeRef> implements DocumentIterable<NodeRef> {
    public final MemoryDocumentStore doc;
    public Int2ObjectAVLTreeMap nodes = new Int2ObjectAVLTreeMap();
    public final Key key;

    public static class Key implements Comparable<Key>, LayerRef {
        public final String layer;
        public final String variant;
        public int id;

        public Key(String layer, String variant) {
            this.layer = layer;
            this.variant = variant;
        }

        @Override
        public boolean equal(LayerRef ref) {
            return id == ((Key)ref).id;
        }

        @Override
        public String getLayer() {
            return layer;
        }

        @Override
        public String getVariant() {
            return variant;
        }

        @Override
        public int compareTo(Key o) {
            int result = layer.compareTo(o.layer);
            if(result == 0) {
                if(variant == null || o.variant == null)
                    return -Boolean.compare(variant == null, o.variant == null);
                else {
                    return variant.compareTo(o.variant);
                }
            }
            else
                return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!layer.equals(key.layer)) return false;
            return !(variant != null ? !variant.equals(key.variant) : key.variant != null);
        }

        @Override
        public int hashCode() {
            int result = layer.hashCode();
            result = 31 * result + (variant != null ? variant.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "( " + layer + ", " + Objects.toString(variant) + " )";
        }
    }

    public MemoryNodeCollection(MemoryDocumentStore doc, Key key) {
        this.doc = doc;
        this.key = key;
    }

    public Key getKey() {
        return key;
    }

    private static final class SingleElementIterator implements Iterator<NodeRef> {
        protected boolean nxt = true;
        protected MemoryNode node;

        public SingleElementIterator(MemoryNode node) {
            this.node = node;
        }

        @Override
        public boolean hasNext() {
            return nxt;
        }

        @Override
        public NodeRef next() {
            nxt = false;
            return node;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<NodeRef> iterator() {
        if(nodes.isEmpty())
            return Collections.emptyIterator();

        final ObjectBidirectionalIterator<Int2ObjectMap.Entry> nodesIter = nodes.int2ObjectEntrySet().iterator();
        final SingleElementIterator template = new SingleElementIterator(null);

        return new Iterator<NodeRef>() {
            Iterator<NodeRef> iterator = Collections.emptyIterator();
            NodeRef nxt = null;

            private boolean moveForward() {
                if(nxt == null) {
                    if(!iterator.hasNext() && nodesIter.hasNext()) {
                        Int2ObjectMap.Entry next = nodesIter.next();
                        if(next.getValue() instanceof MemoryNode) {
                            iterator = template;
                            template.node = (MemoryNode)next.getValue();
                            template.nxt = true;
                        }
                        else if(next.getValue() instanceof ObjectArrayList) {
                            iterator = ((ObjectArrayList<NodeRef>) next.getValue()).iterator();
                        }
                        else
                            throw new UnsupportedOperationException("BUG! Logic error in code!");

                        nxt = iterator.next();
                        return true;
                    }
                    else if(iterator.hasNext()) {
                        nxt = iterator.next();
                        return true;
                    }
                    else
                        return false;
                }
                else
                    return true;
            }

            @Override
            public boolean hasNext() {
                return moveForward();
            }

            @Override
            public NodeRef next() {
                if(!moveForward())
                    throw new NoSuchElementException();

                NodeRef retVal = nxt;
                nxt = null;
                return retVal;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public MemoryNode create() {
        MemoryNode memoryNode = new MemoryNode(this);
        memoryNode.start = -1;
        memoryNode.end = -1;

        add(memoryNode);
        return memoryNode;
    }

    public MemoryNode create(int start, int end) {
        MemoryNode memoryNode = new MemoryNode(this);
        memoryNode.start = start;
        memoryNode.end = end;

        add(memoryNode);
        return memoryNode;
    }

    public void add(MemoryNode node) {
        node.storage = this;
        Object val = nodes.get(node.start);
        if(val == null) {
            nodes.put(node.start, node);
        } else if(val instanceof MemoryNode) {
            ObjectArrayList<MemoryNode> nodeGroup = new ObjectArrayList<>();
            nodeGroup.add((MemoryNode)val);
            nodeGroup.add(node);
            nodes.put(((MemoryNode) val).start, nodeGroup);
        } else if(val instanceof ObjectArrayList) {
            ObjectArrayList<MemoryNode> nodeGroup = (ObjectArrayList<MemoryNode>) val;
            nodeGroup.add(node);
        }
    }

    public void remove(MemoryNode node) {
        unlink(node);
        node.remove();
    }

    private void unlink(MemoryNode node) {
        Object o = nodes.get(node.start);
        if(o instanceof MemoryNode) {
            nodes.remove(node.start);
        }
        else if(o instanceof ObjectArrayList) {
            ObjectArrayList lst = (ObjectArrayList)o;
            lst.remove(node);
            if(lst.size() == 0)
                nodes.remove(node.start);
            else if(lst.size() == 1)
                nodes.put(node.start, lst.get(0));
        }
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public void variantChanged(MemoryNode node, String variant) {
        if(!variant.equals(key.variant)) {
            unlink(node);
            doc.getNodeCollection(this.key.layer, variant).add(node);
        }
    }

    public void rangeChanged(MemoryNode node, int start, int end) {
        unlink(node);
        node.start = start;
        node.end = end;
        add(node);
    }

    private static abstract class ElementNavigator {
        public abstract Object source();
        public abstract boolean next();
        public abstract boolean prev();
        public abstract MemoryNode current();
    }

    private static class SingleElementNavigator extends ElementNavigator {
        private MemoryNode elem = null;

        public void reset(MemoryNode node) {
            this.elem = node;
        }

        @Override
        public Object source() {
            return elem;
        }

        @Override
        public boolean next() {
            return false;
        }

        @Override
        public boolean prev() {
            return false;
        }

        @Override
        public MemoryNode current() {
            return elem;
        }
    }

    private static class ListElementNavigator extends ElementNavigator {
        private int pos = 0;
        private List<MemoryNode> elems;

        public void reset(List<MemoryNode> elems) {
            this.elems = elems;
            this.pos = 0;
        }

        @Override
        public Object source() {
            return elems;
        }

        @Override
        public boolean next() {
            if(pos == elems.size()-1)
                return false;
            else {
                pos++;
                return true;
            }
        }

        public MemoryNode current() {
            return elems.get(pos);
        }

        @Override
        public boolean prev() {
            if(pos == 0)
                return false;
            else {
                pos--;
                return true;
            }
        }
    }

    private static class MapIteratorElementNavigator {
        public Int2ObjectMap.Entry<Object> curr;
        public ObjectBidirectionalIterator<Int2ObjectMap.Entry<Object>> iter;
        public int lastDir = 1;

        public void reset(ObjectBidirectionalIterator<Int2ObjectMap.Entry<Object>> iter) {
            this.iter = iter;
            if(iter.hasNext()) {
                curr = iter.next();
                lastDir = 1;
            }
            else if(iter.hasPrevious()) {
                curr = iter.previous();
                lastDir = -1;
            }
            else
            {
                lastDir = 0;
                curr = null;
            }
        }

        public Int2ObjectMap.Entry<Object> current() {
            return curr;
        }

        public boolean next() {
            if(lastDir == 0)
                return false;

            if(lastDir == -1) {
                lastDir = 1;
                iter.next();
            }

            if(iter.hasNext()) {
                curr = iter.next();
                return true;
            }
            else
                return false;
        }

        public boolean prev() {
            if(lastDir == 0)
                return false;

            if(lastDir == 1) {
                iter.previous();
                lastDir = -1;
            }

            if(iter.hasPrevious()) {
                curr = iter.previous();
                return true;
            }
            else
                return false;
        }
    }

    private static class TempEntry implements Int2ObjectMap.Entry<Object> {
        public int key;

        @Override
        public int getIntKey() {
            return key;
        }

        @Override
        public Integer getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return null;
        }

        @Override
        public Object setValue(Object value) {
            return null;
        }
    }

    public class Gen2NodeNavigator implements DocumentNodeNavigator {

        @SuppressWarnings("unchecked")
        private final Int2ObjectSortedMap<Object> annotations = nodes.tailMap(0);

        private final SingleElementNavigator singleNav = new SingleElementNavigator();
        private final ListElementNavigator listNav = new ListElementNavigator();
        private final TempEntry entry = new TempEntry();

        private final MapIteratorElementNavigator mapnav = new MapIteratorElementNavigator();
        private ElementNavigator navigator;

        public Gen2NodeNavigator() {
            moveToStart();
        }

        private MemoryNode prev;
        private MemoryNode curr;
        private MemoryNode next;

        private void moveForward() {
            if(!(curr == null && next != null)) {
                if(!navigator.next()) {
                    if(!mapnav.next()) {
                        next = null;
                    }
                    else {
                        setIterator(mapnav.current().getValue(), false);
                        next = navigator.current();
                    }
                }
                else
                    next = navigator.current();
            }
        }

        @SuppressWarnings("unchecked")
        private void setIterator(Object o, boolean end) {
            if(o instanceof MemoryNode) {
                navigator = singleNav;
                singleNav.reset((MemoryNode)o);
            }
            else if(o instanceof List) {
                navigator = listNav;
                listNav.reset((List<MemoryNode>)o);
                if(end)
                    listNav.pos = listNav.elems.size()-1;
            }
            else
                throw new UnsupportedOperationException();
        }

        private void moveBackward() {
            if(!navigator.prev()) {
                if(!mapnav.prev()) {
                    prev = null;
                }
                else {
                    setIterator(mapnav.current().getValue(), true);
                    prev = navigator.current();
                }
            }
            else
                prev = navigator.current();
        }

        @SuppressWarnings("unchecked")
        private void moveToStart() {
            prev = null;
            curr = null;
            mapnav.reset(annotations.int2ObjectEntrySet().iterator());
            if(mapnav.current() == null)
                next = null;
            else
            {
                setIterator(mapnav.current().getValue(),false);
                next = navigator.current();
            }
        }

        private void moveTo(int pos, boolean floor) {
            entry.key = pos;
            mapnav.reset(annotations.int2ObjectEntrySet().iterator(entry));
            setIterator(mapnav.current().getValue(), false);

            prev = null;
            curr = null;
            next = null;

            if(mapnav.current() == null)
                return;

            if(floor) {
                if(mapnav.current().getIntKey() > pos) {
                    if(mapnav.prev()) {
                        setIterator(mapnav.current().getValue(), false);
                    }
                }

                next = navigator.current();
            } else {
                if(mapnav.current().getIntKey() > pos) {
                    if(mapnav.prev()) {
                        if(mapnav.current().getIntKey() < pos)
                            mapnav.next();
                        else
                            setIterator(mapnav.current().getValue(), false);
                    }
                }

                next = navigator.current();
            }
        }

        @Override
        public NodeRef current() {
            return curr;
        }

        @Override
        public boolean next() {
            moveForward();
            if(next == null)
                return false;

            curr = next;
            return true;
        }

        @Override
        public boolean nextFloor(int start) {
            moveForward();
            if(next == null)
                return false;
            else if(next.start >= start)
            {
                curr = next;
                return true;
            }
            else
            {
                moveTo(start, true);
                if(next != null) {
                    curr = next;
                    return true;
                }
                else
                    return false;
            }
        }

        @Override
        public boolean hasReachedEnd() {
            return next == null;
        }

        @Override
        public boolean prev() {
            moveBackward();
            if(prev == null)
                return false;

            curr = prev;
            return true;
        }

        @Override
        public void reset() {
            moveToStart();
        }

        @Override
        public boolean next(int start) {
            moveForward();
            if(next == null)
                return false;
            else if(next.start >= start)
            {
                curr = next;
                return true;
            }
            else
            {
                moveTo(start, false);
                if(next != null) {
                    curr = next;
                    return true;
                }
                else
                    return false;
            }
        }

        @Override
        public int start() {
            return curr.start;
        }

        @Override
        public int end() {
            return curr.end;
        }
    }

    @SuppressWarnings("unchecked")
    public DocumentNodeNavigator annotationNavigator() {
        final Int2ObjectSortedMap<Object> annotations = nodes.tailMap(0);

        return new Gen2NodeNavigator();
    }
}
