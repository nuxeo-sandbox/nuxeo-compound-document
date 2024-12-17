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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.labs.compound.TestFeature;
import org.nuxeo.labs.compound.TestHelper;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import jakarta.inject.Inject;

import static org.nuxeo.labs.compound.TestHelper.createPreviewDocument;

@RunWith(FeaturesRunner.class)
@Features({TestFeature.class})
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestCompoundDocumentAdapter {

    @Inject
    protected CoreSession session;

    @Test
    public void testAdapter() {
        DocumentModel compound = TestHelper.createCompoundDocument(session);
        CompoundDocument compoundDocument = compound.getAdapter(CompoundDocumentAdapter.class);
        Assert.assertNotNull(compoundDocument);

        DocumentModel preview = createPreviewDocument(compound);
        compoundDocument.setPreviewDocument(preview);

        DocumentModel returnedDoc = compoundDocument.getPreviewDocument();
        Assert.assertNotNull(returnedDoc);
    }

}
