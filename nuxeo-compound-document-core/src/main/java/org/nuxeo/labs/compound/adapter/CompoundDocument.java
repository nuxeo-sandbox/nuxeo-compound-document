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

import org.nuxeo.ecm.core.api.DocumentModel;

public interface CompoundDocument {

    String COMPOUND_FACET = "Compound";

    String COMPOUND_PREVIEW_DOCUMENT_PROP = "compound:previewDocument";

    String COMPOUND_THUMBNAIL_DOCUMENT_PROP = "compound:thumbnailDocument";

    /**
     * @return the preview document
     */
    DocumentModel getPreviewDocument();

    /**
     * @param preview the documennt to set as the preview
     */
    void setPreviewDocument(DocumentModel preview);

    /**
     * @return the thumbnail document
     */
    DocumentModel getThumbnailDocument();

    /**
     * @param thumbnail the document to set as the thumbnail
     */
    void setThumbnailDocument(DocumentModel thumbnail);

}
