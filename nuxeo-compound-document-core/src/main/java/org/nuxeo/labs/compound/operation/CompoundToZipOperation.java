/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.labs.compound.operation;

import java.io.IOException;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.labs.compound.service.CompoundDocumentService;

@Operation(id = CompoundToZipOperation.ID, category = Constants.CAT_CONVERSION, label = "Zip Compound Document", description = "Zip Compound Document")

public class CompoundToZipOperation {

    public static final String ID = "Compound.ToZip";

    @Context
    public CompoundDocumentService compoundDocumentService;

    @Param(name = "pageProviderName", required = false)
    String pageProviderName;

    @OperationMethod
    public Blob run(DocumentModel document) throws IOException {
        return pageProviderName != null ? compoundDocumentService.toZip(document, pageProviderName)
                : compoundDocumentService.toZip(document);
    }
}
