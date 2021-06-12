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

package org.nuxeo.labs.compound.adapter;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

public class CompoundDocumentAdapter implements CompoundDocument {

    protected final DocumentModel doc;

    public CompoundDocumentAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public DocumentModel getPreviewDocument() {
        String previewId = (String) doc.getPropertyValue(COMPOUND_PREVIEW_DOCUMENT_PROP);
        if (StringUtils.isNotEmpty(previewId)) {
            return doc.getCoreSession().getDocument(new IdRef(previewId));
        } else {
            return null;
        }
    }

    @Override
    public void setPreviewDocument(DocumentModel preview) {
        doc.setPropertyValue(COMPOUND_PREVIEW_DOCUMENT_PROP, preview.getId());
    }

    @Override
    public DocumentModel getThumbnailDocument() {
        String thumbnailId = (String) doc.getPropertyValue(COMPOUND_THUMBNAIL_DOCUMENT_PROP);
        if (StringUtils.isNotEmpty(thumbnailId)) {
            return doc.getCoreSession().getDocument(new IdRef(thumbnailId));
        } else {
            return null;
        }
    }

    @Override
    public void setThumbnailDocument(DocumentModel thumbnail) {
        doc.setPropertyValue(COMPOUND_THUMBNAIL_DOCUMENT_PROP, thumbnail.getId());
    }
}
