package com.craftaro.serverjars.builder.services.utils

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import com.craftaro.serverjars.builder.App

suspend fun S3Client.uploadObjectBytes(data: ByteArray, path: String, meta: Map<String, String> = emptyMap(), checksumSha256: String? = null, acl: String? = null) = putObject(PutObjectRequest{
    bucket = App.env["S3_BUCKET"]
    key = if(App.env["SERVERJARS_FOLDER"] != null) "${App.env["SERVERJARS_FOLDER"]}/$path" else path
    metadata = meta
    body = ByteStream.fromBytes(data)
    this.checksumSha256 = checksumSha256
    this.acl = if(acl != null) ObjectCannedAcl.fromValue(acl) else null
})

suspend fun S3Client.readObjectBytes(path: String): ByteArray? = getObject(GetObjectRequest {
    bucket = App.env["S3_BUCKET"]
    key = if(App.env["SERVERJARS_FOLDER"] != null) "${App.env["SERVERJARS_FOLDER"]}/$path" else path
}) {
    it.body?.toByteArray()
}

suspend fun S3Client.deleteObject(path: String) = deleteObject(DeleteObjectRequest {
    bucket = App.env["S3_BUCKET"]
    key = if(App.env["SERVERJARS_FOLDER"] != null) "${App.env["SERVERJARS_FOLDER"]}/$path" else path
})

suspend fun S3Client.objectExists(path: String): Boolean = try {
    getObjectAcl(GetObjectAclRequest {
        bucket = App.env["S3_BUCKET"]
        key = if(App.env["SERVERJARS_FOLDER"] != null) "${App.env["SERVERJARS_FOLDER"]}/$path" else path
    })
    true
} catch (_: NoSuchKey) {
    false
}