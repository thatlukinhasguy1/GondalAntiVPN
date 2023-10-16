package dev.thatlukinhasguy.antivpn.utils

import java.awt.Color
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Class used to execute Discord Webhooks with low effort
 */
class WebhookUtil(private val url: String) {
    private var content: String? = null
    private var username: String? = null
    private var avatarUrl: String? = null
    private val embeds = mutableListOf<EmbedObject>()

    fun addEmbed(embed: EmbedObject) {
        embeds.add(embed)
    }

    @Throws(IOException::class)
    fun execute() {
        if (content == null && embeds.isEmpty()) {
            throw IllegalArgumentException("Set content or add at least one EmbedObject")
        }

        val json = JSONObject()

        json.put("content", content)
        json.put("username", username)
        json.put("avatar_url", avatarUrl)
        json.put("tts", false)

        if (embeds.isNotEmpty()) {
            val embedObjects = mutableListOf<JSONObject>()

            for (embed in embeds) {
                val jsonEmbed = JSONObject()

                jsonEmbed.put("title", embed.title)
                jsonEmbed.put("description", embed.description)
                jsonEmbed.put("url", embed.url)

                embed.color?.let { color ->
                    val rgb = color.rgb
                    jsonEmbed.put("color", rgb and 0xFFFFFF)
                }

                with(embed.footer) {
                    if (this != null) {
                        val jsonFooter = JSONObject()

                        jsonFooter.put("text", text)
                        jsonFooter.put("icon_url", iconUrl)
                        jsonEmbed.put("footer", jsonFooter)
                    }
                }

                with(embed.image) {
                    if (this != null) {
                        val jsonImage = JSONObject()

                        jsonImage.put("url", url)
                        jsonEmbed.put("image", jsonImage)
                    }
                }

                with(embed.thumbnail) {
                    if (this != null) {
                        val jsonThumbnail = JSONObject()

                        jsonThumbnail.put("url", url)
                        jsonEmbed.put("thumbnail", jsonThumbnail)
                    }
                }

                with(embed.author) {
                    if (this != null) {
                        val jsonAuthor = JSONObject()

                        jsonAuthor.put("name", name)
                        jsonAuthor.put("url", url)
                        jsonAuthor.put("icon_url", iconUrl)
                        jsonEmbed.put("author", jsonAuthor)
                    }
                }

                val jsonFields = mutableListOf<JSONObject>()
                for (field in embed.fields) {
                    val jsonField = JSONObject()

                    jsonField.put("name", field.name)
                    jsonField.put("value", field.value)
                    jsonField.put("inline", field.inline)

                    jsonFields.add(jsonField)
                }

                jsonEmbed.put("fields", jsonFields.toTypedArray())
                embedObjects.add(jsonEmbed)
            }

            json.put("embeds", embedObjects.toTypedArray())
        }

        val url = URL(this.url)
        val connection = url.openConnection() as HttpsURLConnection
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("User-Agent", "Java-DiscordWebhook-BY-Gelox_")
        connection.doOutput = true
        connection.requestMethod = "POST"

        val stream = connection.outputStream
        stream.write(json.toString().toByteArray())
        stream.flush()
        stream.close()

        connection.inputStream.close() // I'm not sure why, but it doesn't work without getting the InputStream
        connection.disconnect()
    }

    /**
     * Class representing an Embed in a Discord message
    */
    class EmbedObject {
        var title: String? = null
        var description: String? = null
        var url: String? = null
        var color: Color? = null

        var footer: Footer? = null
        var thumbnail: Thumbnail? = null
        var image: Image? = null
        var author: Author? = null
        val fields = mutableListOf<Field>()

        fun setTitle(title: String): EmbedObject {
            this.title = title
            return this
        }

        fun setFooter(text: String, iconUrl: String): EmbedObject {
            this.footer = Footer(text, iconUrl)
            return this
        }

        fun setColor(color: Color): EmbedObject {
            this.color = color
            return this
        }

        fun addField(name: String, value: String, inline: Boolean): EmbedObject {
            this.fields.add(Field(name, value, inline))
            return this
        }

        // Data classes for Footer, Thumbnail, Image, Author, and Field

        data class Footer(val text: String, val iconUrl: String)
        data class Thumbnail(val url: String)
        data class Image(val url: String)
        data class Author(val name: String, val url: String, val iconUrl: String)
        data class Field(val name: String, val value: String, val inline: Boolean)
    }

    /**
     * Internal JSONObject representation
    */
    private class JSONObject {
        private val map = HashMap<String, Any?>()

        fun put(key: String, value: Any?) {
            value?.let { map[key] = it }
        }

        override fun toString(): String {
            val builder = StringBuilder()
            val entrySet = map.entries
            builder.append("{")
            for ((i, entry) in entrySet.withIndex()) {
                val value = entry.value
                builder.append(quote(entry.key)).append(":")
                when (value) {
                    is String -> builder.append(quote(value))
                    is Int -> builder.append(value)
                    is Boolean -> builder.append(value)
                    is JSONObject -> builder.append(value)
                    is Array<*> -> {
                        builder.append("[")
                        val len = value.size
                        for (j in 0 until len) {
                            builder.append(value[j].toString()).append(if (j != len - 1) "," else "")
                        }
                        builder.append("]")
                    }
                }
                builder.append(if (i + 1 == entrySet.size) "}" else ",")
            }
            return builder.toString()
        }

        private fun quote(string: String): String {
            return "\"$string\""
        }
    }
}
