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

package org.nuxeo.labs.compound.io;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.labs.compound.adapter.CompoundDocument.COMPOUND_FACET;
import static org.nuxeo.labs.compound.adapter.CompoundDocument.COMPOUND_PREVIEW_DOCUMENT_PROP;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.thumbnail.io.ThumbnailJsonEnricher;

import com.fasterxml.jackson.core.JsonGenerator;

@Setup(mode = SINGLETON, priority = REFERENCE)
public class CompoundDocumentThumbnailEnricher extends ThumbnailJsonEnricher {

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        // not compound or no preview document, fallback to default
        if (!document.hasFacet(COMPOUND_FACET) || document.getPropertyValue(COMPOUND_PREVIEW_DOCUMENT_PROP) == null) {
            super.write(jg, document);
            return;
        }

        // get thumbnail
        Blob thumbnail = document.getAdapter(ThumbnailAdapter.class).getThumbnail(document.getCoreSession());

        // if thumbnail is null or an icon, fallback to default
        if (thumbnail == null || thumbnail.getDigest() == null) {
            super.write(jg, document);
            return;
        }

        jg.writeFieldName(NAME);
        jg.writeStartObject();
        jg.writeStringField(THUMBNAIL_URL_LABEL,
                String.format(THUMBNAIL_URL_PATTERN, ctx.getBaseUrl().replaceAll("/$", ""),
                        document.getRepositoryName(), document.getId(),
                        URLEncoder.encode(defaultString(thumbnail.getDigest()), StandardCharsets.UTF_8)));
        jg.writeEndObject();

    }
}
