package de.rakhman.cooking

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.auth.http.HttpCredentialsAdapter

fun buildSheetsService(httpCredentialsAdapter: HttpCredentialsAdapter): Sheets {
    return Sheets.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        httpCredentialsAdapter
    )
        .setApplicationName("Recipe Plan Android")
        .build()
}