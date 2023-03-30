package ru.netology.coroutines.dto

data class PostWithAll (
    val post: Post,
    val author: Author,
    val comments: List<CommentWithAuthor>
)