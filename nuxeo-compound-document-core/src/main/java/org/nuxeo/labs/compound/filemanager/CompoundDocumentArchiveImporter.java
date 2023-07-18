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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.service.extension.DefaultFileImporter;
import org.nuxeo.labs.compound.service.CompoundDocumentService;
import org.nuxeo.runtime.api.Framework;

public class CompoundDocumentArchiveImporter extends DefaultFileImporter {

    protected static final Logger log = LogManager.getLogger(CompoundDocumentArchiveImporter.class);

    @Override
    public DocumentModel createOrUpdate(FileImporterContext context) {
        CoreSession session = context.getSession();
        CompoundDocumentService compoundDocumentService = Framework.getService(CompoundDocumentService.class);
        DocumentModel parent = session.getDocument(new PathRef(context.getParentPath()));
        try {
            return compoundDocumentService.createCompoundFromArchive(parent, context.getBlob());
        } catch (Exception e) {
            log.warn(e);
            return null;
        }
    }
}