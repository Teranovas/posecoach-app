package com.example.posecoach.repository

import com.example.posecoach.network.FullResponse
import com.example.posecoach.network.RetrofitClient
import com.example.posecoach.network.SimpleResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File

class PoseRepository {

    private fun filePart(imageFile: File): MultipartBody.Part {
        val req = RequestBody.create("image/*".toMediaTypeOrNull(), imageFile)
        return MultipartBody.Part.createFormData("image", imageFile.name, req)
    }

    suspend fun analyzeSimple(imageFile: File, mode: String?): Response<SimpleResponse> {
        return RetrofitClient.api.analyzePose(
            image = filePart(imageFile),
            format = "simple",
            mode = mode
        )
    }

    suspend fun analyzeFull(imageFile: File, mode: String?): Response<FullResponse> {
        return RetrofitClient.api.analyzePoseFull(
            image = filePart(imageFile),
            format = "full",
            mode = mode
        )
    }

    suspend fun overlay(imageFile: File, mode: String?): Response<ResponseBody> {
        return RetrofitClient.api.analyzePoseOverlay(
            image = filePart(imageFile),
            mode = mode
        )
    }
}
