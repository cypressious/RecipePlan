package de.rakhman.cooking.backend

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import de.rakhman.cooking.backend.SheetsQuickstart.APPLICATION_NAME
import de.rakhman.cooking.backend.SheetsQuickstart.JSON_FACTORY
import de.rakhman.cooking.backend.SheetsQuickstart.getCredentials
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader

object SheetsQuickstart {
    const val APPLICATION_NAME = "Google Sheets API Java Quickstart"
    val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()
    const val TOKENS_DIRECTORY_PATH = "tokens"

    val SCOPES = mutableListOf<String?>(SheetsScopes.SPREADSHEETS_READONLY)
    const val CREDENTIALS_FILE_PATH = "/credentials.json"

    @Throws(IOException::class)
    fun getCredentials(HTTP_TRANSPORT: NetHttpTransport?): Credential? {
        // Load client secrets.
        val inputStream = SheetsQuickstart::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
        if (inputStream == null) {
            throw FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH)
        }
        val clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(inputStream))

        // Build flow and trigger user authorization request.
        val flow = GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
        )
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()
        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    fun getSheetsService(): Sheets {
        val transport = GoogleNetHttpTransport.newTrustedTransport()
        return Sheets.Builder(transport, JSON_FACTORY, getCredentials(transport))
            .setApplicationName(APPLICATION_NAME)
            .build()
    }
}