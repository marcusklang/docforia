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

import se.lth.cs.docforia.query.Proposition;
import se.lth.cs.docforia.query.Var;

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
        protected Proposition key;

        public Group(Proposition key) {
            this.key = key;
        }

        public Group copy() {
            return new Group(key.copy());
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == null)
                return false;

            if(obj instanceof Group) {
                return key.equals(((Group) obj).key);
            }
            else
                return false;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }
/*
    public DocumentIterable<GroupProposition> query() {
        HashMap<Group,List<Proposition>> groups = new HashMap<Group, List<Proposition>>();
        List<Proposition> result = parent.result(-1);

        Group grp = new Group();

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
    }*/
}
