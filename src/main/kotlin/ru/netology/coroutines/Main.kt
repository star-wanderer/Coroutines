package ru.netology.coroutines

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import ru.netology.coroutines.dto.*
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
                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response)
                    }
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWithException(e)
                    }
                })
        }
    }

    private suspend fun <T> makeRequest(url: String, client: OkHttpClient, typeToken: TypeToken<T>): T =
        withContext(Dispatchers.IO) {
            gson.fromJson(client.apiCall(url).body?.string(), typeToken)
        }

    fun main(){
        CoroutineScope(EmptyCoroutineContext).launch {
            var posts = getPosts(client)
                .map { post ->
                    async {
                         PostWithAll(post, getAuthor(client, post.authorId), getCommentsWithAuthors(client, getComments(client, post.id)))
                    }
            }.awaitAll()
            posts.forEach{ post ->
                println(post.post)
                println("--->${post.author}")
                post.comments.forEach{comment ->
                    println("------>${comment.comment}")
                    println("--------->${comment.author}")
                }
            }
        }
        Thread.sleep(1000L)
    }

    private suspend fun getPosts(client: OkHttpClient): List<Post> =
        makeRequest(url = "$BASE_URL/api/slow/posts", client, object : TypeToken<List<Post>>() {})

    private suspend fun getAuthor(client: OkHttpClient, entityId: Long): Author =
        makeRequest(url = "$BASE_URL/api/slow/authors/$entityId", client, object : TypeToken<Author>() {})

    private suspend fun getComments(client: OkHttpClient, postId: Long): List<Comment> =
        makeRequest(url = "$BASE_URL/api/slow/posts/$postId/comments", client, object : TypeToken<List<Comment>>() {})

    private suspend fun getCommentsWithAuthors(client: OkHttpClient, comments: List<Comment>): List<CommentWithAuthor> =
        comments.map { comment ->
            CommentWithAuthor(comment, getAuthor(client, comment.authorId))
        }
