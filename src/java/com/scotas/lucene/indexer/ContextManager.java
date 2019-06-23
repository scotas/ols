package com.scotas.lucene.indexer;

import java.util.Hashtable;

/**
 * Copyright 2004 The Apache Software Foundation
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
public final class ContextManager
{
    private static Hashtable ctx = new Hashtable(100);
    private static int count = 0;
    private static final int MAXCOUNT = 0x7fffffff;

    public ContextManager()
    {
    }

    public static Object clearContext(int key) {
        if(key < 0 || key > MAXCOUNT) {
            throw new InstantiationError("count is too large");
        } else {
            Object r = ctx.remove(new Integer(key));
            return r;
        }
    }

    public static Object getContext(int key) {
        if(key < 0 || key > MAXCOUNT)
            throw new RuntimeException("count is too large");
        else
            return ctx.get(new Integer(key));
    }

    public static int setContext(Object o) {
        int assignedCount = count;
        ctx.put(new Integer(assignedCount), o);
        if (++count == MAXCOUNT)
          count = 0;
        return assignedCount;
    }
}
