package com.liujyks.trainflow.core.database

/**
 * Package boundary for Room entities, DAO contracts, schema, and migrations.
 *
 * Feature UI must consume repository or domain models instead of depending on
 * database entities. Core model classes stay platform independent and are not
 * annotated with Room metadata.
 */
internal object DatabaseBoundary
