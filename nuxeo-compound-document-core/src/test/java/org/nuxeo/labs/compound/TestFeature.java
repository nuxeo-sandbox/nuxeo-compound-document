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

import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

@Features({AutomationFeature.class })
@Deploy({
        "nuxeo-compound-document-core",
        "org.nuxeo.ecm.platform.filemanager",
        "org.nuxeo.ecm.platform.types",
        "org.nuxeo.ecm.platform.webapp.types",
        "org.nuxeo.ecm.actions",
        "nuxeo-compound-document-core:test-compound-type.xml",
        "org.nuxeo.ecm.platform.thumbnail"
})
public class TestFeature implements RunnerFeature {

    @Override
    public void beforeRun(FeaturesRunner runner) {
        Framework.getProperties().setProperty("nuxeo.compound.type.default","Compound");
        Framework.getProperties().setProperty("nuxeo.compound.subtype.default","CompoundSubFolder");
    }

}
