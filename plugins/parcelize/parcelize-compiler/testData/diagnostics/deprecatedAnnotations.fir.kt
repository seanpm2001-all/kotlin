// WITH_RUNTIME
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

<!DEPRECATED_PARCELER!>object Parceler1<!> : Parceler<String> {
    override fun create(parcel: Parcel) = parcel.readInt().toString()

    override fun String.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(length)
    }
}

<!DEPRECATED_PARCELER!>object Parceler2<!> : Parceler<List<String>> {
    override fun create(parcel: Parcel) = listOf(parcel.readString()!!)

    override fun List<String>.write(parcel: Parcel, flags: Int) {
        parcel.writeString(this.joinToString(","))
    }
}

<!DEPRECATED_ANNOTATION!>@Parcelize<!>
<!FORBIDDEN_DEPRECATED_ANNOTATION!>@<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>TypeParceler<!><String, Parceler2><!>
data class Test(
    val a: String,
    val b: <!FORBIDDEN_DEPRECATED_ANNOTATION!>@<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>WriteWith<!><Parceler1><!> String,
    val c: <!FORBIDDEN_DEPRECATED_ANNOTATION!>@<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>WriteWith<!><Parceler2><!> List<<!FORBIDDEN_DEPRECATED_ANNOTATION!>@<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>WriteWith<!><Parceler1><!> String>
) : Parcelable {
    <!DEPRECATED_ANNOTATION!>@IgnoredOnParcel<!>
    val x by lazy { "foo" }
}

<!DEPRECATED_PARCELER!>interface ParcelerForUser: Parceler<User><!>

<!DEPRECATED_ANNOTATION!>@Parcelize<!>
class User(val name: String) : Parcelable {
    private companion object : ParcelerForUser {
        override fun User.write(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
        }

        override fun create(parcel: Parcel) = User(parcel.readString()!!)
    }
}
