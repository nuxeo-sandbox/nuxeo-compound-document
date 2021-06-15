/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Michael Vachette
 */

package org.nuxeo.labs.compound.api;

import org.nuxeo.ecm.core.api.Blob;

import java.util.List;

public class CompoundArchive {
    Blob blob;
    List<String> validEntryList;
    String pathPrefix;

    public CompoundArchive(Blob blob, List<String> validEntryList, String pathPrefix) {
        this.blob = blob;
        this.validEntryList = validEntryList;
        this.pathPrefix = pathPrefix;
    }

    public Blob getBlob() {
        return blob;
    }

    public void setBlob(Blob blob) {
        this.blob = blob;
    }

    public List<String> getValidEntryList() {
        return validEntryList;
    }

    public void setValidEntryList(List<String> validEntryList) {
        this.validEntryList = validEntryList;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }
}
