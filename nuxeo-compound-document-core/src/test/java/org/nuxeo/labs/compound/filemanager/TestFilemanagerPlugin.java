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

package org.nuxeo.labs.compound.filemanager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.labs.compound.TestFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

import static org.nuxeo.labs.compound.TestHelper.INDD_ZIP_PATH;
import static org.nuxeo.labs.compound.TestHelper.checkInddCompound;

@RunWith(FeaturesRunner.class)
@Features({TestFeature.class})
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestFilemanagerPlugin {

    @Inject
    protected CoreSession session;

    @Inject
    protected FileManager fileManager;

    @Test
    public void testCreateInddStructureFromZip() throws IOException {
        DocumentModel destination = session.createDocumentModel(session.getRootDocument().getPathAsString(),"folder","Folder");
        destination = session.createDocument(destination);

        Blob blob = new FileBlob(new File(getClass().getResource(INDD_ZIP_PATH).getPath()));

        FileImporterContext context = FileImporterContext.builder(session, blob, destination.getPathAsString())
                .overwrite(true)
                .build();

        DocumentModel compound = fileManager.createOrUpdateDocument(context);
        checkInddCompound(compound);
    }

}
