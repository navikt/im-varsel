import java.security.MessageDigest

fun String.toHash(type: String) =
        MessageDigest
                .getInstance(type)
                .digest(this.toByteArray())
                .fold("", { str, it -> str + "%02x".format(it) })