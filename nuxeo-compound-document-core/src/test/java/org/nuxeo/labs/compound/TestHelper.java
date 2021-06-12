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

package org.nuxeo.labs.compound;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

import java.io.File;
import java.io.Serializable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants.THUMBNAIL_FACET;
import static org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants.THUMBNAIL_PROPERTY_NAME;

public class TestHelper {

    public static final String PREVIEW_THUMBNAIL_NAME = "preview";
    public static final String THUMBNAIL_THUMBNAIL_NAME = "thumbnail";
    public static String COMPOUND_DOC_TYPE = "Compound";
    public static String COMPOUND_SUB_FOLDER_DOC_TYPE = "CompoundSubFolder";

    public static String PNG_PATH = "/files/image.png";
    public static String INDD_ZIP_PATH = "/files/2017_Fall_Event_Invitation.zip";


    public static DocumentModel createCompoundDocument(CoreSession session) {
        DocumentModel compound = session.createDocumentModel(session.getRootDocument().getPathAsString(),"compound",COMPOUND_DOC_TYPE);
        return session.createDocument(compound);
    }

    public static Blob getImageBlob(Class<?> klass, String filename) {
        FileBlob blob = new FileBlob(new File(klass.getResource(PNG_PATH).getPath()));
        blob.setFilename(filename);
        return blob;
    }

    public static DocumentModel createPreviewDocument(DocumentModel compound) {
        CoreSession session = compound.getCoreSession();
        DocumentModel preview = session.createDocumentModel(session.getRootDocument().getPathAsString(),"preview","File");
        preview.addFacet(THUMBNAIL_FACET);
        preview.setPropertyValue(THUMBNAIL_PROPERTY_NAME, (Serializable) getImageBlob(compound.getClass(), PREVIEW_THUMBNAIL_NAME));
        return session.createDocument(preview);
    }

    public static DocumentModel createThumbnailDocument(DocumentModel compound) {
        CoreSession session = compound.getCoreSession();
        DocumentModel thumbnail = session.createDocumentModel(session.getRootDocument().getPathAsString(),"thumbnail","File");
        thumbnail.addFacet(THUMBNAIL_FACET);
        thumbnail.setPropertyValue(THUMBNAIL_PROPERTY_NAME, (Serializable) getImageBlob(compound.getClass(), THUMBNAIL_THUMBNAIL_NAME));
        return session.createDocument(thumbnail);
    }

    public static void checkInddCompound(DocumentModel compound) {
        assertNotNull(compound);

        CoreSession session = compound.getCoreSession();
        DocumentModelList components = session.query(
                String.format(
                        "Select * From Document Where ecm:ancestorId ='%s' AND ecm:mixinType != 'Folderish'",
                        compound.getId()));
        assertEquals(10,components.size());

        DocumentModelList folders = session.query(
                String.format(
                        "Select * From Document Where ecm:ancestorId ='%s' AND ecm:mixinType = 'Folderish'",
                        compound.getId()));
        assertEquals(2,folders.size());
    }

}
