package se.lth.cs.docforia.query;
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

import se.lth.cs.docforia.Document;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Query collectors
 */
public class QueryCollectors {
    private static class HashPartialResult {
        private Function<Proposition,Proposition> keyFunction;
        private Function<Proposition,Proposition> valueFunction;
        private HashMap<Proposition,List<Proposition>> data = new HashMap<>();

        public HashPartialResult(Function<Proposition, Proposition> keyFunction, Function<Proposition,Proposition> valueFunction) {
            this.keyFunction = keyFunction;
            this.valueFunction = valueFunction;
        }

        public void add(Proposition proposition) {
            Proposition key = keyFunction.apply(proposition);
            List<Proposition> values = data.get(key);
            if(values == null) {
                values = new ArrayList<>();
                data.put(key, values);
            }

            values.add(valueFunction.apply(proposition));
        }

        public HashPartialResult merge(HashPartialResult result) {
            for (Map.Entry<Proposition, List<Proposition>> entry : result.data.entrySet()) {
                List<Proposition> values = data.get(entry.getKey());
                if(values == null)
                {
                    data.put(entry.getKey(), values);
                }
                else {
                    values.addAll(entry.getValue());
                }
            }
            return this;
        }
    }

    private static final Set<Collector.Characteristics> UnorderedCharacteristic = EnumSet.of(Collector.Characteristics.UNORDERED);

    /**
     * Create group list collector
     * @param keyFunction key extraction function
     */
    public static Collector<Proposition,HashPartialResult, List<PropositionGroup>> toListGroups(Function<Proposition,Proposition> keyFunction, Function<Proposition,Proposition> valueFunction) {
        return new Collector<Proposition, HashPartialResult, List<PropositionGroup>>() {
            @Override
            public Supplier<HashPartialResult> supplier() {
                return () -> new HashPartialResult(keyFunction, valueFunction);
            }

            @Override
            public BiConsumer<HashPartialResult, Proposition> accumulator() {
                return HashPartialResult::add;
            }

            @Override
            public BinaryOperator<HashPartialResult> combiner() {
                return HashPartialResult::merge;
            }

            @Override
            public Function<HashPartialResult, List<PropositionGroup>> finisher() {
                return pr -> pr.data.entrySet().stream().map(e -> new PropositionGroup(e.getKey(), e.getValue())).collect(Collectors.toList());
            }

            @Override
            public Set<Characteristics> characteristics() {
                return UnorderedCharacteristic;
            }
        };
    }

    /**
     * Create a group list collector that also sorts the values
     * @param keyFunction    key extraction function
     * @param valueComparator value sorting
     */
    public static Collector<Proposition,?,List<PropositionGroup>> toSortedListGroups(Function<Proposition,Proposition> keyFunction, Function<Proposition,Proposition> valueFunction, Comparator<Proposition> valueComparator) {
        return new Collector<Proposition, HashPartialResult, List<PropositionGroup>>() {
            @Override
            public Supplier<HashPartialResult> supplier() {
                return () -> new HashPartialResult(keyFunction, valueFunction);
            }

            @Override
            public BiConsumer<HashPartialResult, Proposition> accumulator() {
                return HashPartialResult::add;
            }

            @Override
            public BinaryOperator<HashPartialResult> combiner() {
                return HashPartialResult::merge;
            }

            @Override
            public Function<HashPartialResult, List<PropositionGroup>> finisher() {
                return pr -> pr.data.entrySet()
                                    .stream()
                                    .map(e -> {
                                        List<Proposition> values = e.getValue();
                                        Collections.sort(values, valueComparator);
                                        return new PropositionGroup(e.getKey(), values);
                                    })
                                    .sorted((a,b) -> valueComparator.compare(a.value(0), b.value(0)))
                                    .collect(Collectors.toList());
            }

            @Override
            public Set<Characteristics> characteristics() {
                return UnorderedCharacteristic;
            }
        };
    }

