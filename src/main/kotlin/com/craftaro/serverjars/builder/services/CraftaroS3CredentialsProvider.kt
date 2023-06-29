package com.craftaro.serverjars.builder.services

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.util.Attributes
import com.craftaro.serverjars.builder.App

class CraftaroS3CredentialsProvider: CredentialsProvider {
    override suspend fun resolve(attributes: Attributes): Credentials {
        return Credentials(
            accessKeyId = App.env["S3_ACCESS_KEY"] ?: "",
            secretAccessKey = App.env["S3_SECRET_KEY"] ?: "",
            providerName = "CraftaroS3CredentialsProvider"
        )
    }
}