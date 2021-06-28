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

package org.nuxeo.labs.compound.listener;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.labs.compound.TestFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;
import java.io.File;
import java.io.Serializable;

import static org.nuxeo.labs.compound.TestHelper.COMPOUND_DOC_TYPE;
import static org.nuxeo.labs.compound.service.CompoundDocumentServiceImpl.STRUCTURE_IMPORTED;

@RunWith(FeaturesRunner.class)
@Features({TestFeature.class})
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestCompoundDocumentNewVersionListener {

    @Inject
    protected CoreSession session;

    @Test
    public void testListener() {
        Blob upadteBlob = new FileBlob(new File(getClass().getResource("/files/update_test_update.zip").getPath()));

        DocumentModel compound = session.createDocumentModel(session.getRootDocument().getPathAsString(),"folder",COMPOUND_DOC_TYPE);
        compound = session.createDocument(compound);

        compound.setPropertyValue("file:content", (Serializable) upadteBlob);
        compound = session.saveDocument(compound);

        DocumentModelList children = session.getChildren(compound.getRef());
        Assert.assertEquals(4,children.size());
    }

    @Test
    public void testListenerRunsOnlyOnce() {
        Blob upadteBlob = new FileBlob(new File(getClass().getResource("/files/update_test_update.zip").getPath()));

        DocumentModel compound = session.createDocumentModel(session.getRootDocument().getPathAsString(),"folder",COMPOUND_DOC_TYPE);
        compound = session.createDocument(compound);

        compound.putContextData(STRUCTURE_IMPORTED,true);
        compound.setPropertyValue("file:content", (Serializable) upadteBlob);
        compound = session.saveDocument(compound);

        DocumentModelList children = session.getChildren(compound.getRef());
        Assert.assertEquals(0,children.size());
    }

}
