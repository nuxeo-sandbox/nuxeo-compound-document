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

package org.nuxeo.labs.compound.thumbnail;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.ecm.platform.thumbnail.factories.ThumbnailDocumentFactory;
import org.nuxeo.runtime.api.Framework;

import static org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants.THUMBNAIL_FACET;
import static org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants.THUMBNAIL_PROPERTY_NAME;
import static org.nuxeo.labs.compound.adapter.CompoundDocument.COMPOUND_PREVIEW_DOCUMENT_PROP;

public class CompoundDocumentThumbnailFactory extends ThumbnailDocumentFactory {

    @Override
    public Blob getThumbnail(DocumentModel doc, CoreSession session) {

        //give priority to thumbnail property
        if (doc.hasFacet(THUMBNAIL_FACET)) {
            Blob blob = (Blob) doc.getPropertyValue(THUMBNAIL_PROPERTY_NAME);
            if (blob!=null) {
                return blob;
            }
        }

        ThumbnailService thumbnailService = Framework.getService(ThumbnailService.class);

        // otherwise get the thumbnail from the preview document
        String previewDocId = (String) doc.getPropertyValue(COMPOUND_PREVIEW_DOCUMENT_PROP);
        if (StringUtils.isNotEmpty(previewDocId)) {
            DocumentModel previewDoc = session.getDocument(new IdRef(previewDocId));
            Blob  thumbnailBlob = thumbnailService.getThumbnail(previewDoc,session);
            if (thumbnailBlob != null) {
                return thumbnailBlob;
            }
        }

        //fallback to default factory
        return getDefaultThumbnail(doc);
    }

}