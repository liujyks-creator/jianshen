package com.liujyks.trainflow.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.trainFlowPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = TrainFlowPreferenceKeys.DATASTORE_NAME
)
