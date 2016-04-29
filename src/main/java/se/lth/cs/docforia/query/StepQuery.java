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

import java.util.List;

/**
 * Compiled iterative query
 */
public class StepQuery {
    private final Document doc;
    private final List<Var> vars;
    private final List<Predicate> predicates;

    public StepQuery(Document doc, List<Var> vars, List<Predicate> predicates) {
        this.doc = doc;
        this.vars = vars;
        this.predicates = predicates;
        this.current = new Proposition(doc, vars.size());
        this.predicates.get(p).enter(current);
    }

    private int p = 0;
    private Proposition current;

    public Proposition current() {
        return current;
    }

    public boolean step()
    {
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
                    p--;
                    return true;
                }
                else
                {
                    predicates.get(p).enter(current);
                }
            }
        }

        return false;
    }
}
