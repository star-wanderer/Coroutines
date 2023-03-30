package ru.netology.coroutines.dto

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.AttributeType

data class Attachment(
    val url: String ="",
    val description: String = "",
    val type: String
)
