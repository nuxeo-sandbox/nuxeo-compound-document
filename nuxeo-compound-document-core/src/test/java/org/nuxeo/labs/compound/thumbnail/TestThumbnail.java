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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.labs.compound.TestFeature;
import org.nuxeo.labs.compound.TestHelper;
import org.nuxeo.labs.compound.adapter.CompoundDocument;
import org.nuxeo.labs.compound.adapter.CompoundDocumentAdapter;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import jakarta.inject.Inject;

import static org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants.THUMBNAIL_FACET;
import static org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants.THUMBNAIL_PROPERTY_NAME;
import static org.nuxeo.labs.compound.TestHelper.PREVIEW_THUMBNAIL_NAME;
import static org.nuxeo.labs.compound.TestHelper.THUMBNAIL_THUMBNAIL_NAME;

@RunWith(FeaturesRunner.class)
@Features({TestFeature.class})
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestThumbnail {

    @Inject
    protected CoreSession session;

    @Inject
    protected ThumbnailService thumbnailService;

    @Test
    public void testNoThumbnail() {
        DocumentModel compound = TestHelper.createCompoundDocument(session);

        Blob thumbnail = thumbnailService.getThumbnail(compound,session);
        Assert.assertNull(thumbnail);
    }

    @Test
    public void testPreviewThumbnail() {
        DocumentModel compound = TestHelper.createCompoundDocument(session);
        CompoundDocument compoundDocument = compound.getAdapter(CompoundDocumentAdapter.class);

        DocumentModel preview = TestHelper.createPreviewDocument(compound);

        compoundDocument.setPreviewDocument(preview);

        Blob thumbnail = thumbnailService.getThumbnail(compound,session);
        Assert.assertNotNull(thumbnail);
    }

    @Test
    public void testPreviewPriority() {
        DocumentModel compound = TestHelper.createCompoundDocument(session);
        CompoundDocument compoundDocument = compound.getAdapter(CompoundDocumentAdapter.class);

        DocumentModel previewDoc = TestHelper.createPreviewDocument(compound);
        compoundDocument.setPreviewDocument(previewDoc);
        Blob thumbnail = thumbnailService.getThumbnail(compound,session);
        Assert.assertNotNull(thumbnail);
        Assert.assertEquals(PREVIEW_THUMBNAIL_NAME,thumbnail.getFilename());

        DocumentModel thumbnailDoc = TestHelper.createThumbnailDocument(compound);
        compoundDocument.setThumbnailDocument(thumbnailDoc);
        thumbnail = thumbnailService.getThumbnail(compound,session);
        Assert.assertNotNull(thumbnail);
        Assert.assertEquals(THUMBNAIL_THUMBNAIL_NAME,thumbnail.getFilename());

        compound.addFacet(THUMBNAIL_FACET);
        compound.setPropertyValue(THUMBNAIL_PROPERTY_NAME, new StringBlob("dummy"));
        thumbnail = thumbnailService.getThumbnail(compound,session);
        Assert.assertNotNull(thumbnail);
        Assert.assertTrue(thumbnail instanceof StringBlob);
    }

}
