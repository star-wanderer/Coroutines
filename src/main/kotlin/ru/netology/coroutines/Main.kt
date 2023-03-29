package ru.netology.coroutines

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import ru.netology.coroutines.dto.Author
import ru.netology.coroutines.dto.Comment
import ru.netology.coroutines.dto.Post
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

    private val client = OkHttpClient.Builder()
        .connectTimeout(10,TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY})
        .build()

    private const val BASE_URL = "http://127.0.0.1:9999"
    private val gson = Gson()

    suspend fun OkHttpClient.apiCall(url: String): Response {
        return suspendCoroutine { continuation ->
            Request.Builder()
                .url(url)
                .build()
                .let(::newCall).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response)
                    }
                })
        }
    }

    private suspend fun <T> makeRequest(url: String, typeToken: TypeToken<T>): T =
        withContext(Dispatchers.IO) {
            gson.fromJson(client.apiCall(url).body?.string(), typeToken)
        }

    fun main(){
        CoroutineScope(EmptyCoroutineContext).launch {
            val posts = makeRequest(url = "$BASE_URL/api/slow/posts", object : TypeToken<List<Post>>() {})
            posts.map { post ->
                async { makeRequest(url = "$BASE_URL/api/slow/authors/${post.id}", object : TypeToken<Author>() {})
                }
                async {val comments = makeRequest(url = "$BASE_URL/api/slow/posts/${post.id}/comments", object : TypeToken<List<Comment>>() {})
                    comments.map { comment ->
                        async { makeRequest(url = "$BASE_URL/api/slow/author/${comment.id}", object : TypeToken<Author>() {})
                        }
                    }
                }
            }.awaitAll()
        }
        Thread.sleep(10000L )
    }