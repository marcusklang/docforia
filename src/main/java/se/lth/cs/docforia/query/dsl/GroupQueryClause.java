package se.lth.cs.docforia.query.dsl;
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

import se.lth.cs.docforia.StoreRef;
import se.lth.cs.docforia.query.GroupProposition;
import se.lth.cs.docforia.query.Proposition;
import se.lth.cs.docforia.query.Var;
import se.lth.cs.docforia.util.DocumentIterable;
import se.lth.cs.docforia.util.DocumentIterables;

import java.util.*;

/**
 * Group Query Clause
 */
public class GroupQueryClause {
    private final QueryClause parent;
    private final Var[] groupBy;

    public GroupQueryClause(QueryClause parent, Var[] groupBy) {
        this.parent = parent;
        this.groupBy = groupBy;
    }

    protected static class Group {
        protected StoreRef[] key;

        public Group(StoreRef[] key) {
            this.key = key;
        }

        public Group copy() {
            return new Group(Arrays.copyOf(key,key.length));
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == null)
                return false;

            if(obj instanceof Group) {
                return Arrays.deepEquals(key, ((Group)obj).key);
            }
            else
                return false;
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(key);
        }
    }

    protected void groupOf(Group grp, Proposition prop) {
        for (int i = 0; i < groupBy.length; i++) {
            grp.key[i] = prop.proposition[groupBy[i].getIndex()];
        }
    }

    public DocumentIterable<GroupProposition> query() {
        HashMap<Group,List<Proposition>> groups = new HashMap<Group, List<Proposition>>();
        List<Proposition> result = parent.result(-1);

        Group grp = new Group(new StoreRef[groupBy.length]);

        for (Proposition proposition : result) {
            groupOf(grp, proposition);
            List<Proposition> propositions = groups.get(grp);
            if(propositions == null) {
                propositions = new ArrayList<Proposition>();
                groups.put(grp.copy(), propositions);
            }

            propositions.add(proposition);
        }

        List<GroupProposition> grouplist = new ArrayList<GroupProposition>();

        for (Map.Entry<Group, List<Proposition>> groupListEntry : groups.entrySet()) {
            GroupProposition groupProposition = new GroupProposition(groupListEntry.getValue().get(0),groupListEntry.getValue());
            grouplist.add(groupProposition);
        }

        if(parent.orderByRange.size() > 0) {
            final Comparator<Proposition> sorter = parent.orderByComparator();

            Collections.sort(grouplist, (o1, o2) -> sorter.compare(o1.key(), o2.key()));
        }

        return DocumentIterables.wrap(grouplist);
    }
}
