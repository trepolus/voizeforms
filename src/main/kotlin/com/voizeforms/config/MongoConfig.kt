package com.voizeforms.config

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.ktor.server.config.*

object MongoConfig {
    private lateinit var client: MongoClient
    private lateinit var database: MongoDatabase

        fun init(config: ApplicationConfig): MongoDatabase {
        val connectionString = config.property("mongodb.connectionString").getString()
        val databaseName = config.property("mongodb.database").getString()
        
        // Validate connection string is not empty
        if (connectionString.isBlank()) {
            throw IllegalStateException("MongoDB connection string cannot be empty. Check MONGODB_CONNECTION_STRING environment variable.")
        }
        
        client = MongoClient.create(connectionString)
        database = client.getDatabase(databaseName)
        
        return database
    }

    fun getDatabase(): MongoDatabase = database

    fun close() {
        if (::client.isInitialized) {
            client.close()
        }
    }
} 