package com.example.posecoach.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

data class SimpleResponse(
    val pose: String?,
    val feedback: String?,
    val score: Int?
)

data class FullResponse(
    val ok: Boolean,
    val landmarks: List<Map<String, Any>>?,
    val angles: Map<String, Double>?,
    val metrics: Map<String, Any>?,
    val feedback: List<String>?
)

interface PoseApi {
    @Multipart
    @POST("analyze_pose")
    suspend fun analyzePose(
        @Part image: MultipartBody.Part,
        @Query("format") format: String = "simple",
        @Query("mode") mode: String? = null
    ): Response<SimpleResponse>

    @Multipart
    @POST("analyze_pose")
    suspend fun analyzePoseFull(
        @Part image: MultipartBody.Part,
        @Query("format") format: String = "full",
        @Query("mode") mode: String? = null
    ): Response<FullResponse>

    @Multipart
    @POST("analyze_pose_overlay")
    suspend fun analyzePoseOverlay(
        @Part image: MultipartBody.Part,
        @Query("mode") mode: String? = null
    ): Response<ResponseBody> // PNG 바이너리
}
