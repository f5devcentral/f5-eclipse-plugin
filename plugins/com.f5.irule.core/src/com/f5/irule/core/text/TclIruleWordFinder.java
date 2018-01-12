/*******************************************************************************
 * Copyright 2015-2017 F5 Networks, Inc.
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
 *******************************************************************************/
package com.f5.irule.core.text;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.IDocument;

public class TclIruleWordFinder {
    public static IRegion find (IDocument document, int offset) {
        String source = document.get();

        int start = -1;
        int end = -1;
        int length = source.length();
        int pos;

        String c;

        // find start of word
        pos = offset;
        while (pos > 0) {
            c = source.substring(pos, pos + 1);

            if (!c.matches("[\\w:]")) {
                break;
            }

            start = pos--;
        }

        // find end of word
        pos = offset;
        while (pos < length) {
            c = source.substring(pos, pos + 1);

            if (!c.matches("[\\w:]")) {
                break;
            }

            end = pos++;
        }

        if (start < 0 || end < 0) {
            return null;
        }

        return new Region(start, end - start + 1);
    }
}
