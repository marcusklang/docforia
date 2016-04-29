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

import java.util.ArrayList;
import java.util.List;

/**
 * Compiled Query
 */
public class Query {
    private final Document doc;
    private final List<Var> vars;
    private final List<Predicate> predicates;

    public Query(Document doc, List<Var> vars, List<Predicate> predicates) {
        this.doc = doc;
        this.vars = vars;
        this.predicates = predicates;
    }

    public List<Proposition> evaluate()
    {
        //1. Initialize
        ArrayList<Proposition> props = new ArrayList<Proposition>();

        //2. Main search loop
        int p = 0;
        Proposition current = new Proposition(doc, vars.size());
        predicates.get(p).enter(current);
        while(p != -1) {
            Predicate pred = predicates.get(p);

            if(!pred.next(current)) {
                //no more propositions
                pred.exit(current);
                p--;
            }
            else
            {
                p++;
                if(p == predicates.size()) {
                    //goal reached!
                    props.add(current.copy());
                    p--;
                }
                else
                {
                    predicates.get(p).enter(current);
                }
            }
        }

        return props;
    }

    public List<Proposition> evaluate(int n)
    {
        //1. Initialize
        ArrayList<Proposition> props = new ArrayList<Proposition>();

        //2. Main search loop
        int p = 0;
        Proposition current = new Proposition(doc, vars.size());
        predicates.get(p).enter(current);

        int i = 0;
        while(p != -1 && i < n) {
            Predicate pred = predicates.get(p);

            if(!pred.next(current)) {
                //no more propositions
                pred.exit(current);
                p--;
            }
            else
            {
                p++;
                if(p == predicates.size()) {
                    //goal reached!
                    props.add(current.copy());
                    i++;
                    p--;
                }
                else
                {
                    predicates.get(p).enter(current);
                }
            }
        }

        return props;
    }
}
