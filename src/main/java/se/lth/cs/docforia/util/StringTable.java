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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * Utility class for human readable text tables for output e.g. to stdout
 */
public class StringTable {
    private ArrayList<Object[]> rows = new ArrayList<>();
    private String[] cols;
    private ValueFormatter[] colFormatters;
    private boolean[] alignLeft;

    private static final ValueFormatter defaultFormatter = o -> o == null ? "" : Objects.toString(o);

    public StringTable(String...cols) {
        this.cols = cols;
        colFormatters = new ValueFormatter[cols.length];
        alignLeft = new boolean[cols.length];
        for (int i = 0; i < colFormatters.length; i++) {
            colFormatters[i] = defaultFormatter;
            alignLeft[i] = true;
        }
    }

    public StringTable row(Object...data) {
        if(data.length < cols.length)
            rows.add(Arrays.copyOf(data, cols.length));
        else if(data.length > cols.length) {
            rows.add(Arrays.copyOf(data, cols.length));
        }
        else
            rows.add(data);

        return this;
    }

    public StringTable setFormatter(int col, ValueFormatter formatter) {
        colFormatters[col] = formatter;
        return this;
    }

    public StringTable alignLeft(int col) {
        alignLeft[col] = true;
        return this;
    }

    public StringTable alignRight(int col) {
        alignLeft[col] = false;
        return this;
    }

    public static void repeat(char ch, int num, StringBuilder sb) {
        for(int i = 0; i < num; i++)
            sb.append(ch);
    }

    public static void align(boolean left, int len, String text, StringBuilder sb) {
        if(text.length() == len)
            sb.append(text);
        else {
            if(left) {
                sb.append(text);
                repeat(' ', len-text.length(), sb);
            } else {
                repeat(' ', len-text.length(), sb);
                sb.append(text);
            }
        }
    }

    public void print(PrintStream output) {
        int[] minColWidths = new int[cols.length];
        for (int i = 0; i < cols.length; i++) {
            minColWidths[i] = cols[i].length();
        }

        for (Object[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                minColWidths[i] = Math.max(colFormatters[i].format(row[i]).length(), minColWidths[i]);
            }
        }

        //Top line
        StringBuilder line = new StringBuilder();
        line.append("+");
        for(int i = 0; i < cols.length; i++) {
            line.append("-");
            repeat('-', minColWidths[i], line);
            line.append("-+");
        }
        output.println(line);
        line.setLength(0);

        line.append("|");
        for(int i = 0; i < cols.length; i++) {
            line.append(" ");
            align(true, minColWidths[i], cols[i], line);
            line.append(" |");
        }
        output.println(line);
        line.setLength(0);

        //Bottom seperator
        line.append("+");
        for(int i = 0; i < cols.length; i++) {
            line.append("-");
            repeat('-', minColWidths[i], line);
            line.append("-+");
        }
        output.println(line);
        line.setLength(0);

        for (Object[] row : rows) {
            line.append("|");
            for(int i = 0; i < cols.length; i++) {
                line.append(" ");
                align(alignLeft[i], minColWidths[i], colFormatters[i].format(row[i]), line);
                line.append(" |");
            }
            output.println(line);
            line.setLength(0);
        }

        line.append("+");
        for(int i = 0; i < cols.length; i++) {
            line.append("-");
            repeat('-', minColWidths[i], line);
            line.append("-+");
        }
        output.println(line);
    }
}
