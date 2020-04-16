import java.security.MessageDigest

fun String.toHash(type: String) =
        MessageDigest
                .getInstance(type)
                .digest(this.toByteArray())
                .joinToString(separator = "", limit = 40) { Integer.toHexString(it.toInt()) }