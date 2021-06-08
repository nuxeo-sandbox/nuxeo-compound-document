# Description

A plugin that provides building blocks to handle compound documents such as 3D models or Material formats 

# How to build
```
git clone https://github.com/nuxeo-sandbox/nuxeo-compound-document
cd nuxeo-compound-document
mvn clean install
```

# Plugin Features

## Facet and schemas
A `Compound` facet and `compound` schema are provided by this plugin.

## Filemanager plugin
A filemanager plugin to import compound documents from a zip archive. Two automation scripts are used:
- `javascript.utils_get_compound_type`, compute the Compound document type from the content of the zip archive
- `javascript.utils_get_compound_sub_folder_type`, compute the folder document types  

At the end of the import, the `compoundDocumentTreeImported` event is fired.

## Thumbnail factory
The thumbnail factory is configured for documents which use the `Compound` facet. The priority order is:
- the `thumb:thumbnail` property of the compound document
- the thumbnail of the the preview document set in the `compound:previewDocument` property
- the default thumbnail (document tyope icon)

# Support

**These features are not part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

# Nuxeo Marketplace
This plugin is published on the [marketplace](https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-compound-document)

# License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

# About Nuxeo

Nuxeo Platform is an open source Content Services platform, written in Java. Data can be stored in both SQL & NoSQL databases.

The development of the Nuxeo Platform is mostly done by Nuxeo employees with an open development model.

The source code, documentation, roadmap, issue tracker, testing, benchmarks are all public.

Typically, Nuxeo users build different types of information management solutions for [document management](https://www.nuxeo.com/solutions/document-management/), [case management](https://www.nuxeo.com/solutions/case-management/), and [digital asset management](https://www.nuxeo.com/solutions/dam-digital-asset-management/), use cases. It uses schema-flexible metadata & content models that allows content to be repurposed to fulfill future use cases.

More information is available at [www.nuxeo.com](https://www.nuxeo.com).
