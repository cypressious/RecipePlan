package de.rakhman.cooking.backend

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets

fun buildSheetsService(httpCredentialsAdapter: HttpRequestInitializer): Sheets {
    return Sheets.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        httpCredentialsAdapter
    )
        .setApplicationName("Recipe Plan")
        .build()
}