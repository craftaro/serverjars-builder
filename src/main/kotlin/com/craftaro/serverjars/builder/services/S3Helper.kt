package com.craftaro.serverjars.builder.services

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import com.craftaro.serverjars.builder.App

suspend fun S3Client.uploadByteArrayToS3(data: ByteArray, path: String, meta: Map<String, String> = emptyMap(), checksumSha256: String? = null, acl: String? = null) = putObject(PutObjectRequest{
    bucket = App.env["S3_BUCKET"]
    key = path
    metadata = meta
    body = ByteStream.fromBytes(data)
    this.checksumSha256 = checksumSha256
    this.acl = if(acl != null) ObjectCannedAcl.fromValue(acl) else null
})

suspend fun S3Client.readByteArrayFromS3(path: String): ByteArray? = getObject(GetObjectRequest {
    bucket = App.env["S3_BUCKET"]
    key = path
}) {
    it.body?.toByteArray()
}

suspend fun S3Client.deleteFromS3(path: String) = deleteObject(DeleteObjectRequest {
    bucket = App.env["S3_BUCKET"]
    key = path
})