    /**
     * Create a group list collector that also sorts the values
     * @param keyFunction    key extraction function
     * @param keyComparator  key comparator function
     * @return
     */
    public static Collector<Proposition,?,List<PropositionGroup>> toSortedKeyListGroups(Function<Proposition,Proposition> keyFunction, Function<Proposition,Proposition> valueFunction, Comparator<Proposition> keyComparator) {
        return new Collector<Proposition, HashPartialResult, List<PropositionGroup>>() {
            @Override
            public Supplier<HashPartialResult> supplier() {
                return () -> new HashPartialResult(keyFunction, valueFunction);
            }

            @Override
            public BiConsumer<HashPartialResult, Proposition> accumulator() {
                return HashPartialResult::add;
            }

            @Override
            public BinaryOperator<HashPartialResult> combiner() {
                return HashPartialResult::merge;
            }

            @Override
            public Function<HashPartialResult, List<PropositionGroup>> finisher() {
                return pr -> pr.data.entrySet()
                                    .stream()
                                    .map(e -> new PropositionGroup(e.getKey(), e.getValue()))
                                    .sorted((x,y) -> keyComparator.compare(x.key(), y.key()))
                                    .collect(Collectors.toList());
            }

            @Override
            public Set<Characteristics> characteristics() {
                return UnorderedCharacteristic;
            }
        };
    }

    /**
     * Create a group list collector that also sorts the values
     * @param keyFunction    key extraction function
     * @param keyComparator  key comparator
     * @param valueComparator value comparator
     * @param valueFunction   value extraction function
     */
    public static Collector<Proposition,HashPartialResult,List<PropositionGroup>> toSortedListGroups(Function<Proposition,Proposition> keyFunction, Function<Proposition,Proposition> valueFunction, Comparator<Proposition> keyComparator, Comparator<Proposition> valueComparator) {
        return new Collector<Proposition, HashPartialResult, List<PropositionGroup>>() {
            @Override
            public Supplier<HashPartialResult> supplier() {
                return () -> new HashPartialResult(keyFunction, valueFunction);
            }

            @Override
            public BiConsumer<HashPartialResult, Proposition> accumulator() {
                return HashPartialResult::add;
            }

            @Override
            public BinaryOperator<HashPartialResult> combiner() {
                return HashPartialResult::merge;
            }

            @Override
            public Function<HashPartialResult, List<PropositionGroup>> finisher() {
                return pr -> pr.data.entrySet()
                                    .stream()
                                    .map(e -> {
                                        List<Proposition> values = e.getValue();
                                        Collections.sort(values, valueComparator);
                                        return new PropositionGroup(e.getKey(), values);
                                    })
                                    .sorted((a,b) -> keyComparator.compare(a.key(), b.key()))
                          .collect(Collectors.toList());
            }

            @Override
            public Set<Characteristics> characteristics() {
                return UnorderedCharacteristic;
            }
        };
    }

    public static class GroupCollectorBuilder {
        private Document document;
        private Var[] keys;
        private Var[] values = null;
        private NodeVar[] orderByKey = null;
        private NodeVar[] orderByValue = null;

        public GroupCollectorBuilder(Document document, Var...keys) {
            this.document = document;
            this.keys = keys;
        }

        public GroupCollectorBuilder values(Var...vars) {
            this.values = vars;
            return this;
        }

        public GroupCollectorBuilder orderByKey(NodeVar...vars) {
            this.orderByKey = vars;
            return this;
        }

        public GroupCollectorBuilder orderByValue(NodeVar...vars) {
            this.orderByValue = vars;
            return this;
        }

        public Collector<Proposition,?,List<PropositionGroup>> collector() {
            Function<Proposition,Proposition> keyFunction = StreamUtils.subset(document, keys);
            Function<Proposition,Proposition> valueFunction = values != null ? StreamUtils.subset(document, values) : p -> p;

            if(orderByKey != null && orderByValue != null) {
                return toSortedListGroups(keyFunction, valueFunction, StreamUtils.orderBy(orderByKey), StreamUtils.orderBy(orderByValue));
            } else if(orderByKey != null) {
                return toSortedKeyListGroups(keyFunction, valueFunction, StreamUtils.orderBy(orderByKey));
            }
            else if(orderByValue != null) {
                return toSortedListGroups(keyFunction, valueFunction, StreamUtils.orderBy(orderByValue));
            }
            else {
                return toListGroups(keyFunction, valueFunction);
            }
        }
    }

    /**
     * Construct a GroupBy collector builder
     * @param doc the document that the query is run over
     * @param keys the vars that are keys
     */
    public static GroupCollectorBuilder groupBy(Document doc, Var...keys) {
        return new GroupCollectorBuilder(doc, keys);
    }
}
