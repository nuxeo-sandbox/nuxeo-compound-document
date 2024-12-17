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

package org.nuxeo.labs.compound.rendition;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.labs.compound.TestFeature;
import org.nuxeo.labs.compound.service.CompoundDocumentService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import static org.nuxeo.labs.compound.TestHelper.COMPOUND_DOC_TYPE;
import static org.nuxeo.labs.compound.TestHelper.INDD_ZIP_PATH;

@RunWith(FeaturesRunner.class)
@Features({TestFeature.class})
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({
        "org.nuxeo.ecm.actions",
        "org.nuxeo.ecm.platform.rendition.core"
})
public class TestCompound2ZipRendition {

    @Inject
    protected CoreSession session;

    @Inject
    protected RenditionService renditionService;

    @Inject
    protected CompoundDocumentService compoundDocumentService;

    @Test
    public void testOperation() throws IOException {
        Blob blob = new FileBlob(new File(getClass().getResource(INDD_ZIP_PATH).getPath()));
        DocumentModel compound = compoundDocumentService.createCompoundFromArchive(session.getRootDocument(), blob, COMPOUND_DOC_TYPE);

        Blob zip = renditionService.getRendition(compound,"compoundZipExport").getBlob();

        Assert.assertNotNull(zip);
        Assert.assertEquals("application/zip",zip.getMimeType());

        try (ZipFile zipFile = new ZipFile(zip.getFile())) {
            Assert.assertEquals(10, zipFile.stream().count());
        }

    }

}
