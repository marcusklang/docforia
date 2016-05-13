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

import java.util.Comparator;

/**
 * Range comparator for use in sorting by annotation starts
 */
public class PropositionRangeComparator implements Comparator<Proposition> {
    private NodeVar[] nodeVars;

    public PropositionRangeComparator(NodeVar...nodeVars) {
        this.nodeVars = nodeVars;
    }

    @Override
    public int compare(Proposition o1, Proposition o2) {
        for (NodeVar nodeVar : nodeVars) {
            int result = o1.get(nodeVar).compareTo(o2.get(nodeVar));
            if (result != 0)
                return result;
        }
        return 0;
    }
}
